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
	 * @param recalculationTriggerReason Description of what triggered this recalculation (for debugging)
	 */
	public void recalculateOptimalPathFromCurrentState(String recalculationTriggerReason)
	{
		state.setLastPathRecalcCaller(recalculationTriggerReason);
		log.debug("Path recalculation triggered by: {}", recalculationTriggerReason);

		if (!state.isInTrialArea())
		{
			state.getOptimalPath().clear();
			state.getCurrentSegmentPath().clear();
			state.getNextSegmentPath().clear();
			state.getPlannedLaps().clear();
			return;
		}

		state.setTicksSinceLastPathRecalc(0);

		WorldPoint playerBoatLocation = state.getBoatLocation();
		if (playerBoatLocation == null)
		{
			return;
		}

		long multiLapPlanningStartTime = config.debugMode() ? System.currentTimeMillis() : 0;
		planOptimalMultiLapRoute();
		if (config.debugMode())
		{
			state.setLastPathPlanningTimeMs(System.currentTimeMillis() - multiLapPlanningStartTime);
		}

		if (state.getLostSupplies().isEmpty() && state.isHasRumOnUs())
		{
			List<WorldPoint> pathToDropoffOnly = new ArrayList<>();
			if (state.getRumReturnLocation() != null)
			{
				pathToDropoffOnly.add(locationHelper.getPathfindingDropoffLocation());
			}
			state.setCurrentSegmentPath(pathToDropoffOnly);
			state.setNextSegmentPath(new ArrayList<>());
			state.setOptimalPath(new ArrayList<>(pathToDropoffOnly));
			log.debug("No supplies and have rum, pathing to dropoff");
			return;
		}

		if (state.getCurrentLap() < state.getPlannedLaps().size())
		{
			List<WorldPoint> waypointsForCurrentLap = new ArrayList<>(state.getPlannedLaps().get(state.getCurrentLap()));

			// Actual dropoff is impassable, so we target 2 tiles south
			if (state.getRumReturnLocation() != null)
			{
				waypointsForCurrentLap.add(locationHelper.getPathfindingDropoffLocation());
			}

			long aStarStartTime = config.debugMode() ? System.currentTimeMillis() : 0;
			List<WorldPoint> expandedCurrentSegmentPath = expandStrategicWaypointsWithTacticalAStar(state.getBoatLocation(), waypointsForCurrentLap);
			state.setCurrentSegmentPath(expandedCurrentSegmentPath);
			if (config.debugMode())
			{
				state.setLastAStarTimeMs(System.currentTimeMillis() - aStarStartTime);
			}
		}
		else
		{
			state.setCurrentSegmentPath(new ArrayList<>());
		}

		if (state.getCurrentLap() + 1 < state.getPlannedLaps().size())
		{
			List<WorldPoint> waypointsForNextLap = new ArrayList<>(state.getPlannedLaps().get(state.getCurrentLap() + 1));

			// Actual dropoff is impassable, so we target 2 tiles south
			if (state.getRumReturnLocation() != null)
			{
				waypointsForNextLap.add(locationHelper.getPathfindingDropoffLocation());
			}

			// Actual dropoff is impassable, so we target 2 tiles south
			WorldPoint nextLapStartLocation = state.getRumReturnLocation() != null ? locationHelper.getPathfindingDropoffLocation() : state.getBoatLocation();
			List<WorldPoint> expandedNextSegmentPath = expandStrategicWaypointsWithTacticalAStar(nextLapStartLocation, waypointsForNextLap);
			state.setNextSegmentPath(expandedNextSegmentPath);
		}
		else
		{
			state.setNextSegmentPath(new ArrayList<>());
		}

		state.setOptimalPath(new ArrayList<>(state.getCurrentSegmentPath()));

		log.debug("Planned {} laps, currently on lap {}/{}", state.getPlannedLaps().size(), state.getCurrentLap() + 1, state.getRumsNeeded());
	}

	/**
	 * Plans optimal laps for collecting all lost supplies
	 * Uses multi-lap optimization to minimize TOTAL time, not just first lap
	 */
	private void planOptimalMultiLapRoute()
	{
		state.getPlannedLaps().clear();

		if (state.getBoatLocation() == null || state.getLostSupplies().isEmpty())
		{
			return;
		}

		int numberOfLapsNeeded = Math.max(1, state.getRumsNeeded() - state.getCurrentLap());

		if (!state.getLostSuppliesForCurrentLap().isEmpty() || !state.getLostSuppliesForFutureLaps().isEmpty())
		{
			replanCurrentLapWithAssignedSuppliesOnly();
			return;
		}

		state.getLostSuppliesForCurrentLap().clear();
		state.getLostSuppliesForFutureLaps().clear();

		boolean shouldPreferWestStartDirection = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer multiLapOptimizer = new MultiLapOptimizer(
			state.getKnownRockLocations(),
			shouldPreferWestStartDirection,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		WorldPoint rumPickupWorldLocation = state.getRumPickupLocation();
		WorldPoint rumReturnWorldLocation = state.getRumReturnLocation();

		if (rumReturnWorldLocation == null && rumPickupWorldLocation != null)
		{
			rumReturnWorldLocation = new WorldPoint(
				rumPickupWorldLocation.getX() - State.PICKUP_OFFSET_X,
				rumPickupWorldLocation.getY() - State.PICKUP_OFFSET_Y,
				rumPickupWorldLocation.getPlane()
			);
		}
		else if (rumPickupWorldLocation == null && rumReturnWorldLocation != null)
		{
			rumPickupWorldLocation = new WorldPoint(
				rumReturnWorldLocation.getX() + State.PICKUP_OFFSET_X,
				rumReturnWorldLocation.getY() + State.PICKUP_OFFSET_Y,
				rumReturnWorldLocation.getPlane()
			);
		}
		else if (rumReturnWorldLocation == null && rumPickupWorldLocation == null)
		{
			rumReturnWorldLocation = state.getBoatLocation();
			rumPickupWorldLocation = new WorldPoint(
				rumReturnWorldLocation.getX() + State.PICKUP_OFFSET_X,
				rumReturnWorldLocation.getY() + State.PICKUP_OFFSET_Y,
				rumReturnWorldLocation.getPlane()
			);
		}

		// Actual pickup is impassable, so we target 2 tiles north
		WorldPoint pathfindingSafePickupLocation = rumPickupWorldLocation != null ? new WorldPoint(rumPickupWorldLocation.getX(), rumPickupWorldLocation.getY() + 2, rumPickupWorldLocation.getPlane()) : null;
		// Actual dropoff is impassable, so we target 2 tiles south
		WorldPoint pathfindingSafeDropoffLocation = rumReturnWorldLocation != null ? new WorldPoint(rumReturnWorldLocation.getX(), rumReturnWorldLocation.getY() - 2, rumReturnWorldLocation.getPlane()) : null;

		int maximumAStarSearchDistance = 100;
		List<List<WorldPoint>> allPlannedLaps = multiLapOptimizer.planMultipleLaps(
			state.getBoatLocation(),
			state.getLostSupplies(),
			numberOfLapsNeeded,
			pathfindingSafePickupLocation,
			pathfindingSafeDropoffLocation,
			maximumAStarSearchDistance,
			state.isHasRumOnUs(),
			state.getCurrentLap()
		);
		state.setPlannedLaps(allPlannedLaps);

		for (int lapIndex = 0; lapIndex < allPlannedLaps.size(); lapIndex++)
		{
			List<WorldPoint> waypointsForThisLap = allPlannedLaps.get(lapIndex);

			for (WorldPoint waypointLocation : waypointsForThisLap)
			{
				for (GameObject lostSupplyObject : state.getLostSupplies())
				{
					if (lostSupplyObject.getWorldLocation().equals(waypointLocation))
					{
						if (lapIndex == 0)
						{
							state.getLostSuppliesForCurrentLap().add(lostSupplyObject);
						}
						else
						{
							state.getLostSuppliesForFutureLaps().add(lostSupplyObject);
						}
						break;
					}
				}
			}
		}

		log.debug("Planned {} laps with {} total supplies ({} current, {} future) - TOTAL TIME OPTIMIZED",
			allPlannedLaps.size(), state.getLostSupplies().size(), state.getLostSuppliesForCurrentLap().size(), state.getLostSuppliesForFutureLaps().size());
	}

	/**
	 * Replans only the current lap using assigned supplies (doesn't go backwards for future lap supplies)
	 * Uses strategic pathfinding (rocks + speed boosts, no clouds)
	 */
	private void replanCurrentLapWithAssignedSuppliesOnly()
	{
		if (state.getBoatLocation() == null)
		{
			return;
		}

		Set<GameObject> suppliesForCurrentLapOnly = new HashSet<>(state.getLostSuppliesForCurrentLap());
		suppliesForCurrentLapOnly.retainAll(state.getLostSupplies());

		if (suppliesForCurrentLapOnly.isEmpty())
		{
			List<List<WorldPoint>> plannedLapsWithOnlyPickup = new ArrayList<>();
			plannedLapsWithOnlyPickup.add(new ArrayList<>());
			state.setPlannedLaps(plannedLapsWithOnlyPickup);
			return;
		}

		boolean shouldPreferWestStartDirection = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer multiLapOptimizer = new MultiLapOptimizer(
			state.getKnownRockLocations(),
			shouldPreferWestStartDirection,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		WorldPoint rumPickupWorldLocation = state.getRumPickupLocation();
		WorldPoint rumReturnWorldLocation = state.getRumReturnLocation();

		if (rumReturnWorldLocation == null && rumPickupWorldLocation != null)
		{
			rumReturnWorldLocation = new WorldPoint(
				rumPickupWorldLocation.getX() - State.PICKUP_OFFSET_X,
				rumPickupWorldLocation.getY() - State.PICKUP_OFFSET_Y,
				rumPickupWorldLocation.getPlane()
			);
		}
		else if (rumPickupWorldLocation == null && rumReturnWorldLocation != null)
		{
			rumPickupWorldLocation = new WorldPoint(
				rumReturnWorldLocation.getX() + State.PICKUP_OFFSET_X,
				rumReturnWorldLocation.getY() + State.PICKUP_OFFSET_Y,
				rumReturnWorldLocation.getPlane()
			);
		}
		else if (rumReturnWorldLocation == null && rumPickupWorldLocation == null)
		{
			rumReturnWorldLocation = state.getBoatLocation();
			rumPickupWorldLocation = new WorldPoint(
				rumReturnWorldLocation.getX() + State.PICKUP_OFFSET_X,
				rumReturnWorldLocation.getY() + State.PICKUP_OFFSET_Y,
				rumReturnWorldLocation.getPlane()
			);
		}

		// Actual pickup is impassable, so we target 2 tiles north
		WorldPoint pathfindingSafePickupLocation = rumPickupWorldLocation != null ? new WorldPoint(rumPickupWorldLocation.getX(), rumPickupWorldLocation.getY() + 2, rumPickupWorldLocation.getPlane()) : null;
		// Actual dropoff is impassable, so we target 2 tiles south
		WorldPoint pathfindingSafeDropoffLocation = rumReturnWorldLocation != null ? new WorldPoint(rumReturnWorldLocation.getX(), rumReturnWorldLocation.getY() - 2, rumReturnWorldLocation.getPlane()) : null;

		int maximumAStarSearchDistance = 100;
		List<List<WorldPoint>> replannedCurrentLap = multiLapOptimizer.planMultipleLaps(
			state.getBoatLocation(),
			suppliesForCurrentLapOnly,
			1,
			pathfindingSafePickupLocation,
			pathfindingSafeDropoffLocation,
			maximumAStarSearchDistance,
			state.isHasRumOnUs(),
			state.getCurrentLap()
		);

		List<List<WorldPoint>> allPlannedLaps = new ArrayList<>();
		if (!replannedCurrentLap.isEmpty())
		{
			allPlannedLaps.add(replannedCurrentLap.get(0));
		}

		if (state.getCurrentLap() + 1 < state.getRumsNeeded() && !state.getLostSuppliesForFutureLaps().isEmpty())
		{
			Set<GameObject> suppliesForFutureLapsOnly = new HashSet<>(state.getLostSuppliesForFutureLaps());
			suppliesForFutureLapsOnly.retainAll(state.getLostSupplies());

			if (!suppliesForFutureLapsOnly.isEmpty() && state.getRumReturnLocation() != null)
			{
				List<List<WorldPoint>> plannedNextLap = multiLapOptimizer.planMultipleLaps(
					state.getRumReturnLocation(),
					suppliesForFutureLapsOnly,
					1,
					state.getRumPickupLocation(),
					state.getRumReturnLocation(),
					maximumAStarSearchDistance,
					false,
					state.getCurrentLap() + 1
				);

				if (!plannedNextLap.isEmpty())
				{
					allPlannedLaps.add(plannedNextLap.get(0));
				}
			}
		}

		state.setPlannedLaps(allPlannedLaps);

		log.debug("Replanned current lap only with {} supplies (avoiding {} future lap supplies)",
			suppliesForCurrentLapOnly.size(), state.getLostSuppliesForFutureLaps().size());
	}

	/**
	 * Expands waypoints into full A* paths using tactical pathfinding
	 * Only expands the next 3 waypoints for performance (user only needs immediate path)
	 * Uses BarracudaTileCostCalculator to route through speed boosts and avoid clouds/rocks
	 */
	private List<WorldPoint> expandStrategicWaypointsWithTacticalAStar(WorldPoint startLocation, List<WorldPoint> strategicWaypoints)
	{
		if (strategicWaypoints.isEmpty())
		{
			return new ArrayList<>();
		}

		Set<NPC> currentlyDangerousClouds = new HashSet<>();
		for (NPC lightningCloud : state.getLightningClouds())
		{
			if (!isCloudAnimationSafe(lightningCloud.getAnimation()))
			{
				currentlyDangerousClouds.add(lightningCloud);
			}
		}

		BarracudaTileCostCalculator tileCostCalculator = new BarracudaTileCostCalculator(
			state.getKnownSpeedBoostLocations(),
			state.getKnownRockLocations(),
			state.getRocks(),
			currentlyDangerousClouds,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY()
		);

		AStarPathfinder aStarPathfinder = new AStarPathfinder(tileCostCalculator);

		List<WorldPoint> completeExpandedPath = new ArrayList<>();
		WorldPoint currentPathfindingPosition = startLocation;
		int maximumAStarSearchDistance = 100;

		int numberOfWaypointsToExpandWithAStar = Math.min(config.pathLookahead(), strategicWaypoints.size());

		for (int waypointIndex = 0; waypointIndex < numberOfWaypointsToExpandWithAStar; waypointIndex++)
		{
			WorldPoint targetWaypoint = strategicWaypoints.get(waypointIndex);
			List<WorldPoint> pathSegmentToWaypoint = aStarPathfinder.findPath(currentPathfindingPosition, targetWaypoint, maximumAStarSearchDistance);

			if (pathSegmentToWaypoint.isEmpty())
			{
				completeExpandedPath.add(targetWaypoint);
			}
			else
			{
				for (int tileIndex = 1; tileIndex < pathSegmentToWaypoint.size(); tileIndex++)
				{
					completeExpandedPath.add(pathSegmentToWaypoint.get(tileIndex));
				}
			}

			currentPathfindingPosition = targetWaypoint;
		}

		return completeExpandedPath;
	}

	/**
	 * Checks if a cloud animation indicates it's safe
	 * @param cloudAnimationId The cloud's current animation ID
	 * @return true if the cloud is safe (harmless animation)
	 */
	private boolean isCloudAnimationSafe(int cloudAnimationId)
	{
		return cloudAnimationId == State.CLOUD_ANIM_HARMLESS || cloudAnimationId == State.CLOUD_ANIM_HARMLESS_ALT;
	}

	/**
	 * Checks if a cloud is dangerous based on its animation
	 * @param lightningCloud The cloud NPC to check
	 * @return true if the cloud is dangerous
	 */
	public boolean isCloudCurrentlyDangerous(NPC lightningCloud)
	{
		int cloudAnimationId = lightningCloud.getAnimation();
		return !isCloudAnimationSafe(cloudAnimationId);
	}

	/**
	 * Generates a circular path around the exclusion zone
	 * Used as a fallback when no specific targets are available
	 */
	public List<WorldPoint> generateCircularPathAroundExclusionZone(WorldPoint startLocation)
	{
		List<WorldPoint> circularPath = new ArrayList<>();

		if (state.getRumReturnLocation() == null)
		{
			return circularPath;
		}

		int exclusionZoneCenterX = (state.getExclusionZoneMinX() + state.getExclusionZoneMaxX()) / 2;
		int exclusionZoneCenterY = (state.getExclusionZoneMinY() + state.getExclusionZoneMaxY()) / 2;

		// 10 tile buffer around exclusion zone
		int circularPathRadiusX = (state.getExclusionZoneMaxX() - state.getExclusionZoneMinX()) / 2 + 10;
		int circularPathRadiusY = (state.getExclusionZoneMaxY() - state.getExclusionZoneMinY()) / 2 + 10;

		double startAngleFromCenter = Math.atan2(startLocation.getY() - exclusionZoneCenterY, startLocation.getX() - exclusionZoneCenterX);

		for (int waypointIndex = 1; waypointIndex <= 8; waypointIndex++)
		{
			double currentAngle = startAngleFromCenter - (waypointIndex * Math.PI / 4);

			int waypointX = (int) (exclusionZoneCenterX + circularPathRadiusX * Math.cos(currentAngle));
			int waypointY = (int) (exclusionZoneCenterY + circularPathRadiusY * Math.sin(currentAngle));

			circularPath.add(new WorldPoint(waypointX, waypointY, startLocation.getPlane()));
		}

		return circularPath;
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
