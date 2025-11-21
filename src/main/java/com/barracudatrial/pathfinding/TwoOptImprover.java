package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * 2-opt algorithm for improving TSP routes
 * Iteratively swaps route segments to reduce total distance
 */
public class TwoOptImprover
{
	private static final int MAX_ITERATIONS = 20;

	/**
	 * Improves a route using 2-opt swaps
	 * @param route Current route through supplies
	 * @param startPoint Starting position (used for calculating edge costs)
	 * @param distanceMatrix Pre-computed distances between points
	 * @return Improved route (may be same as input if no improvements found)
	 */
	public List<WorldPoint> improveRoute(
		List<WorldPoint> route,
		WorldPoint startPoint,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		if (route.size() < 2)
		{
			return route;
		}

		boolean foundImprovement = true;
		int iterations = 0;

		// Keep trying swaps until no improvements found or max iterations reached
		// Max iterations prevents excessive optimization time for large routes
		while (foundImprovement && iterations < MAX_ITERATIONS)
		{
			foundImprovement = false;
			iterations++;

			for (int firstIndex = 0; firstIndex < route.size() - 1; firstIndex++)
			{
				for (int secondIndex = firstIndex + 2; secondIndex < route.size(); secondIndex++)
				{
					if (trySwapSegment(route, firstIndex, secondIndex, startPoint, distanceMatrix))
					{
						foundImprovement = true;
					}
				}
			}
		}

		return route;
	}

	/**
	 * Tries swapping a segment of the route and keeps it if it improves total distance
	 * @param firstIndex Start of segment to reverse
	 * @param secondIndex End of segment to reverse
	 * @return true if swap was beneficial and applied
	 */
	private boolean trySwapSegment(
		List<WorldPoint> route,
		int firstIndex,
		int secondIndex,
		WorldPoint startPoint,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		WorldPoint beforeSegment = (firstIndex == 0) ? startPoint : route.get(firstIndex - 1);
		WorldPoint segmentStart = route.get(firstIndex);
		WorldPoint afterSegmentStart = route.get(firstIndex + 1);

		WorldPoint segmentEnd = route.get(secondIndex);
		WorldPoint afterSegment = (secondIndex == route.size() - 1) ? null : route.get(secondIndex + 1);

		// Calculate cost of current edge configuration
		double currentCost = getDistance(beforeSegment, segmentStart, distanceMatrix) +
			getDistance(segmentStart, afterSegmentStart, distanceMatrix);

		if (afterSegment != null)
		{
			currentCost += getDistance(segmentEnd, afterSegment, distanceMatrix);
		}

		// Calculate cost after reversing segment
		double newCost = getDistance(beforeSegment, segmentEnd, distanceMatrix) +
			getDistance(segmentEnd, afterSegmentStart, distanceMatrix);

		if (afterSegment != null)
		{
			newCost += getDistance(segmentStart, afterSegment, distanceMatrix);
		}

		// Apply swap if it reduces cost (epsilon to avoid floating point noise)
		if (newCost < currentCost - 0.001)
		{
			reverseSegment(route, firstIndex, secondIndex);
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
