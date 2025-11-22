package com.barracudatrial.pathfinding;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.Set;

public class BarracudaTileCostCalculator
{
	private final Set<WorldPoint> knownSpeedBoostLocations;
	private final Set<WorldPoint> knownRockLocations;
	private final Set<GameObject> visibleRocks;
	private final Set<NPC> lightningClouds;
	private final int exclusionZoneMinX;
	private final int exclusionZoneMaxX;
	private final int exclusionZoneMinY;
	private final int exclusionZoneMaxY;

	private int speedBoostTilesRemaining = 0;
	private WorldPoint lastTile = null;

	// Precomputed spatial lookups for O(1) cost checks (huge performance boost)
	private final Set<WorldPoint> visibleRockLocations;
	private final Set<WorldPoint> veryCloseToRocks;
	private final Set<WorldPoint> closeToRocks;
	private final Set<WorldPoint> cloudDangerZones;

	public BarracudaTileCostCalculator(
		Set<WorldPoint> knownSpeedBoostLocations,
		Set<WorldPoint> knownRockLocations,
		Set<GameObject> visibleRocks,
		Set<NPC> lightningClouds,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY)
	{
		this.knownSpeedBoostLocations = knownSpeedBoostLocations;
		this.knownRockLocations = knownRockLocations;
		this.visibleRocks = visibleRocks;
		this.lightningClouds = lightningClouds;
		this.exclusionZoneMinX = exclusionZoneMinX;
		this.exclusionZoneMaxX = exclusionZoneMaxX;
		this.exclusionZoneMinY = exclusionZoneMinY;
		this.exclusionZoneMaxY = exclusionZoneMaxY;

		this.visibleRockLocations = precomputeVisibleRockLocations(visibleRocks);
		this.veryCloseToRocks = precomputeRockProximity(1);
		this.closeToRocks = precomputeRockProximity(2);
		this.cloudDangerZones = precomputeCloudDangerZones(lightningClouds);
	}

	public double getTileCost(WorldPoint from, WorldPoint to)
	{
		if (lastTile == null || !lastTile.equals(from))
		{
			speedBoostTilesRemaining = 0;
		}
		lastTile = to;

		double cost = 1.0;

		if (isInExclusionZone(to))
		{
			return 999999;
		}

		if (knownRockLocations.contains(to) || visibleRockLocations.contains(to))
		{
			return 999999;
		}

		if (knownSpeedBoostLocations.contains(to))
		{
			cost = -10.0;
			speedBoostTilesRemaining = 5;
		}
		else if (speedBoostTilesRemaining > 0)
		{
			cost /= 3.0;
			speedBoostTilesRemaining--;
		}

		if (veryCloseToRocks.contains(to))
		{
			cost += 100;
		}
		else if (closeToRocks.contains(to))
		{
			cost += 25;
		}

		double distToZone = distanceToExclusionZone(to);
		if (distToZone <= 1)
		{
			cost += 100;
		}
		else if (distToZone <= 2)
		{
			cost += 50;
		}
		else if (distToZone <= 3)
		{
			cost += 25;
		}

		// Use single penalty instead of distance-based since precompute would need 3 separate sets for each distance tier (memory tradeoff)
		if (cloudDangerZones.contains(to))
		{
			cost += 200;
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

		// Check both known and visible rocks
		Set<WorldPoint> allRockLocations = new HashSet<>(knownRockLocations);
		for (GameObject rock : visibleRocks)
		{
			allRockLocations.add(rock.getWorldLocation());
		}

		// For each rock, add all tiles within maxDistance
		for (WorldPoint rock : allRockLocations)
		{
			int plane = rock.getPlane();
			for (int dx = -maxDistance; dx <= maxDistance; dx++)
			{
				for (int dy = -maxDistance; dy <= maxDistance; dy++)
				{
					WorldPoint tile = new WorldPoint(rock.getX() + dx, rock.getY() + dy, plane);
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (dist <= maxDistance && dist > 0) // Exclude rock tile itself
					{
						proximityTiles.add(tile);
					}
				}
			}
		}

		return proximityTiles;
	}

	/**
	 * Precomputes all tiles within cloud danger zones for O(1) lookup
	 */
	private Set<WorldPoint> precomputeCloudDangerZones(Set<NPC> lightningClouds)
	{
		Set<WorldPoint> dangerZones = new HashSet<>();

		for (NPC cloud : lightningClouds)
		{
			WorldPoint cloudLoc = cloud.getWorldLocation();
			int plane = cloudLoc.getPlane();

			// Add all tiles within distance 3 of cloud
			for (int dx = -3; dx <= 3; dx++)
			{
				for (int dy = -3; dy <= 3; dy++)
				{
					WorldPoint tile = new WorldPoint(cloudLoc.getX() + dx, cloudLoc.getY() + dy, plane);
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (dist <= 3)
					{
						dangerZones.add(tile);
					}
				}
			}
		}

		return dangerZones;
	}
}
