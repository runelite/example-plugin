package com.barracudatrial.game;

import com.barracudatrial.BarracudaTrialConfig;
import com.barracudatrial.CachedConfig;
import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.game.route.TemporTantrumRoutes;
import com.barracudatrial.pathfinding.AStarPathfinder;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Orchestrates path calculation for the Barracuda Trial plugin
 * Uses static routes for strategic planning and A* pathfinding for tactical navigation
 */
@Slf4j
public class PathPlanner
{
	private final Client client;
	private final State state;
	private final CachedConfig cachedConfig;
	private final LocationHelper locationHelper;

	public PathPlanner(Client client, State state, CachedConfig cachedConfig, LocationHelper locationHelper)
	{
		this.client = client;
		this.state = state;
		this.cachedConfig = cachedConfig;
		this.locationHelper = locationHelper;
	}

	/**
	 * Recalculates the optimal path based on current game state
	 * Uses static routes for strategic planning and A* for tactical navigation
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
			return;
		}

		state.setTicksSinceLastPathRecalc(0);

		// Use front boat tile for pathfinding (fallback to center if not available)
		WorldPoint playerBoatLocation = state.getFrontBoatTileEstimatedActual();
		if (playerBoatLocation == null)
		{
			playerBoatLocation = state.getBoatLocation();
		}
		if (playerBoatLocation == null)
		{
			return;
		}

		if (state.getCurrentStaticRoute() == null)
		{
			loadStaticRouteForCurrentDifficulty();
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

		List<WorldPoint> nextTargets = findNextUncompletedWaypoints(cachedConfig.getPathLookahead());

		if (nextTargets.isEmpty())
		{
			state.setCurrentSegmentPath(new ArrayList<>());
			state.setNextSegmentPath(new ArrayList<>());
			state.setOptimalPath(new ArrayList<>());
			log.debug("No uncompleted waypoints found in static route");
			return;
		}

		List<WorldPoint> fullPath = pathThroughMultipleTargets(playerBoatLocation, nextTargets);

		state.setCurrentSegmentPath(fullPath);
		state.setOptimalPath(new ArrayList<>(fullPath));
		state.setNextSegmentPath(new ArrayList<>());

		log.debug("Pathing through {} waypoints starting at index {}", nextTargets.size(), state.getNextWaypointIndex());
	}

	/**
	 * Loads the static route for the current difficulty from TemporTantrumRoutes
	 */
	private void loadStaticRouteForCurrentDifficulty()
	{
		Difficulty difficulty = state.getCurrentDifficulty();
		List<RouteWaypoint> staticRoute = TemporTantrumRoutes.getRoute(difficulty);

		if (staticRoute == null || staticRoute.isEmpty())
		{
			log.warn("No static route found for difficulty: {}", difficulty);
			state.setCurrentStaticRoute(new ArrayList<>());
			return;
		}

		state.setCurrentStaticRoute(staticRoute);
		state.setNextWaypointIndex(0);
		log.debug("Loaded static route for difficulty {} with {} waypoints", difficulty, staticRoute.size());
	}

	/**
	 * Finds the next N uncompleted waypoints in the static route sequence.
	 * Routes to waypoints even if not yet visible (game only reveals nearby shipments).
	 * Supports backtracking if a waypoint was missed.
	 *
	 * @param count Maximum number of uncompleted waypoints to return
	 * @return List of uncompleted waypoint locations in route order
	 */
	private List<WorldPoint> findNextUncompletedWaypoints(int count)
	{
		List<WorldPoint> uncompletedWaypoints = new ArrayList<>();

		if (state.getCurrentStaticRoute() == null || state.getCurrentStaticRoute().isEmpty())
		{
			return uncompletedWaypoints;
		}

		int routeSize = state.getCurrentStaticRoute().size();
		boolean foundFirst = false;

		for (int offset = 0; offset < routeSize && uncompletedWaypoints.size() < count; offset++)
		{
			int checkIndex = (state.getNextWaypointIndex() + offset) % routeSize;
			RouteWaypoint waypoint = state.getCurrentStaticRoute().get(checkIndex);
			WorldPoint waypointLocation = waypoint.getLocation();

			if (!state.isWaypointCompleted(waypointLocation))
			{
				if (!foundFirst)
				{
					state.setNextWaypointIndex(checkIndex);
					foundFirst = true;
				}
				uncompletedWaypoints.add(waypointLocation);
			}
		}

		return uncompletedWaypoints;
	}

	/**
	 * Paths through multiple targets in sequence using A*
	 * @param start Starting position
	 * @param targets List of targets to path through in order
	 * @return Complete path through all targets
	 */
	private List<WorldPoint> pathThroughMultipleTargets(WorldPoint start, List<WorldPoint> targets)
	{
		if (targets.isEmpty())
		{
			return new ArrayList<>();
		}

		List<WorldPoint> fullPath = new ArrayList<>();
		WorldPoint currentPosition = start;

		for (WorldPoint target : targets)
		{
			List<WorldPoint> segmentPath = pathToSingleTarget(currentPosition, target);

			if (fullPath.isEmpty())
			{
				fullPath.addAll(segmentPath);
			}
			else if (!segmentPath.isEmpty())
			{
				// Skip first point to avoid duplicates (end of previous segment = start of next segment)
				fullPath.addAll(segmentPath.subList(1, segmentPath.size()));
			}

			currentPosition = target;
		}

		return fullPath;
	}

	/**
	 * Paths from current position to a single target using A*
	 * @param start Starting position
	 * @param target Target position
	 * @return Path from start to target
	 */
	private List<WorldPoint> pathToSingleTarget(WorldPoint start, WorldPoint target)
	{
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

		// Calculate boat direction for A* forward constraint
		// Direction = front tile - back tile (player position)
		int boatDirectionDx = 0;
		int boatDirectionDy = 0;
		WorldPoint frontBoatTile = state.getFrontBoatTileEstimatedActual();
		WorldPoint backBoatTile = state.getBoatLocation();
		if (frontBoatTile != null && backBoatTile != null)
		{
			boatDirectionDx = frontBoatTile.getX() - backBoatTile.getX();
			boatDirectionDy = frontBoatTile.getY() - backBoatTile.getY();
		}

		AStarPathfinder aStarPathfinder = new AStarPathfinder(tileCostCalculator);
		int maximumAStarSearchDistance = 100;

		List<WorldPoint> path = aStarPathfinder.findPath(start, target, maximumAStarSearchDistance, boatDirectionDx, boatDirectionDy);

		if (path.isEmpty())
		{
			List<WorldPoint> fallbackPath = new ArrayList<>();
			fallbackPath.add(target);
			return fallbackPath;
		}

		// Remove starting position from path (we're already there)
		if (!path.isEmpty() && path.get(0).equals(start))
		{
			path.remove(0);
		}

		return path;
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
	 * Helper interface for location calculations
	 * Provides pathfinding-adjusted locations for rum pickup/dropoff
	 */
	public interface LocationHelper
	{
		WorldPoint getPathfindingPickupLocation();
		WorldPoint getPathfindingDropoffLocation();
	}
}
