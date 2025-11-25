package com.barracudatrial.game;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.game.route.*;
import com.barracudatrial.pathfinding.AStarPathfinder;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import com.barracudatrial.pathfinding.PathStabilizer;
import com.barracudatrial.rendering.ObjectRenderer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.World;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

@Slf4j
public class PathPlanner
{
	private final State state;
	private final CachedConfig cachedConfig;
	private final Client client;
	private final PathStabilizer pathStabilizer;

	public PathPlanner(Client client, State state, CachedConfig cachedConfig)
	{
		this.client = client;
		this.state = state;
		this.cachedConfig = cachedConfig;

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

	private void loadStaticRouteForCurrentDifficulty()
	{
		var trial = state.getCurrentTrial();
		if (trial == null)
		{
			log.warn("Trial config not initialized, cannot load route");
			state.setCurrentStaticRoute(new ArrayList<>());
			return;
		}

		Difficulty difficulty = state.getCurrentDifficulty();
		List<RouteWaypoint> staticRoute = trial.getRoute(difficulty);

		if (staticRoute == null || staticRoute.isEmpty())
		{
			log.warn("No static route found for trial {} difficulty: {}", trial.getTrialType(), difficulty);
			state.setCurrentStaticRoute(new ArrayList<>());
			return;
		}

		state.setCurrentStaticRoute(staticRoute);
		state.setNextWaypointIndex(0);
		log.debug("Loaded static route for {} difficulty {} with {} waypoints",
			trial.getTrialType(), difficulty, staticRoute.size());
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

		var route = state.getCurrentStaticRoute();
		if (route == null || route.isEmpty())
		{
			return uncompletedWaypoints;
		}

		int routeSize = route.size();
		boolean foundFirst = false;

		for (int offset = 0; offset < routeSize && uncompletedWaypoints.size() < count; offset++)
		{
			int checkIndex = (state.getNextWaypointIndex() + offset) % routeSize;
			RouteWaypoint waypoint = route.get(checkIndex);

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
		boolean isPlayerCurrentlyOnPath = true;

		for (RouteWaypoint waypoint : waypoints)
		{
			WorldPoint target = waypoint.getLocation();

			// If target is out of the extended scene, find the nearest in-scene tile along the path
			WorldPoint pathfindingTarget = getInSceneTarget(currentPosition, target);

			int initialBoatDx;
			int initialBoatDy;

			if (fullPath.isEmpty())
			{
				// First segment: use actual boat heading from state
				WorldPoint frontBoatTile = state.getFrontBoatTileEstimatedActual();
				WorldPoint backBoatTile = state.getBoatLocation();
				if (frontBoatTile != null && backBoatTile != null)
				{
					initialBoatDx = frontBoatTile.getX() - backBoatTile.getX();
					initialBoatDy = frontBoatTile.getY() - backBoatTile.getY();
				}
				else
				{
					initialBoatDx = 0;
					initialBoatDy = 0;
				}
			}
			else
			{
				// Subsequent segments: derive heading from last step of the accumulated fullPath
				if (fullPath.size() >= 2)
				{
					WorldPoint prev = fullPath.get(fullPath.size() - 2);
					WorldPoint last = fullPath.get(fullPath.size() - 1);
					initialBoatDx = last.getX() - prev.getX();
					initialBoatDy = last.getY() - prev.getY();
				}
				else
				{
					// If we don't have two path points, prefer a neutral heading (0,0)
					initialBoatDx = 0;
					initialBoatDy = 0;
				}
			}

			List<WorldPoint> segmentPath = pathToSingleTarget(currentPosition, pathfindingTarget, waypoint.getType().getToleranceTiles(), isPlayerCurrentlyOnPath, initialBoatDx, initialBoatDy);

			if (fullPath.isEmpty())
			{
				fullPath.addAll(segmentPath);
			}
			else if (!segmentPath.isEmpty())
			{
				// Skip first point to avoid duplicates (end of previous segment = start of next segment)
				fullPath.addAll(segmentPath.subList(1, segmentPath.size()));
			}

			currentPosition = segmentPath.isEmpty() ? currentPosition : segmentPath.get(segmentPath.size() - 1);
			isPlayerCurrentlyOnPath = false;
		}

		return fullPath;
	}

	/**
	 * Paths from current position to a single target using A*
	 * @param start Starting position
	 * @param target Target position
	 * @param goalTolerance Number of tiles away from target that counts as reaching it (0 = exact)
	 * @param isPlayerCurrentlyOnPath Whether or not this is the path that the player is currently navigating
	 * @return Path from start to target
	 */
	private List<WorldPoint> pathToSingleTarget(WorldPoint start, WorldPoint target, int goalTolerance, boolean isPlayerCurrentlyOnPath, int initialBoatDx, int initialBoatDy)
	{
		var tileCostCalculator = getBarracudaTileCostCalculator();

		// Use provided initial heading (may be 0,0 for neutral)
		int boatDirectionDx = initialBoatDx;
		int boatDirectionDy = initialBoatDy;

		int tileDistance = start.distanceTo(target); // Chebyshev distance in tiles
		
		// Never too high, but allow seeking longer on long paths
		int maximumAStarSearchDistance = Math.max(50, Math.min(280, tileDistance * 20));

		var currentStaticRoute = state.getCurrentStaticRoute();

		List<WorldPoint> path = pathStabilizer.findPath(tileCostCalculator, cachedConfig.getRouteOptimization(), currentStaticRoute, start, target, maximumAStarSearchDistance, boatDirectionDx, boatDirectionDy, goalTolerance, isPlayerCurrentlyOnPath);

		if (path.isEmpty())
		{
			List<WorldPoint> fallbackPath = new ArrayList<>();
			fallbackPath.add(target);
			return fallbackPath;
		}

		return path;
	}

	private BarracudaTileCostCalculator getBarracudaTileCostCalculator()
	{
		Set<NPC> currentlyDangerousClouds = new HashSet<>();
		for (NPC lightningCloud : state.getLightningClouds())
		{
			if (!ObjectTracker.IsCloudSafe(lightningCloud.getAnimation()))
			{
				currentlyDangerousClouds.add(lightningCloud);
			}
		}

		var trial = state.getCurrentTrial();
		var boatExclusionWidth = trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM
			? TemporTantrumConfig.BOAT_EXCLUSION_WIDTH
			: JubblyJiveConfig.BOAT_EXCLUSION_WIDTH;
		var boatExclusionHeight = trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM
			? TemporTantrumConfig.BOAT_EXCLUSION_HEIGHT
			: JubblyJiveConfig.BOAT_EXCLUSION_HEIGHT;

		WorldPoint secondaryObjectiveLocation = null;

		if (trial != null)
		{
			var trialType = trial.getTrialType();

			if (trialType == TrialType.TEMPOR_TANTRUM)
			{
				secondaryObjectiveLocation = state.getRumReturnLocation();
			}
			else if (trialType == TrialType.JUBBLY_JIVE)
			{
				var route = state.getCurrentStaticRoute();
				if (route != null && !route.isEmpty())
				{
					var completed = state.getCompletedWaypointIndices();
					for (int i = 0; i < route.size(); i++)
					{
						if (completed.contains(i))
							continue;

						var waypoint = route.get(i);
						if (waypoint.getType() == RouteWaypoint.WaypointType.TOAD_PILLAR)
						{
							secondaryObjectiveLocation = waypoint.getLocation();
							break;
						}
					}
				}
			}
		}

		return new BarracudaTileCostCalculator(
			state.getKnownSpeedBoostLocations(),
			state.getKnownRockLocations(),
			state.getKnownFetidPoolLocations(),
			state.getKnownToadPillarLocations(),
			currentlyDangerousClouds,
			state.getExclusionZoneMinX(),
			state.getExclusionZoneMaxX(),
			state.getExclusionZoneMinY(),
			state.getExclusionZoneMaxY(),
			state.getRumPickupLocation(),
			secondaryObjectiveLocation,
			cachedConfig.getRouteOptimization(),
			boatExclusionWidth,
			boatExclusionHeight
		);
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
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return target;
		}

		// Check if target is already in extended scene
		LocalPoint targetLocal = ObjectRenderer.localPointFromWorldIncludingExtended(worldView, target);
		if (targetLocal != null)
		{
			return target;
		}

		// Target is out of extended scene - binary search for the furthest visible tile
		return findNearestValidPoint(start, target, candidate ->
			ObjectRenderer.localPointFromWorldIncludingExtended(worldView, candidate) != null
		);
	}

	/**
	 * Finds the furthest point from start toward target that satisfies the given validation function.
	 * Uses binary search for O(log n) efficiency.
	 * @param start Starting position
	 * @param target Desired target position
	 * @param isValid Function that returns true if a candidate point is valid
	 * @return The furthest valid point toward target, or start if none found
	 */
	private static WorldPoint findNearestValidPoint(WorldPoint start, WorldPoint target, java.util.function.Predicate<WorldPoint> isValid)
	{
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

			if (isValid.test(candidate))
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
}
