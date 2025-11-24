package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BarracudaTileCostCalculator
{
	// Boat exclusion zone dimensions (small rectangle around objective boats)
	private static final int BOAT_EXCLUSION_WIDTH = 8;
	private static final int BOAT_EXCLUSION_HEIGHT = 3;

    private final Set<WorldPoint> knownRockLocations;
	private final Set<GameObject> visibleRocks;
	private final int exclusionZoneMinX;
	private final int exclusionZoneMaxX;
	private final int exclusionZoneMinY;
	private final int exclusionZoneMaxY;
	private final RouteOptimization routeOptimization;
	private final WorldPoint primaryObjectiveLocation;
	private final WorldPoint secondaryObjectiveLocation;

	private int speedBoostTilesRemaining = 0;
	private WorldPoint lastTile = null;
	private final Set<WorldPoint> consumedBoosts = new HashSet<>();

	// Precomputed spatial lookups for O(1) cost checks (huge performance boost)
	private final Set<WorldPoint> visibleRockLocations;
	private final Set<WorldPoint> veryCloseToRocks;
	private final Set<WorldPoint> closeToRocks;
	private final Set<WorldPoint> cloudDangerZones;
	private final Map<WorldPoint, WorldPoint> boostGrabbableTiles;

	public BarracudaTileCostCalculator(
		Set<WorldPoint> knownSpeedBoostLocations,
		Set<WorldPoint> knownRockLocations,
		Set<GameObject> visibleRocks,
		Set<NPC> lightningClouds,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY,
		WorldPoint primaryObjectiveLocation,
		WorldPoint secondaryObjectiveLocation,
		RouteOptimization routeOptimization)
	{
        this.knownRockLocations = knownRockLocations;
		this.visibleRocks = visibleRocks;
		this.exclusionZoneMinX = exclusionZoneMinX;
		this.exclusionZoneMaxX = exclusionZoneMaxX;
		this.exclusionZoneMinY = exclusionZoneMinY;
		this.exclusionZoneMaxY = exclusionZoneMaxY;
		this.primaryObjectiveLocation = primaryObjectiveLocation;
		this.secondaryObjectiveLocation = secondaryObjectiveLocation;
		this.routeOptimization = routeOptimization;

		this.visibleRockLocations = precomputeVisibleRockLocations(visibleRocks);
		this.veryCloseToRocks = precomputeRockProximity(1);
		this.closeToRocks = precomputeRockProximity(2);
		this.cloudDangerZones = precomputeCloudDangerZones(lightningClouds);
		this.boostGrabbableTiles = computeBoostGrabbableTiles(knownSpeedBoostLocations);
	}

	public double getTileCost(WorldPoint from, WorldPoint to)
	{
		if (lastTile == null || !lastTile.equals(from))
		{
			speedBoostTilesRemaining = 0;
		}
		lastTile = to;

		double cost = 1.0;

		WorldPoint nearbyBoost = findNearbyUnconsumedBoost(to);
		if (nearbyBoost != null)
		{
			cost = (routeOptimization == RouteOptimization.EFFICIENT) ? -8.0 : -5.0;
			speedBoostTilesRemaining = 15;
			consumedBoosts.add(nearbyBoost);
		}
		else if (speedBoostTilesRemaining > 0)
		{
			cost /= 2.0; // Double speed
			speedBoostTilesRemaining--;
		}

		if (isInBoatExclusionZone(to))
		{
			cost += 25; // Discouraged but allowed for pathmaking
		}

		if (knownRockLocations.contains(to) || visibleRockLocations.contains(to))
		{
			cost += 250;
			speedBoostTilesRemaining = 0;
		}
		else if (veryCloseToRocks.contains(to))
		{
			cost += 100;
			speedBoostTilesRemaining = 0;
		}
		else if (closeToRocks.contains(to))
		{
			cost += 50;
		}

		if (isInExclusionZone(to))
		{
			cost += 250;
		}
		else
		{
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
		}

		if (cloudDangerZones.contains(to))
		{
			cost += 200;
			speedBoostTilesRemaining = 0;
		}

		return cost;
	}

	private WorldPoint findNearbyUnconsumedBoost(WorldPoint tile)
	{
		WorldPoint boost = boostGrabbableTiles.get(tile);
		if (boost != null && !consumedBoosts.contains(boost))
		{
			return boost;
		}
		return null;
	}

	/**
	 * Computes all tiles where a boost can be grabbed, mapping each tile to its boost center.
	 * Boosts can be grabbed from a 3x3 area (Chebyshev distance <= 1) around their center.
	 */
	public static Map<WorldPoint, WorldPoint> computeBoostGrabbableTiles(Set<WorldPoint> boostLocations)
	{
		return computeGrabbableTiles(boostLocations, 1);
	}

	/**
	 * Computes all tiles within a given tolerance distance from target locations.
	 * Uses Chebyshev distance (max of dx, dy) for square areas.
	 *
	 * @param locations Center points
	 * @param tolerance Distance in tiles (1 = 3x3 area, 2 = 5x5 area, etc.)
	 * @return Map from grabbable tile to its center point
	 */
	public static Map<WorldPoint, WorldPoint> computeGrabbableTiles(Set<WorldPoint> locations, int tolerance)
	{
		Map<WorldPoint, WorldPoint> grabbableTiles = new HashMap<>();

		for (WorldPoint center : locations)
		{
			int plane = center.getPlane();

			for (int dx = -tolerance; dx <= tolerance; dx++)
			{
				for (int dy = -tolerance; dy <= tolerance; dy++)
				{
					WorldPoint tile = new WorldPoint(center.getX() + dx, center.getY() + dy, plane);
					grabbableTiles.put(tile, center);
				}
			}
		}

		return grabbableTiles;
	}

	private boolean isInExclusionZone(WorldPoint point)
	{
		return point.getX() >= exclusionZoneMinX
			&& point.getX() <= exclusionZoneMaxX
			&& point.getY() >= exclusionZoneMinY
			&& point.getY() <= exclusionZoneMaxY;
	}

	private boolean isInBoatExclusionZone(WorldPoint point)
	{
		if (primaryObjectiveLocation == null && secondaryObjectiveLocation == null)
		{
			return false;
		}

		boolean inPrimaryZone = primaryObjectiveLocation != null
			&& isInRectangularZone(point, primaryObjectiveLocation, BOAT_EXCLUSION_WIDTH, BOAT_EXCLUSION_HEIGHT);

		boolean inSecondaryZone = secondaryObjectiveLocation != null
			&& isInRectangularZone(point, secondaryObjectiveLocation, BOAT_EXCLUSION_WIDTH, BOAT_EXCLUSION_HEIGHT);

		return inPrimaryZone || inSecondaryZone;
	}

	private static boolean isInRectangularZone(WorldPoint point, WorldPoint center, int width, int height)
	{
		int halfWidth = width / 2;
		int halfHeight = height / 2;

		int minX = center.getX() - halfWidth;
		int maxX = center.getX() + halfWidth;
		int minY = center.getY() - halfHeight;
		int maxY = center.getY() + halfHeight;

		return point.getX() >= minX && point.getX() <= maxX
			&& point.getY() >= minY && point.getY() <= maxY;
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
