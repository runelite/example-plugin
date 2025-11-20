package com.barracudatrial.pathfinding;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Optimizes the order of lost supplies collection using A* pathfinding
 * Uses strategic pathfinding (considers rocks and speed boosts, ignores clouds)
 */
public class LostSuppliesOrderOptimizer
{
	private final TileCostCalculator costCalculator;
	private final AStarPathfinder pathfinder;

	public LostSuppliesOrderOptimizer(TileCostCalculator costCalculator)
	{
		this.costCalculator = costCalculator;
		this.pathfinder = new AStarPathfinder(costCalculator);
	}

	/**
	 * Finds the optimal order to collect lost supplies using greedy nearest-neighbor with A* pathfinding
	 * @param start Starting position
	 * @param lostSupplies Set of lost supplies to collect
	 * @param maxSearchDistance Maximum search distance for A* (prevents infinite loops)
	 * @return List of lost supplies locations in optimal collection order
	 */
	public List<WorldPoint> findOptimalOrder(WorldPoint start, Set<GameObject> lostSupplies, int maxSearchDistance)
	{
		List<WorldPoint> collectionOrder = new ArrayList<>();
		Set<GameObject> remainingSupplies = new HashSet<>(lostSupplies);
		WorldPoint currentPosition = start;

		// Greedy nearest-neighbor with A* path cost instead of straight-line distance
		while (!remainingSupplies.isEmpty())
		{
			GameObject nearestSupply = null;
			double minCost = Double.MAX_VALUE;

			for (GameObject supply : remainingSupplies)
			{
				WorldPoint supplyLocation = supply.getWorldLocation();

				List<WorldPoint> path = pathfinder.findPath(currentPosition, supplyLocation, maxSearchDistance);

				if (path.isEmpty())
				{
					continue;
				}

				double pathCost = calculatePathCost(path);

				if (pathCost < minCost)
				{
					minCost = pathCost;
					nearestSupply = supply;
				}
			}

			if (nearestSupply != null)
			{
				WorldPoint supplyLocation = nearestSupply.getWorldLocation();
				collectionOrder.add(supplyLocation);
				remainingSupplies.remove(nearestSupply);
				currentPosition = supplyLocation;
			}
			else
			{
				break;
			}
		}

		return collectionOrder;
	}

	private double calculatePathCost(List<WorldPoint> path)
	{
		if (path.size() < 2)
		{
			return 0;
		}

		// Reset speed boost state for accurate cost calculation per path
		if (costCalculator instanceof StrategicTileCostCalculator)
		{
			((StrategicTileCostCalculator) costCalculator).resetSpeedBoostState();
		}
		else if (costCalculator instanceof BarracudaTileCostCalculator)
		{
			((BarracudaTileCostCalculator) costCalculator).resetSpeedBoostState();
		}

		double totalCost = 0;
		for (int i = 0; i < path.size() - 1; i++)
		{
			totalCost += costCalculator.getTileCost(path.get(i), path.get(i + 1));
		}

		return totalCost;
	}

	/**
	 * Finds the actual A* path between two points
	 * Used for getting the real path to follow (not just the lost supplies order)
	 */
	public List<WorldPoint> findPath(WorldPoint start, WorldPoint goal, int maxSearchDistance)
	{
		return pathfinder.findPath(start, goal, maxSearchDistance);
	}
}
