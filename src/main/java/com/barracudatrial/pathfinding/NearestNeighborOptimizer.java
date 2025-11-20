package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Nearest Neighbor algorithm for TSP
 * Greedy algorithm: repeatedly visit the closest unvisited location
 */
public class NearestNeighborOptimizer
{
	/**
	 * Finds a route through all lost supplies using nearest neighbor heuristic
	 * @param start Starting position
	 * @param lostSupplies Set of lost supplies to visit
	 * @param distanceMatrix Pre-computed distances between points
	 * @param preferWestStart If true, prefer starting with lost supplies west of start
	 * @return Ordered list of lost supplies to visit
	 */
	public List<WorldPoint> findRoute(
		WorldPoint start,
		Set<WorldPoint> lostSupplies,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix,
		boolean preferWestStart)
	{
		if (lostSupplies.isEmpty())
		{
			return new ArrayList<>();
		}

		List<WorldPoint> route = new ArrayList<>();
		Set<WorldPoint> remaining = new HashSet<>(lostSupplies);
		WorldPoint current = start;

		// First lost supply: prefer direction if specified
		WorldPoint first = findFirstSupply(current, remaining, distanceMatrix, preferWestStart);
		if (first != null)
		{
			route.add(first);
			remaining.remove(first);
			current = first;
		}

		// Rest of lost supplies: use normal nearest neighbor
		while (!remaining.isEmpty())
		{
			WorldPoint nearest = findNearest(current, remaining, distanceMatrix);

			if (nearest == null)
			{
				// No reachable lost supplies, add remaining in arbitrary order
				route.addAll(remaining);
				break;
			}

			route.add(nearest);
			remaining.remove(nearest);
			current = nearest;
		}

		return route;
	}

	/**
	 * Finds the first lost supply, preferring west or east direction from start
	 * @param preferWest If true, prefer west; if false, prefer east
	 */
	private WorldPoint findFirstSupply(
		WorldPoint start,
		Set<WorldPoint> lostSupplies,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix,
		boolean preferWest)
	{
		WorldPoint preferredNearest = null;
		WorldPoint fallbackNearest = null;
		double preferredMinDist = Double.MAX_VALUE;
		double fallbackMinDist = Double.MAX_VALUE;

		for (WorldPoint supply : lostSupplies)
		{
			double dist = getDistance(start, supply, distanceMatrix);
			boolean isPreferredDirection = preferWest ? (supply.getX() < start.getX()) : (supply.getX() > start.getX());

			if (isPreferredDirection)
			{
				// Lost supply in preferred direction
				if (dist < preferredMinDist)
				{
					preferredMinDist = dist;
					preferredNearest = supply;
				}
			}
			else
			{
				// Lost supply in opposite direction (fallback)
				if (dist < fallbackMinDist)
				{
					fallbackMinDist = dist;
					fallbackNearest = supply;
				}
			}
		}

		return preferredNearest != null ? preferredNearest : fallbackNearest;
	}

	/**
	 * Finds the nearest unvisited lost supply to current position
	 */
	private WorldPoint findNearest(
		WorldPoint current,
		Set<WorldPoint> remaining,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		WorldPoint nearest = null;
		double minDist = Double.MAX_VALUE;

		for (WorldPoint supply : remaining)
		{
			double dist = getDistance(current, supply, distanceMatrix);

			if (dist < minDist)
			{
				minDist = dist;
				nearest = supply;
			}
		}

		return nearest;
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
