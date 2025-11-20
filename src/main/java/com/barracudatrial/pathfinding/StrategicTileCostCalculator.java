package com.barracudatrial.pathfinding;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.Set;

/**
 * Calculates tile costs for strategic pathfinding (lost supplies order planning)
 * Considers rocks and speed boosts, but NOT clouds (since they move)
 */
public class StrategicTileCostCalculator implements TileCostCalculator
{
	private final Set<WorldPoint> knownSpeedBoostLocations;
	private final Set<WorldPoint> knownRockLocations;
	private final Set<GameObject> visibleRocks;
	private final int exclusionZoneMinX;
	private final int exclusionZoneMaxX;
	private final int exclusionZoneMinY;
	private final int exclusionZoneMaxY;

	// Speed boost gives 3x speed for 8 tiles after pickup
	private int speedBoostTilesRemaining = 0;
	private WorldPoint lastTile = null;

	// Precomputed proximity sets for O(1) lookups instead of expensive loops
	private final Set<WorldPoint> visibleRockLocations;
	private final Set<WorldPoint> veryCloseToRocks;
	private final Set<WorldPoint> closeToRocks;

	public StrategicTileCostCalculator(
		Set<WorldPoint> knownSpeedBoostLocations,
		Set<WorldPoint> knownRockLocations,
		Set<GameObject> visibleRocks,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY)
	{
		this.knownSpeedBoostLocations = knownSpeedBoostLocations;
		this.knownRockLocations = knownRockLocations;
		this.visibleRocks = visibleRocks;
		this.exclusionZoneMinX = exclusionZoneMinX;
		this.exclusionZoneMaxX = exclusionZoneMaxX;
		this.exclusionZoneMinY = exclusionZoneMinY;
		this.exclusionZoneMaxY = exclusionZoneMaxY;

		this.visibleRockLocations = precomputeVisibleRockLocations(visibleRocks);
		this.veryCloseToRocks = precomputeRockProximity(1);
		this.closeToRocks = precomputeRockProximity(2);
	}

	@Override
	public double getTileCost(WorldPoint from, WorldPoint to)
	{
		if (lastTile == null || !lastTile.equals(from))
		{
			speedBoostTilesRemaining = 0;
		}
		lastTile = to;

		double cost = 1.0;

		// Exclusion zone is always impassable
		if (isInExclusionZone(to))
		{
			return 999999;
		}

		// Rocks are impassable obstacles
		if (knownRockLocations.contains(to) || visibleRockLocations.contains(to))
		{
			return 999999;
		}

		// Heavy penalties for proximity to rocks to avoid collisions
		if (veryCloseToRocks.contains(to))
		{
			cost += 10000;
		}
		else if (closeToRocks.contains(to))
		{
			cost += 500;
		}

		// Treat exclusion zone proximity like rocks
		double distToZone = distanceToExclusionZone(to);
		if (distToZone <= 1)
		{
			cost += 10000;
		}
		else if (distToZone <= 2)
		{
			cost += 500;
		}
		else if (distToZone <= 3)
		{
			cost += 50;
		}

		if (knownSpeedBoostLocations.contains(to))
		{
			// Negative cost incentivizes routing through speed boosts
			// Value calculated from 3x speed for 8 tiles = ~5.33 savings
			cost = -5.0;
			speedBoostTilesRemaining = 8;
		}
		else if (speedBoostTilesRemaining > 0)
		{
			cost /= 3.0; // 3x speed from game mechanics
			speedBoostTilesRemaining--;
		}

		return cost;
	}

	public void resetSpeedBoostState()
	{
		speedBoostTilesRemaining = 0;
		lastTile = null;
	}

	private boolean isInExclusionZone(WorldPoint point)
	{
		return point.getX() >= exclusionZoneMinX
			&& point.getX() <= exclusionZoneMaxX
			&& point.getY() >= exclusionZoneMinY
			&& point.getY() <= exclusionZoneMaxY;
	}

	private double distanceToExclusionZone(WorldPoint point)
	{
		if (isInExclusionZone(point))
		{
			return 0;
		}

		int x = point.getX();
		int y = point.getY();

		int dx = Math.max(0, Math.max(exclusionZoneMinX - x, x - exclusionZoneMaxX));
		int dy = Math.max(0, Math.max(exclusionZoneMinY - y, y - exclusionZoneMaxY));

		return Math.sqrt(dx * dx + dy * dy);
	}

	private Set<WorldPoint> precomputeVisibleRockLocations(Set<GameObject> visibleRocks)
	{
		Set<WorldPoint> locations = new HashSet<>();
		for (GameObject rock : visibleRocks)
		{
			locations.add(rock.getWorldLocation());
		}
		return locations;
	}

	private Set<WorldPoint> precomputeRockProximity(int maxDistance)
	{
		Set<WorldPoint> proximityTiles = new HashSet<>();

		Set<WorldPoint> allRockLocations = new HashSet<>(knownRockLocations);
		for (GameObject rock : visibleRocks)
		{
			allRockLocations.add(rock.getWorldLocation());
		}

		for (WorldPoint rock : allRockLocations)
		{
			int plane = rock.getPlane();
			for (int dx = -maxDistance; dx <= maxDistance; dx++)
			{
				for (int dy = -maxDistance; dy <= maxDistance; dy++)
				{
					WorldPoint tile = new WorldPoint(rock.getX() + dx, rock.getY() + dy, plane);
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (dist <= maxDistance && dist > 0)
					{
						proximityTiles.add(tile);
					}
				}
			}
		}

		return proximityTiles;
	}
}
