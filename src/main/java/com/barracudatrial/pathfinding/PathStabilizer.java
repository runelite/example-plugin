package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import com.barracudatrial.game.route.RouteWaypoint;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Wraps a path with the danger zones that existed when it was created.
 * We need this to detect NEWLY appearing dangers - if the path already went through
 * danger zones because there was no alternative, we shouldn't rebuild it unless
 * the situation changes (new lightning clouds, new rocks, etc.)
 */
@Getter
class StabilizedPath
{
	private final PathResult pathResult;
	private final Set<WorldPoint> dangerZonesAtCreation;

	public StabilizedPath(PathResult pathResult, Set<WorldPoint> dangerZonesAtCreation)
	{
		this.pathResult = pathResult;
		this.dangerZonesAtCreation = dangerZonesAtCreation;
	}

}

/**
 * Prevents path thrashing by keeping cached paths stable when the player is following them.
 * Goals:
 * - Don't switch paths just because we moved forward (natural cost reduction)
 * - Don't switch for tiny improvements (1 tile difference)
 * - DO switch when world state changes make the old path dangerous (new lightning clouds)
 * - Treat each waypoint segment independently (don't rebuild segment 1->2 because 2->3 improved)
 */
public class PathStabilizer
{
	private final AStarPathfinder pathfinder;
	private final Map<WorldPoint, StabilizedPath> activePathsByGoal;

    public PathStabilizer(AStarPathfinder pathfinder)
	{
		this.pathfinder = pathfinder;
		this.activePathsByGoal = new HashMap<>();
	}

	/**
	 * Returns the cost ratio threshold for switching paths.
	 * Lower = more stable (harder to switch). 0.90 = new path must be 10% better, 0.60 = 40% better.
	 * Prevents path thrashing from minor optimizations.
	 */
	private double getSwitchCostRatio(RouteOptimization optimization)
	{
		return optimization == RouteOptimization.EFFICIENT ? 0.85 : 0.70;
	}

	public List<WorldPoint> findPath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, List<RouteWaypoint> currentStaticRoute, WorldPoint start, WorldPoint goal, int maxSearchDistance,
	                                  int boatDirectionDx, int boatDirectionDy, int goalTolerance, boolean isPlayerCurrentlyOnPath)
	{
		PathResult newPathResult = pathfinder.findPath(costCalculator, routeOptimization, start, goal, maxSearchDistance, boatDirectionDx, boatDirectionDy, goalTolerance);
		Set<WorldPoint> currentDangerZones = costCalculator.getDangerZoneSnapshot();

		StabilizedPath activeStabilizedPath = activePathsByGoal.get(goal);
		PathResult activePathResult = activeStabilizedPath != null ? activeStabilizedPath.getPathResult() : null;

		if (shouldForceNewPath(activePathResult, newPathResult, goal))
		{
			activePathsByGoal.put(goal, new StabilizedPath(newPathResult, currentDangerZones));
			return newPathResult.getPath();
		}

		if (shouldKeepActivePath(costCalculator, routeOptimization, start, activeStabilizedPath, newPathResult, currentStaticRoute, currentDangerZones, isPlayerCurrentlyOnPath))
		{
			return getTrimmedPath(start, activePathResult);
		}

		activePathsByGoal.put(goal, new StabilizedPath(newPathResult, currentDangerZones));
		return newPathResult.getPath();
	}

	private boolean shouldForceNewPath(PathResult activePathResult, PathResult newPathResult, WorldPoint goal)
	{
		if (activePathResult == null || activePathResult.getPath().isEmpty())
		{
			return true;
		}

		if (newPathResult.getPath().isEmpty())
		{
			return true;
		}

		return false;
    }

	private boolean shouldKeepActivePath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, StabilizedPath activeStabilizedPath, PathResult newPathResult, List<RouteWaypoint> currentStaticRoute, Set<WorldPoint> currentDangerZones, boolean isPlayerCurrentlyOnPath)
	{
		PathResult activePathResult = activeStabilizedPath.getPathResult();

		// If old path goes through NEW danger zones, force new path
		int closestIndex = findClosestPointOnPath(start, activePathResult.getPath());
		Set<WorldPoint> oldDangerZones = activeStabilizedPath.getDangerZonesAtCreation();
		if (doesPathIntersectNewDangerZones(activePathResult, closestIndex, oldDangerZones, currentDangerZones))
		{
			return false;
		}

		if (isPlayerCurrentlyOnPath && !isWithinProximityOfPath(start, activePathResult, currentStaticRoute))
		{
			return false;
		}

        return !isNewPathSignificantlyBetter(costCalculator, routeOptimization, start, activePathResult, newPathResult);
    }

	private boolean isWithinProximityOfPath(WorldPoint start, PathResult pathResult, List<RouteWaypoint> currentStaticRoute)
	{
		var pathNodes = pathResult.getPathNodes();
		if (pathNodes.isEmpty())
		{
			return false;
		}

		int closestIndex = -1;
		int minDistance = Integer.MAX_VALUE;

		for (int i = 0; i < pathNodes.size(); i++)
		{
			var nodePosition = pathNodes.get(i).getPosition();
			int dx = Math.abs(start.getX() - nodePosition.getX());
			int dy = Math.abs(start.getY() - nodePosition.getY());
			int chebyshevDistance = Math.max(dx, dy);

			if (chebyshevDistance < minDistance)
			{
				minDistance = chebyshevDistance;
				closestIndex = i;
			}
		}

		// When you're at the start of a new segment (closestIndex=0), give extra leeway (5 tiles) for merging
		// onto the path. This handles waypoint pickups from a distance (~4 tiles). Once you're traveling along
		// the path (closestIndex>0), enforce stricter tolerance (3 tiles) - if you stray, recalculate.
		int tolerance = (closestIndex == 0) ? 5 : 3;

		return minDistance <= tolerance;
	}

	private boolean isNewPathSignificantlyBetter(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, PathResult activePathResult, PathResult newPathResult)
	{
		double newCost = newPathResult.getCost();

		int closestIndex = findClosestPointOnPath(start, activePathResult.getPath());
		double oldRemainingCost = activePathResult.getCostFromIndex(closestIndex);

		double switchCostRatio = getSwitchCostRatio(routeOptimization);

		return newCost <= switchCostRatio * oldRemainingCost;
	}

	private boolean doesPathIntersectNewDangerZones(PathResult pathResult, int fromIndex, Set<WorldPoint> oldDangerZones, Set<WorldPoint> currentDangerZones)
	{
		// Find danger zones that exist now but didn't exist when the path was created
		Set<WorldPoint> newDangerZones = new HashSet<>(currentDangerZones);
		newDangerZones.removeAll(oldDangerZones);

		// Check if path intersects any of these NEW danger zones
		List<PathNode> pathNodes = pathResult.getPathNodes();
		for (int i = fromIndex; i < pathNodes.size(); i++)
		{
			if (newDangerZones.contains(pathNodes.get(i).getPosition()))
			{
				return true;
			}
		}
		return false;
	}

	private List<WorldPoint> getTrimmedPath(WorldPoint start, PathResult pathResult)
	{
		List<WorldPoint> fullPath = pathResult.getPath();
		int closestIndex = findClosestPointOnPath(start, fullPath);

		// If player is exactly on the closest path tile, start from next tile
		if (closestIndex < fullPath.size() && fullPath.get(closestIndex).equals(start))
		{
			closestIndex++;
		}

		// Return remaining path from that point forward
		if (closestIndex >= fullPath.size())
		{
			return new ArrayList<>();
		}

		return new ArrayList<>(fullPath.subList(closestIndex, fullPath.size()));
	}

	private int findClosestPointOnPath(WorldPoint position, List<WorldPoint> path)
	{
		int closestIndex = 0;
		double minDistance = Double.POSITIVE_INFINITY;

		for (int i = 0; i < path.size(); i++)
		{
			int dx = position.getX() - path.get(i).getX();
			int dy = position.getY() - path.get(i).getY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			if (distance < minDistance)
			{
				minDistance = distance;
				closestIndex = i;
			}
		}

		return closestIndex;
	}

	public void clearActivePath()
	{
		this.activePathsByGoal.clear();
	}
}
