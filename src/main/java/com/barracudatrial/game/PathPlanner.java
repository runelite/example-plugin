package com.barracudatrial.game;

import com.barracudatrial.BarracudaTrialConfig;
import com.barracudatrial.pathfinding.AStarPathfinder;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import com.barracudatrial.pathfinding.MultiLapOptimizer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Orchestrates path calculation for the Barracuda Trial plugin
 * Handles multi-lap optimization, A* pathfinding, and path recalculation
 */
@Slf4j
public class PathPlanner
{
	private final Client client;
	private final State state;
	private final BarracudaTrialConfig config;
	private final LocationHelper locationHelper;

	public PathPlanner(Client client, State state, BarracudaTrialConfig config, LocationHelper locationHelper)
	{
		this.client = client;
		this.state = state;
		this.config = config;
		this.locationHelper = locationHelper;
	}

	/**
	 * Recalculates the optimal path based on current game state
	 * @param caller Description of what triggered this recalculation (for debugging)
	 */
	public void recalculateOptimalPath(String caller)
	{
		state.setLastPathRecalcCaller(caller);
		log.debug("Path recalculation triggered by: {}", caller);

		if (!state.isInTrialArea())
		{
			state.getOptimalPath().clear();
			state.getCurrentSegmentPath().clear();
			state.getNextSegmentPath().clear();
			state.getPlannedLaps().clear();
			return;
		}

		// Reset tick counter when manually recalculating
		state.setTicksSinceLastPathRecalc(0);

		// Use boat location as the start point (not player location!)
		WorldPoint startPoint = state.getBoatLocation();
		if (startPoint == null)
		{
			return;
		}

		// Plan optimal laps for collecting lost supplies (handles all cases including no supplies)
		long planStart = config.debugMode() ? System.currentTimeMillis() : 0;
		planOptimalLaps();
		if (config.debugMode())
		{
			state.setLastPathPlanningTimeMs(System.currentTimeMillis() - planStart);
		}

		// Special case: If no supplies AND we have rum, go straight to dropoff
		if (state.getLostSupplies().isEmpty() && state.isHasRumOnUs())
		{
			List<WorldPoint> currentSegmentPath = new ArrayList<>();
			if (state.getRumReturnLocation() != null)
			{
				currentSegmentPath.add(locationHelper.getPathfindingDropoffLocation());
			}
			state.setCurrentSegmentPath(currentSegmentPath);
			state.setNextSegmentPath(new ArrayList<>());
			state.setOptimalPath(new ArrayList<>(currentSegmentPath));
			log.debug("No supplies and have rum, pathing to dropoff");
			return;
		}

		// Set current and next segment paths
		if (state.getCurrentLap() < state.getPlannedLaps().size())
		{
			// Optimizer returns complete route: Lost Supplies → Pickup → Lost Supplies
			// with pickup inserted at optimal position
			List<WorldPoint> waypoints = new ArrayList<>(state.getPlannedLaps().get(state.getCurrentLap()));

			// Add rum dropoff at end (mandatory lap endpoint)
			// Target 2 tiles south of dropoff (actual dropoff is impassable)
			if (state.getRumReturnLocation() != null)
			{
				waypoints.add(locationHelper.getPathfindingDropoffLocation());
			}

			// Expand waypoints into full A* path (tactical navigation with speed boosts)
			long astarStart = config.debugMode() ? System.currentTimeMillis() : 0;
			List<WorldPoint> currentSegmentPath = expandWaypointsWithAStar(state.getBoatLocation(), waypoints);
			state.setCurrentSegmentPath(currentSegmentPath);
			if (config.debugMode())
			{
				state.setLastAStarTimeMs(System.currentTimeMillis() - astarStart);
			}
		}
		else
		{
			state.setCurrentSegmentPath(new ArrayList<>());
		}

		// Show next segment preview (if more laps needed)
		if (state.getCurrentLap() + 1 < state.getPlannedLaps().size())
		{
			// Optimizer returns complete route: Lost Supplies → Pickup → Lost Supplies
			// with pickup inserted at optimal position
			List<WorldPoint> nextWaypoints = new ArrayList<>(state.getPlannedLaps().get(state.getCurrentLap() + 1));

			// Add rum dropoff at end (mandatory lap endpoint)
			// Target 2 tiles south of dropoff (actual dropoff is impassable)
			if (state.getRumReturnLocation() != null)
			{
				nextWaypoints.add(locationHelper.getPathfindingDropoffLocation());
			}

			// Expand waypoints into full A* path
			// Start from pathfinding dropoff location (2 tiles south of actual)
			WorldPoint nextStart = state.getRumReturnLocation() != null ? locationHelper.getPathfindingDropoffLocation() : state.getBoatLocation();
			List<WorldPoint> nextSegmentPath = expandWaypointsWithAStar(nextStart, nextWaypoints);
			state.setNextSegmentPath(nextSegmentPath);
		}
		else
		{
			state.setNextSegmentPath(new ArrayList<>());
		}

		// Update optimalPath for backward compatibility
		state.setOptimalPath(new ArrayList<>(state.getCurrentSegmentPath()));

		log.debug("Planned {} laps, currently on lap {}/{}", state.getPlannedLaps().size(), state.getCurrentLap() + 1, state.getRumsNeeded());
	}

	/**
	 * Plans optimal laps for collecting all lost supplies
	 * Uses multi-lap optimization to minimize TOTAL time, not just first lap
	 */
	private void planOptimalLaps()
	{
		state.getPlannedLaps().clear();

		if (state.getBoatLocation() == null || state.getLostSupplies().isEmpty())
		{
			return;
		}

		// Calculate how many laps we need
		int lapsNeeded = Math.max(1, state.getRumsNeeded() - state.getCurrentLap());

		// If we already have supply assignments, only replan current lap to avoid going backwards
		if (!state.getLostSuppliesForCurrentLap().isEmpty() || !state.getLostSuppliesForFutureLaps().isEmpty())
		{
			// Just replan the current lap with its assigned supplies
			replanCurrentLapOnly();
			return;
		}

		// Initial planning: use multi-lap optimizer for GLOBAL optimization
		// This ensures we minimize TOTAL time, not just first lap time
		state.getLostSuppliesForCurrentLap().clear();
		state.getLostSuppliesForFutureLaps().clear();

		// Create multi-lap optimizer (uses straight-line distance with rock and exclusion zone penalties)
		boolean preferWestStart = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer optimizer = new MultiLapOptimizer(
			state.getKnownRockLocations(),
			preferWestStart,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		// Use fallback locations if we haven't seen the rum objects yet
		WorldPoint pickupLoc = state.getRumPickupLocation();
		WorldPoint returnLoc = state.getRumReturnLocation();

		// Calculate missing locations using known offsets
		if (returnLoc == null && pickupLoc != null)
		{
			// Calculate return from pickup (reverse offset)
			returnLoc = new WorldPoint(
				pickupLoc.getX() - State.PICKUP_OFFSET_X,
				pickupLoc.getY() - State.PICKUP_OFFSET_Y,
				pickupLoc.getPlane()
			);
		}
		else if (pickupLoc == null && returnLoc != null)
		{
			// Calculate pickup from return (forward offset)
			pickupLoc = new WorldPoint(
				returnLoc.getX() + State.PICKUP_OFFSET_X,
				returnLoc.getY() + State.PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}
		else if (returnLoc == null && pickupLoc == null)
		{
			// Fallback: use boat location as dropoff and calculate pickup
			returnLoc = state.getBoatLocation();
			pickupLoc = new WorldPoint(
				returnLoc.getX() + State.PICKUP_OFFSET_X,
				returnLoc.getY() + State.PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}

		// Plan all laps considering total time
		// Use pathfinding offsets: pickup +2 north, dropoff +2 south
		WorldPoint pathfindingPickup = pickupLoc != null ? new WorldPoint(pickupLoc.getX(), pickupLoc.getY() + 2, pickupLoc.getPlane()) : null;
		WorldPoint pathfindingDropoff = returnLoc != null ? new WorldPoint(returnLoc.getX(), returnLoc.getY() - 2, returnLoc.getPlane()) : null;

		int maxSearchDistance = 100;
		List<List<WorldPoint>> plannedLaps = optimizer.planMultipleLaps(
			state.getBoatLocation(),
			state.getLostSupplies(),
			lapsNeeded,
			pathfindingPickup,
			pathfindingDropoff,
			maxSearchDistance,
			state.isHasRumOnUs(),
			state.getCurrentLap()
		);
		state.setPlannedLaps(plannedLaps);

		// Track which supplies are assigned to which laps
		for (int lap = 0; lap < plannedLaps.size(); lap++)
		{
			List<WorldPoint> lapPath = plannedLaps.get(lap);

			// Find which supplies correspond to this lap's waypoints
			for (WorldPoint waypoint : lapPath)
			{
				for (GameObject supply : state.getLostSupplies())
				{
					if (supply.getWorldLocation().equals(waypoint))
					{
						if (lap == 0)
						{
							state.getLostSuppliesForCurrentLap().add(supply);
						}
						else
						{
							state.getLostSuppliesForFutureLaps().add(supply);
						}
						break;
					}
				}
			}
		}

		log.debug("Planned {} laps with {} total supplies ({} current, {} future) - TOTAL TIME OPTIMIZED",
			plannedLaps.size(), state.getLostSupplies().size(), state.getLostSuppliesForCurrentLap().size(), state.getLostSuppliesForFutureLaps().size());
	}

	/**
	 * Replans only the current lap using assigned supplies (doesn't go backwards for future lap supplies)
	 * Uses strategic pathfinding (rocks + speed boosts, no clouds)
	 */
	private void replanCurrentLapOnly()
	{
		if (state.getBoatLocation() == null)
		{
			return;
		}

		// Only consider supplies assigned to current lap
		Set<GameObject> currentLapSupplies = new HashSet<>(state.getLostSuppliesForCurrentLap());
		currentLapSupplies.retainAll(state.getLostSupplies()); // Only supplies still visible

		if (currentLapSupplies.isEmpty())
		{
			// No supplies for current lap, path to rum pickup
			List<List<WorldPoint>> plannedLaps = new ArrayList<>();
			plannedLaps.add(new ArrayList<>());
			state.setPlannedLaps(plannedLaps);
			return;
		}

		// Create multi-lap optimizer (uses straight-line distance with rock and exclusion zone penalties)
		boolean preferWestStart = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer optimizer = new MultiLapOptimizer(
			state.getKnownRockLocations(),
			preferWestStart,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		// Use fallback locations if we haven't seen the rum objects yet
		WorldPoint pickupLoc = state.getRumPickupLocation();
		WorldPoint returnLoc = state.getRumReturnLocation();

		// Calculate missing locations using known offsets
		if (returnLoc == null && pickupLoc != null)
		{
			// Calculate return from pickup (reverse offset)
			returnLoc = new WorldPoint(
				pickupLoc.getX() - State.PICKUP_OFFSET_X,
				pickupLoc.getY() - State.PICKUP_OFFSET_Y,
				pickupLoc.getPlane()
			);
		}
		else if (pickupLoc == null && returnLoc != null)
		{
			// Calculate pickup from return (forward offset)
			pickupLoc = new WorldPoint(
				returnLoc.getX() + State.PICKUP_OFFSET_X,
				returnLoc.getY() + State.PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}
		else if (returnLoc == null && pickupLoc == null)
		{
			// Fallback: use boat location as dropoff and calculate pickup
			returnLoc = state.getBoatLocation();
			pickupLoc = new WorldPoint(
				returnLoc.getX() + State.PICKUP_OFFSET_X,
				returnLoc.getY() + State.PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}

		// Replan current lap
		// Use pathfinding offsets: pickup +2 north, dropoff +2 south
		WorldPoint pathfindingPickup = pickupLoc != null ? new WorldPoint(pickupLoc.getX(), pickupLoc.getY() + 2, pickupLoc.getPlane()) : null;
		WorldPoint pathfindingDropoff = returnLoc != null ? new WorldPoint(returnLoc.getX(), returnLoc.getY() - 2, returnLoc.getPlane()) : null;

		int maxSearchDistance = 100;
		List<List<WorldPoint>> replanned = optimizer.planMultipleLaps(
			state.getBoatLocation(),
			currentLapSupplies,
			1, // Just current lap
			pathfindingPickup,
			pathfindingDropoff,
			maxSearchDistance,
			state.isHasRumOnUs(),
			state.getCurrentLap()
		);

		List<List<WorldPoint>> plannedLaps = new ArrayList<>();
		if (!replanned.isEmpty())
		{
			plannedLaps.add(replanned.get(0));
		}

		// Also plan next lap as preview (using future lap supplies)
		if (state.getCurrentLap() + 1 < state.getRumsNeeded() && !state.getLostSuppliesForFutureLaps().isEmpty())
		{
			Set<GameObject> futureLapSupplies = new HashSet<>(state.getLostSuppliesForFutureLaps());
			futureLapSupplies.retainAll(state.getLostSupplies());

			if (!futureLapSupplies.isEmpty() && state.getRumReturnLocation() != null)
			{
				List<List<WorldPoint>> nextLapPlan = optimizer.planMultipleLaps(
					state.getRumReturnLocation(),
					futureLapSupplies,
					1,
					state.getRumPickupLocation(),
					state.getRumReturnLocation(),
					maxSearchDistance,
					false, // Next lap starts without rum
					state.getCurrentLap() + 1
				);

				if (!nextLapPlan.isEmpty())
				{
					plannedLaps.add(nextLapPlan.get(0));
				}
			}
		}

		state.setPlannedLaps(plannedLaps);

		log.debug("Replanned current lap only with {} supplies (avoiding {} future lap supplies)",
			currentLapSupplies.size(), state.getLostSuppliesForFutureLaps().size());
	}

	/**
	 * Expands waypoints into full A* paths using tactical pathfinding
	 * Only expands the next 3 waypoints for performance (user only needs immediate path)
	 * Uses BarracudaTileCostCalculator to route through speed boosts and avoid clouds/rocks
	 */
	private List<WorldPoint> expandWaypointsWithAStar(WorldPoint start, List<WorldPoint> waypoints)
	{
		if (waypoints.isEmpty())
		{
			return new ArrayList<>();
		}

		// Filter out safe clouds (only pass dangerous ones to cost calculator)
		Set<NPC> dangerousClouds = new HashSet<>();
		for (NPC cloud : state.getLightningClouds())
		{
			if (!isCloudSafe(cloud.getAnimation()))
			{
				dangerousClouds.add(cloud);
			}
		}

		// Create tactical cost calculator (includes clouds, speed boosts, rocks, exclusion zone)
		BarracudaTileCostCalculator costCalculator = new BarracudaTileCostCalculator(
			state.getKnownSpeedBoostLocations(),
			state.getKnownRockLocations(),
			state.getRocks(),
			dangerousClouds,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		// Create A* pathfinder
		AStarPathfinder pathfinder = new AStarPathfinder(costCalculator);

		List<WorldPoint> fullPath = new ArrayList<>();
		WorldPoint current = start;
		int maxSearchDistance = 100;

		// Only expand next N waypoints with A* (configurable for performance and visual clarity)
		// Remaining waypoints will be expanded when closer on future recalcs
		int waypointsToExpand = Math.min(config.pathLookahead(), waypoints.size());

		// Expand first N waypoints with A*
		for (int i = 0; i < waypointsToExpand; i++)
		{
			WorldPoint waypoint = waypoints.get(i);
			List<WorldPoint> segment = pathfinder.findPath(current, waypoint, maxSearchDistance);

			if (segment.isEmpty())
			{
				// No path found, fallback to direct waypoint
				fullPath.add(waypoint);
			}
			else
			{
				// Add all tiles in this segment (except first, which is current position)
				for (int j = 1; j < segment.size(); j++)
				{
					fullPath.add(segment.get(j));
				}
			}

			current = waypoint;
		}

		// Do NOT add remaining waypoints - they will be calculated when closer
		// This reduces both computation time and visual clutter

		return fullPath;
	}

	/**
	 * Checks if a cloud animation indicates it's safe
	 * @param animationId The cloud's current animation ID
	 * @return true if the cloud is safe (harmless animation)
	 */
	private boolean isCloudSafe(int animationId)
	{
		return animationId == State.CLOUD_ANIM_HARMLESS || animationId == State.CLOUD_ANIM_HARMLESS_ALT;
	}

	/**
	 * Checks if a cloud is dangerous based on its animation
	 * @param cloud The cloud NPC to check
	 * @return true if the cloud is dangerous
	 */
	public boolean isCloudDangerous(NPC cloud)
	{
		int animation = cloud.getAnimation();
		// Cloud is dangerous if it's NOT in a safe animation state
		return !isCloudSafe(animation);
	}

	/**
	 * Generates a circular path around the exclusion zone
	 * Used as a fallback when no specific targets are available
	 */
	public List<WorldPoint> generateCircularPath(WorldPoint start)
	{
		List<WorldPoint> path = new ArrayList<>();

		// If we don't have rum return location yet, can't calculate circular path
		if (state.getRumReturnLocation() == null)
		{
			return path;
		}

		// Create waypoints traveling west around the island
		// These waypoints circle around the exclusion zone
		int centerX = (state.getExclusionZoneMinX() + state.getExclusionZoneMaxX()) / 2;
		int centerY = (state.getExclusionZoneMinY() + state.getExclusionZoneMaxY()) / 2;

		// Calculate radius to stay outside exclusion zone
		int radiusX = (state.getExclusionZoneMaxX() - state.getExclusionZoneMinX()) / 2 + 10; // 10 tile buffer
		int radiusY = (state.getExclusionZoneMaxY() - state.getExclusionZoneMinY()) / 2 + 10;

		// Generate 8 waypoints around the circle, starting from current position
		// and going counter-clockwise (west)
		double startAngle = Math.atan2(start.getY() - centerY, start.getX() - centerX);

		for (int i = 1; i <= 8; i++)
		{
			// Travel counter-clockwise (subtract angle)
			double angle = startAngle - (i * Math.PI / 4); // 45 degrees per waypoint

			int waypointX = (int) (centerX + radiusX * Math.cos(angle));
			int waypointY = (int) (centerY + radiusY * Math.sin(angle));

			path.add(new WorldPoint(waypointX, waypointY, start.getPlane()));
		}

		return path;
	}

	/**
	 * Helper interface for location calculations
	 * Provides pathfinding-adjusted locations for rum pickup/dropoff
	 */
	public interface LocationHelper
	{
		WorldPoint getPathfindingPickupLocation();
		WorldPoint getPathfindingDropoffLocation();
	}
}
