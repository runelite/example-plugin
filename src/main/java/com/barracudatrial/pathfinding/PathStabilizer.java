package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class PathStabilizer
{
	private final AStarPathfinder pathfinder;
	private PathResult activePathResult;
	private final int pathProximityTolerance = 2;

	public PathStabilizer(AStarPathfinder pathfinder)
	{
		this.pathfinder = pathfinder;
		this.activePathResult = null;
	}

	private double getImprovementThreshold(RouteOptimization optimization)
	{
		return optimization == RouteOptimization.EFFICIENT ? 0.95 : 0.80;
	}

	public List<WorldPoint> findPath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, WorldPoint goal, int maxSearchDistance,
	                                  int boatDirectionDx, int boatDirectionDy)
	{
		PathResult newPathResult = pathfinder.findPath(costCalculator, routeOptimization, start, goal, maxSearchDistance, boatDirectionDx, boatDirectionDy);

		if (shouldForceNewPath(newPathResult, goal))
		{
			activePathResult = newPathResult;
			return newPathResult.getPath();
		}

		if (shouldKeepActivePath(costCalculator, routeOptimization, start, newPathResult))
		{
			return activePathResult.getPath();
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

	private boolean shouldKeepActivePath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, PathResult newPathResult)
	{
		List<WorldPoint> activePath = activePathResult.getPath();

		if (!isWithinProximityOfPath(start, activePath))
		{
			return false;
		}

        return !isNewPathSignificantlyBetter(costCalculator, routeOptimization, start, newPathResult);
    }

	private boolean isWithinProximityOfPath(WorldPoint start, List<WorldPoint> path)
	{
		for (WorldPoint pathPoint : path)
		{
			int dx = Math.abs(start.getX() - pathPoint.getX());
			int dy = Math.abs(start.getY() - pathPoint.getY());
			int chebyshevDistance = Math.max(dx, dy);

			if (chebyshevDistance <= pathProximityTolerance)
			{
				return true;
			}
		}
		return false;
	}

	private boolean isNewPathSignificantlyBetter(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, PathResult newPathResult)
	{
		double newCost = newPathResult.getCost();
		double oldRemainingCost = estimateRemainingCost(costCalculator, start, activePathResult.getPath());
		double improvementThreshold = getImprovementThreshold(routeOptimization);

		return newCost <= improvementThreshold * oldRemainingCost;
	}

	private double estimateRemainingCost(BarracudaTileCostCalculator costCalculator, WorldPoint currentPosition, List<WorldPoint> oldPath)
	{
		int closestIndex = findClosestPointOnPath(currentPosition, oldPath);
		WorldPoint closestPoint = oldPath.get(closestIndex);

		double costToPath = costCalculator.getTileCost(currentPosition, closestPoint);

		double costAlongPath = 0.0;
		for (int i = closestIndex; i < oldPath.size() - 1; i++)
		{
			costAlongPath += costCalculator.getTileCost(oldPath.get(i), oldPath.get(i + 1));
		}

		return costToPath + costAlongPath;
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
