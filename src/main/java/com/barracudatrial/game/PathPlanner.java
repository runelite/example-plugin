package com.barracudatrial.game;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.game.route.TemporTantrumRoutes;
import com.barracudatrial.pathfinding.AStarPathfinder;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import com.barracudatrial.pathfinding.PathStabilizer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

@Slf4j
public class PathPlanner
{
	private final State state;
	private final CachedConfig cachedConfig;
	private final LocationHelper locationHelper;
	private final PathStabilizer pathStabilizer;

	public PathPlanner(State state, CachedConfig cachedConfig, LocationHelper locationHelper)
	{
		this.state = state;
		this.cachedConfig = cachedConfig;
		this.locationHelper = locationHelper;

		AStarPathfinder aStarPathfinder = new AStarPathfinder();
		this.pathStabilizer = new PathStabilizer(aStarPathfinder);
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

		List<RouteWaypoint> nextWaypoints = findNextUncompletedWaypoints(cachedConfig.getPathLookahead());

		if (nextWaypoints.isEmpty())
		{
			state.setCurrentSegmentPath(new ArrayList<>());
			state.setNextSegmentPath(new ArrayList<>());
			state.setOptimalPath(new ArrayList<>());
			log.debug("No uncompleted waypoints found in static route");
			return;
		}

		List<WorldPoint> fullPath = pathThroughMultipleWaypoints(playerBoatLocation, nextWaypoints);

		state.setCurrentSegmentPath(fullPath);
		state.setOptimalPath(new ArrayList<>(fullPath));
		state.setNextSegmentPath(new ArrayList<>());

		log.debug("Pathing through {} waypoints starting at index {}", nextWaypoints.size(), state.getNextWaypointIndex());
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
	 * @return List of uncompleted waypoints in route order
	 */
	private List<RouteWaypoint> findNextUncompletedWaypoints(int count)
	{
		List<RouteWaypoint> uncompletedWaypoints = new ArrayList<>();

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

			if (!state.isWaypointCompleted(checkIndex))
			{
				if (!foundFirst)
				{
					state.setNextWaypointIndex(checkIndex);
					foundFirst = true;
				}
				uncompletedWaypoints.add(waypoint);
			}
		}

		return uncompletedWaypoints;
	}

	/**
	 * Paths through multiple waypoints in sequence using A*
	 * @param start Starting position
	 * @param waypoints List of waypoints to path through in order
	 * @return Complete path through all waypoints
	 */
	private List<WorldPoint> pathThroughMultipleWaypoints(WorldPoint start, List<RouteWaypoint> waypoints)
	{
		if (waypoints.isEmpty())
		{
			return new ArrayList<>();
		}

		List<WorldPoint> fullPath = new ArrayList<>();
		WorldPoint currentPosition = start;

		for (RouteWaypoint waypoint : waypoints)
		{
			WorldPoint target = waypoint.getLocation();

			// If target is out of the extended scene, find the nearest in-scene tile along the path
			WorldPoint pathfindingTarget = getInSceneTarget(currentPosition, target);

			List<WorldPoint> segmentPath = pathToSingleTarget(currentPosition, pathfindingTarget, waypoint.getType().getToleranceTiles());

			if (fullPath.isEmpty())
			{
				fullPath.addAll(segmentPath);
			}
			else if (!segmentPath.isEmpty())
			{
				// Skip first point to avoid duplicates (end of previous segment = start of next segment)
				fullPath.addAll(segmentPath.subList(1, segmentPath.size()));
			}

			currentPosition = pathfindingTarget;
		}

		return fullPath;
	}

	/**
	 * Paths from current position to a single target using A*
	 * @param start Starting position
	 * @param target Target position
	 * @param goalTolerance Number of tiles away from target that counts as reaching it (0 = exact)
	 * @return Path from start to target
	 */
	private List<WorldPoint> pathToSingleTarget(WorldPoint start, WorldPoint target, int goalTolerance)
	{
		Set<NPC> currentlyDangerousClouds = new HashSet<>();
		for (NPC lightningCloud : state.getLightningClouds())
		{
			if (!ObjectTracker.IsCloudSafe(lightningCloud.getAnimation()))
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
			state.getExclusionZoneMaxY(),
			cachedConfig.getRouteOptimization()
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

		int maximumAStarSearchDistance = 250;
		List<WorldPoint> path = pathStabilizer.findPath(tileCostCalculator, cachedConfig.getRouteOptimization(), start, target, maximumAStarSearchDistance, boatDirectionDx, boatDirectionDy, goalTolerance);

		if (path.isEmpty())
		{
			List<WorldPoint> fallbackPath = new ArrayList<>();
			fallbackPath.add(target);
			return fallbackPath;
		}

		if (path.get(0).equals(start))
		{
			path.remove(0);
		}

		return path;
	}

	/**
	 * Returns the target if it's in the extended scene, otherwise finds the nearest in-scene tile
	 * along the path from start to target using efficient binary search.
	 * @param start Starting position
	 * @param target Desired target position
	 * @return Target if in scene, otherwise nearest in-scene tile toward target
	 */
	private WorldPoint getInSceneTarget(WorldPoint start, WorldPoint target)
	{
		net.runelite.api.WorldView worldView = locationHelper.getTopLevelWorldView();
		if (worldView == null)
		{
			return target;
		}

		// Check if target is already in scene
		net.runelite.api.coords.LocalPoint targetLocal = net.runelite.api.coords.LocalPoint.fromWorld(worldView, target);
		if (targetLocal != null)
		{
			return target;
		}

		// Target is out of scene - binary search for the furthest visible tile
		int dx = target.getX() - start.getX();
		int dy = target.getY() - start.getY();
		int maxDistance = Math.max(Math.abs(dx), Math.abs(dy));

		if (maxDistance < 1)
		{
			return start;
		}

		int plane = start.getPlane();
		int low = 0;
		int high = maxDistance;
		WorldPoint bestCandidate = start;

		while (low <= high)
		{
			int mid = (low + high) / 2;
			int x = start.getX() + (dx * mid / maxDistance);
			int y = start.getY() + (dy * mid / maxDistance);
			WorldPoint candidate = new WorldPoint(x, y, plane);

			net.runelite.api.coords.LocalPoint lp = net.runelite.api.coords.LocalPoint.fromWorld(worldView, candidate);
			if (lp != null)
			{
				bestCandidate = candidate;
				low = mid + 1;
			}
			else
			{
				high = mid - 1;
			}
		}

		return bestCandidate;
	}

	public void reset()
	{
		pathStabilizer.clearActivePath();
	}

	/**
	 * Helper interface for location calculations
	 * Provides pathfinding-adjusted locations for rum pickup/dropoff
	 */
	public interface LocationHelper
	{
		WorldPoint getPathfindingPickupLocation();
		WorldPoint getPathfindingDropoffLocation();
		net.runelite.api.WorldView getTopLevelWorldView();
	}
}
