package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class PathStabilizer
{
	private final AStarPathfinder pathfinder;
	private PathResult activePathResult;

    public PathStabilizer(AStarPathfinder pathfinder)
	{
		this.pathfinder = pathfinder;
		this.activePathResult = null;
	}

	private double getImprovementThreshold(RouteOptimization optimization)
	{
		return optimization == RouteOptimization.EFFICIENT ? 0.90 : 0.70;
	}

	public List<WorldPoint> findPath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, WorldPoint goal, int maxSearchDistance,
	                                  int boatDirectionDx, int boatDirectionDy, int goalTolerance)
	{
		PathResult newPathResult = pathfinder.findPath(costCalculator, routeOptimization, start, goal, maxSearchDistance, boatDirectionDx, boatDirectionDy, goalTolerance);

		if (shouldForceNewPath(newPathResult, goal))
		{
			activePathResult = newPathResult;
			return newPathResult.getPath();
		}

		if (shouldKeepActivePath(routeOptimization, start, newPathResult))
		{
			return getTrimmedPath(start, activePathResult);
		}

		activePathResult = newPathResult;
		return newPathResult.getPath();
	}

	private boolean shouldForceNewPath(PathResult newPathResult, WorldPoint goal)
	{
		if (activePathResult == null || activePathResult.getPath().isEmpty())
		{
			return true;
		}

		if (newPathResult.getPath().isEmpty())
		{
			return true;
		}

		List<WorldPoint> activePath = activePathResult.getPath();
		WorldPoint activeGoal = activePath.get(activePath.size() - 1);
        return !activeGoal.equals(goal);
    }

	private boolean shouldKeepActivePath(RouteOptimization routeOptimization, WorldPoint start, PathResult newPathResult)
	{
		List<WorldPoint> activePath = activePathResult.getPath();

		if (!isWithinProximityOfPath(start, activePath))
		{
			return false;
		}

        return !isNewPathSignificantlyBetter(routeOptimization, start, newPathResult);
    }

	private boolean isWithinProximityOfPath(WorldPoint start, List<WorldPoint> path)
	{
		for (WorldPoint pathPoint : path)
		{
			int dx = Math.abs(start.getX() - pathPoint.getX());
			int dy = Math.abs(start.getY() - pathPoint.getY());
			int chebyshevDistance = Math.max(dx, dy);

            int pathProximityTolerance = 2.5;
            if (chebyshevDistance <= pathProximityTolerance)
			{
				return true;
			}
		}
		return false;
	}

	private boolean isNewPathSignificantlyBetter(RouteOptimization routeOptimization, WorldPoint start, PathResult newPathResult)
	{
		double newCost = newPathResult.getCost();

		int closestIndex = findClosestPointOnPath(start, activePathResult.getPath());
		double oldRemainingCost = activePathResult.getCostFromIndex(closestIndex);

		double improvementThreshold = getImprovementThreshold(routeOptimization);

		return newCost <= improvementThreshold * oldRemainingCost;
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
		this.activePathResult = null;
	}
}
