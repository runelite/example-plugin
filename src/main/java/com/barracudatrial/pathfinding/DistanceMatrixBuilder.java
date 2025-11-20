package com.barracudatrial.pathfinding;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * Builds distance matrix for lost supplies routing optimization
 * Uses straight-line distance with obstacle penalties (fast, ~1ms for 30 lost supplies)
 */
public class DistanceMatrixBuilder
{
	private final Set<WorldPoint> rockLocations;
	private final int exclusionZoneMinX;
	private final int exclusionZoneMaxX;
	private final int exclusionZoneMinY;
	private final int exclusionZoneMaxY;

	public DistanceMatrixBuilder(
		Set<WorldPoint> rockLocations,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY)
	{
		this.rockLocations = rockLocations;
		this.exclusionZoneMinX = exclusionZoneMinX;
		this.exclusionZoneMaxX = exclusionZoneMaxX;
		this.exclusionZoneMinY = exclusionZoneMinY;
		this.exclusionZoneMaxY = exclusionZoneMaxY;
	}

	/**
	 * Builds distance matrix for all lost supplies pairs
	 * @return Map from lost supplies WorldPoint to Map of distances to other lost supplies
	 */
	public Map<WorldPoint, Map<WorldPoint, Double>> buildMatrix(Set<GameObject> lostSupplies)
	{
		Map<WorldPoint, Map<WorldPoint, Double>> matrix = new HashMap<>();

		List<WorldPoint> suppliesLocations = new ArrayList<>();
		for (GameObject supply : lostSupplies)
		{
			suppliesLocations.add(supply.getWorldLocation());
		}

		for (WorldPoint from : suppliesLocations)
		{
			Map<WorldPoint, Double> distances = new HashMap<>();

			for (WorldPoint to : suppliesLocations)
			{
				if (from.equals(to))
				{
					distances.put(to, 0.0);
				}
				else
				{
					double dist = calculateDistance(from, to);
					distances.put(to, dist);
				}
			}

			matrix.put(from, distances);
		}

		return matrix;
	}

	private double calculateDistance(WorldPoint from, WorldPoint to)
	{
		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double distance = Math.sqrt(dx * dx + dy * dy);

		// Treat exclusion zone like rocks for path planning
		if (lineIntersectsExclusionZone(from, to))
		{
			distance *= 3.0; // Heavier than rocks - exclusion zone always deadly
		}
		else if (lineNearExclusionZone(from, to))
		{
			distance *= 1.5;
		}

		if (lineIntersectsRock(from, to))
		{
			distance *= 2.0;
		}
		else if (lineNearRock(from, to))
		{
			distance *= 1.3;
		}

		return distance;
	}

	private boolean lineIntersectsRock(WorldPoint from, WorldPoint to)
	{
		int numSamples = (int) Math.ceil(from.distanceTo(to));

		for (int i = 0; i <= numSamples; i++)
		{
			double t = (double) i / numSamples;
			int x = (int) (from.getX() + t * (to.getX() - from.getX()));
			int y = (int) (from.getY() + t * (to.getY() - from.getY()));
			WorldPoint point = new WorldPoint(x, y, from.getPlane());

			if (rockLocations.contains(point))
			{
				return true;
			}
		}

		return false;
	}

	private boolean lineNearRock(WorldPoint from, WorldPoint to)
	{
		int numSamples = (int) Math.ceil(from.distanceTo(to));

		for (int i = 0; i <= numSamples; i++)
		{
			double t = (double) i / numSamples;
			int x = (int) (from.getX() + t * (to.getX() - from.getX()));
			int y = (int) (from.getY() + t * (to.getY() - from.getY()));
			WorldPoint point = new WorldPoint(x, y, from.getPlane());

			for (WorldPoint rock : rockLocations)
			{
				if (point.distanceTo(rock) <= 2.0)
				{
					return true;
				}
			}
		}

		return false;
	}

	private boolean lineIntersectsExclusionZone(WorldPoint from, WorldPoint to)
	{
		int numSamples = (int) Math.ceil(from.distanceTo(to));

		for (int i = 0; i <= numSamples; i++)
		{
			double t = (double) i / numSamples;
			int x = (int) (from.getX() + t * (to.getX() - from.getX()));
			int y = (int) (from.getY() + t * (to.getY() - from.getY()));

			if (x >= exclusionZoneMinX && x <= exclusionZoneMaxX
				&& y >= exclusionZoneMinY && y <= exclusionZoneMaxY)
			{
				return true;
			}
		}

		return false;
	}

	private boolean lineNearExclusionZone(WorldPoint from, WorldPoint to)
	{
		int numSamples = (int) Math.ceil(from.distanceTo(to));

		for (int i = 0; i <= numSamples; i++)
		{
			double t = (double) i / numSamples;
			int x = (int) (from.getX() + t * (to.getX() - from.getX()));
			int y = (int) (from.getY() + t * (to.getY() - from.getY()));

			int dx = Math.max(0, Math.max(exclusionZoneMinX - x, x - exclusionZoneMaxX));
			int dy = Math.max(0, Math.max(exclusionZoneMinY - y, y - exclusionZoneMaxY));
			double distToZone = Math.sqrt(dx * dx + dy * dy);

			if (distToZone <= 3.0)
			{
				return true;
			}
		}

		return false;
	}
}
