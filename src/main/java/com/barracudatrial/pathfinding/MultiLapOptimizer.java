package com.barracudatrial.pathfinding;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class MultiLapOptimizer
{
	private final DistanceMatrixBuilder matrixBuilder;
	private final LostSuppliesPartitioner partitioner;
	private final NearestNeighborOptimizer nearestNeighbor;
	private final TwoOptImprover twoOpt;
	private final boolean preferWestStart;
	private final int exclusionZoneCenterX;

	public MultiLapOptimizer(
		Set<WorldPoint> rockLocations,
		boolean preferWestStart,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY)
	{
		this.matrixBuilder = new DistanceMatrixBuilder(
			rockLocations,
			exclusionZoneMinX,
			exclusionZoneMaxX,
			exclusionZoneMinY,
			exclusionZoneMaxY
		);
		this.partitioner = new LostSuppliesPartitioner();
		this.nearestNeighbor = new NearestNeighborOptimizer();
		this.twoOpt = new TwoOptImprover();
		this.preferWestStart = preferWestStart;
		this.exclusionZoneCenterX = (exclusionZoneMinX + exclusionZoneMaxX) / 2;
	}

	/**
	 * Route is circular around exclusion zone:
	 * Dropoff (west) → Lost Supplies → Pickup (north/east) → Lost Supplies → Dropoff (west)
	 *
	 * Number of laps is fixed (1-3 based on difficulty level from game)
	 *
	 * For non-final laps:
	 * - Before pickup (no rum): Only collect supplies on first side (West if preferWestStart, East otherwise)
	 * - After pickup (has rum): Only collect supplies on opposite side
	 *
	 * For final lap: Collect ALL remaining supplies regardless of position
	 *
	 * @param hasRum Whether we currently have rum (determines which side to collect from)
	 * @param currentLap Current lap number (0-indexed)
	 */
	public List<List<WorldPoint>> planMultipleLaps(
		WorldPoint startPosition,
		Set<GameObject> allLostSupplies,
		int numLaps,
		WorldPoint rumPickupLocation,
		WorldPoint rumReturnLocation,
		int maxSearchDistance,
		boolean hasRum,
		int currentLap)
	{
		if (numLaps <= 0)
		{
			return createEmptyLaps(numLaps);
		}

		if (allLostSupplies.isEmpty())
		{
			List<List<WorldPoint>> laps = new ArrayList<>();
			for (int i = 0; i < numLaps; i++)
			{
				List<WorldPoint> emptyRoute = new ArrayList<>();
				Map<WorldPoint, Map<WorldPoint, Double>> emptyMatrix = new HashMap<>();
				RouteWithCost result = insertPickupOptimally(emptyRoute, startPosition, rumPickupLocation, rumReturnLocation, emptyMatrix);
				laps.add(result.route);
			}
			return laps;
		}

		// Filter supplies based on current state (unless it's the final lap)
		boolean isFinalLap = (currentLap == numLaps - 1);
		Set<GameObject> filteredSupplies;

		if (isFinalLap)
		{
			// Final lap: collect ALL remaining supplies
			filteredSupplies = new HashSet<>(allLostSupplies);
			log.debug("Final lap - considering all {} supplies", filteredSupplies.size());
		}
		else
		{
			// Non-final lap: filter by side based on rum state and direction
			filteredSupplies = filterSuppliesBySide(allLostSupplies, hasRum);
			log.debug("Lap {}/{} (hasRum={}): filtered to {} supplies on {} side",
				currentLap + 1, numLaps, hasRum, filteredSupplies.size(),
				getSideToCollect(hasRum));
		}

		long startTime = System.currentTimeMillis();

		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix = matrixBuilder.buildMatrix(filteredSupplies);

		List<WorldPoint> supplyLocations = new ArrayList<>();
		for (GameObject supply : filteredSupplies)
		{
			supplyLocations.add(supply.getWorldLocation());
		}

		List<List<Set<WorldPoint>>> partitions = partitioner.generatePartitions(supplyLocations, numLaps);

		List<List<WorldPoint>> bestSolution = null;
		double bestTotalCost = Double.MAX_VALUE;

		for (List<Set<WorldPoint>> partition : partitions)
		{
			List<List<WorldPoint>> lapRoutes = new ArrayList<>();
			double totalCost = 0;

			for (Set<WorldPoint> lapSupplies : partition)
			{
				if (lapSupplies.isEmpty())
				{
					lapRoutes.add(new ArrayList<>());
					continue;
				}

				List<WorldPoint> supplyRoute = nearestNeighbor.findRoute(startPosition, lapSupplies, distanceMatrix, preferWestStart);
				supplyRoute = twoOpt.improve(supplyRoute, startPosition, distanceMatrix);

				RouteWithCost result = insertPickupOptimally(supplyRoute, startPosition, rumPickupLocation, rumReturnLocation, distanceMatrix);

				lapRoutes.add(result.route);
				totalCost += result.cost;
			}

			if (totalCost < bestTotalCost)
			{
				bestTotalCost = totalCost;
				bestSolution = lapRoutes;
			}
		}

		long endTime = System.currentTimeMillis();
		log.debug("Multi-lap optimization took {}ms, {} laps, {} supplies (filtered from {}), cost={}",
			endTime - startTime, numLaps, filteredSupplies.size(), allLostSupplies.size(), bestTotalCost);

		return bestSolution != null ? bestSolution : createEmptyLaps(numLaps);
	}

	/**
	 * Filters supplies to only include those on the appropriate side based on current state
	 */
	private Set<GameObject> filterSuppliesBySide(Set<GameObject> supplies, boolean hasRum)
	{
		Set<GameObject> filtered = new HashSet<>();
		boolean collectWest = shouldCollectWest(hasRum);

		for (GameObject supply : supplies)
		{
			WorldPoint location = supply.getWorldLocation();
			boolean isWest = location.getX() < exclusionZoneCenterX;

			if (isWest == collectWest)
			{
				filtered.add(supply);
			}
		}

		return filtered;
	}

	/**
	 * Determines if we should collect West supplies based on current state
	 */
	private boolean shouldCollectWest(boolean hasRum)
	{
		// Before pickup (no rum): collect first side
		// After pickup (has rum): collect opposite side
		if (!hasRum)
		{
			// Going TO pickup - collect first side
			return preferWestStart;
		}
		else
		{
			// Going TO dropoff - collect opposite side
			return !preferWestStart;
		}
	}

	/**
	 * Gets a string describing which side we're collecting from
	 */
	private String getSideToCollect(boolean hasRum)
	{
		return shouldCollectWest(hasRum) ? "West" : "East";
	}

	/**
	 * Route is circular: Dropoff → Lost Supplies → Pickup → Lost Supplies → Dropoff
	 */
	private RouteWithCost insertPickupOptimally(
		List<WorldPoint> supplyRoute,
		WorldPoint dropoffLocation,
		WorldPoint pickupLocation,
		WorldPoint returnLocation,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		if (pickupLocation == null || returnLocation == null || dropoffLocation == null)
		{
			return new RouteWithCost(new ArrayList<>(supplyRoute), 0.0);
		}

		if (supplyRoute.isEmpty())
		{
			List<WorldPoint> route = new ArrayList<>();
			route.add(pickupLocation);

			double cost = getDistance(dropoffLocation, pickupLocation, distanceMatrix);
			cost += getDistance(pickupLocation, returnLocation, distanceMatrix);

			return new RouteWithCost(route, cost);
		}

		double bestCost = Double.MAX_VALUE;
		int bestInsertPosition = 0;

		for (int insertPosition = 0; insertPosition <= supplyRoute.size(); insertPosition++)
		{
			double cost = 0;
			WorldPoint current = dropoffLocation;

			for (int i = 0; i < insertPosition; i++)
			{
				WorldPoint next = supplyRoute.get(i);
				cost += getDistance(current, next, distanceMatrix);
				current = next;
			}

			cost += getDistance(current, pickupLocation, distanceMatrix);
			current = pickupLocation;

			for (int i = insertPosition; i < supplyRoute.size(); i++)
			{
				WorldPoint next = supplyRoute.get(i);
				cost += getDistance(current, next, distanceMatrix);
				current = next;
			}

			cost += getDistance(current, returnLocation, distanceMatrix);

			if (cost < bestCost)
			{
				bestCost = cost;
				bestInsertPosition = insertPosition;
			}
		}

		List<WorldPoint> finalRoute = new ArrayList<>();
		for (int i = 0; i < bestInsertPosition; i++)
		{
			finalRoute.add(supplyRoute.get(i));
		}
		finalRoute.add(pickupLocation);
		for (int i = bestInsertPosition; i < supplyRoute.size(); i++)
		{
			finalRoute.add(supplyRoute.get(i));
		}

		return new RouteWithCost(finalRoute, bestCost);
	}

	private static class RouteWithCost
	{
		final List<WorldPoint> route;
		final double cost;

		RouteWithCost(List<WorldPoint> route, double cost)
		{
			this.route = route;
			this.cost = cost;
		}
	}

	private double getDistance(
		WorldPoint from,
		WorldPoint to,
		Map<WorldPoint, Map<WorldPoint, Double>> distanceMatrix)
	{
		if (from == null || to == null)
		{
			return 99999.0;
		}

		Map<WorldPoint, Double> fromDistances = distanceMatrix.get(from);

		if (fromDistances != null && fromDistances.containsKey(to))
		{
			return fromDistances.get(to);
		}

		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	private List<List<WorldPoint>> createEmptyLaps(int numLaps)
	{
		List<List<WorldPoint>> empty = new ArrayList<>();
		for (int i = 0; i < numLaps; i++)
		{
			empty.add(new ArrayList<>());
		}
		return empty;
	}
}
