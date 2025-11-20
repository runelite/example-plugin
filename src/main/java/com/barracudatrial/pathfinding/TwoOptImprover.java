package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.*;

public class TwoOptImprover
{
	private static final int MAX_ITERATIONS = 20;

	public List<WorldPoint> improve(
		List<WorldPoint> route,
		WorldPoint startPoint,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		if (route.size() < 2)
		{
			return route;
		}

		boolean improved = true;
		int iterations = 0;

		while (improved && iterations < MAX_ITERATIONS)
		{
			improved = false;
			iterations++;

			for (int i = 0; i < route.size() - 1; i++)
			{
				for (int j = i + 2; j < route.size(); j++)
				{
					if (trySwap(route, i, j, startPoint, distanceMatrix))
					{
						improved = true;
					}
				}
			}
		}

		return route;
	}

	private boolean trySwap(
		List<WorldPoint> route,
		int i,
		int j,
		WorldPoint startPoint,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		WorldPoint previousToI = (i == 0) ? startPoint : route.get(i - 1);
		WorldPoint pointAtI = route.get(i);
		WorldPoint pointAfterI = route.get(i + 1);

		WorldPoint pointAtJ = route.get(j);
		WorldPoint pointAfterJ = (j == route.size() - 1) ? null : route.get(j + 1);

		double currentCost = getDistance(previousToI, pointAtI, distanceMatrix) +
			getDistance(pointAtI, pointAfterI, distanceMatrix);

		if (pointAfterJ != null)
		{
			currentCost += getDistance(pointAtJ, pointAfterJ, distanceMatrix);
		}

		double newCost = getDistance(previousToI, pointAtJ, distanceMatrix) +
			getDistance(pointAtJ, pointAfterI, distanceMatrix);

		if (pointAfterJ != null)
		{
			newCost += getDistance(pointAtI, pointAfterJ, distanceMatrix);
		}

		if (newCost < currentCost - 0.001)
		{
			reverseSegment(route, i, j);
			return true;
		}

		return false;
	}

	private void reverseSegment(List<WorldPoint> route, int start, int end)
	{
		while (start < end)
		{
			WorldPoint temp = route.get(start);
			route.set(start, route.get(end));
			route.set(end, temp);
			start++;
			end--;
		}
	}

	private double getDistance(
		WorldPoint from,
		WorldPoint to,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		Map<WorldPoint, Double> fromDistances = distanceMatrix.get(from);

		if (fromDistances != null && fromDistances.containsKey(to))
		{
			return fromDistances.get(to);
		}

		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}
}
