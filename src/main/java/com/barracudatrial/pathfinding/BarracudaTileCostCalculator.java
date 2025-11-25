package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BarracudaTileCostCalculator
{
	private final int exclusionZoneMinX;
	private final int exclusionZoneMaxX;
	private final int exclusionZoneMinY;
	private final int exclusionZoneMaxY;
	private final RouteOptimization routeOptimization;
	private final WorldPoint primaryObjectiveLocation;
	private final WorldPoint secondaryObjectiveLocation;
	private final int boatExclusionWidth;
	private final int boatExclusionHeight;

	private int speedBoostTilesRemaining = 0;
	private WorldPoint lastTile = null;
	private final Set<WorldPoint> consumedBoosts = new HashSet<>();

	// Precomputed spatial lookups for O(1) cost checks
	private final Set<WorldPoint> rockLocations;
	private final Set<WorldPoint> closeToRocks;
	private final Set<WorldPoint> cloudDangerZones;
	private final Map<WorldPoint, List<WorldPoint>> boostGrabbableTiles;
	private final Set<WorldPoint> fetidPoolLocations;
	private final Set<WorldPoint> toadPillarLocations;
	private Set<WorldPoint> closeToFetidPoolsAndToadPillars = new HashSet<>();

	public BarracudaTileCostCalculator(
		Map<WorldPoint, List<WorldPoint>> knownSpeedBoostLocations,
		Set<WorldPoint> knownRockLocations,
		Set<WorldPoint> knownFetidPoolLocations,
		Set<WorldPoint> knownToadPillarLocations,
		Set<NPC> lightningClouds,
		int exclusionZoneMinX,
		int exclusionZoneMaxX,
		int exclusionZoneMinY,
		int exclusionZoneMaxY,
		WorldPoint primaryObjectiveLocation,
		WorldPoint secondaryObjectiveLocation,
		RouteOptimization routeOptimization,
		int boatExclusionWidth,
		int boatExclusionHeight)
	{
		this.exclusionZoneMinX = exclusionZoneMinX;
		this.exclusionZoneMaxX = exclusionZoneMaxX;
		this.exclusionZoneMinY = exclusionZoneMinY;
		this.exclusionZoneMaxY = exclusionZoneMaxY;
		this.primaryObjectiveLocation = primaryObjectiveLocation;
		this.secondaryObjectiveLocation = secondaryObjectiveLocation;
		this.routeOptimization = routeOptimization;
		this.boatExclusionWidth = boatExclusionWidth;
		this.boatExclusionHeight = boatExclusionHeight;

		this.rockLocations = knownRockLocations;
		this.closeToRocks = precomputeTileProximity(rockLocations, 1);
		this.cloudDangerZones = precomputeCloudDangerZones(lightningClouds);
		this.boostGrabbableTiles = knownSpeedBoostLocations;
		this.fetidPoolLocations = knownFetidPoolLocations;
		this.toadPillarLocations = knownToadPillarLocations;
		this.closeToFetidPoolsAndToadPillars = precomputeTileProximity(fetidPoolLocations, 1);
		this.closeToFetidPoolsAndToadPillars.addAll(precomputeTileProximity(toadPillarLocations, 1));
	}

	public double getTileCost(WorldPoint from, WorldPoint to)
	{
		int maxTileCost = 100000;

		if (lastTile == null || !lastTile.equals(from))
		{
			speedBoostTilesRemaining = 0;
		}
		lastTile = to;

		double cost = 1.0;

		WorldPoint unconsumedBoost = getUnconsumedBoost(to);
		if (unconsumedBoost != null)
		{
			cost = (routeOptimization == RouteOptimization.EFFICIENT) ? -6.0 : -4.0;
			speedBoostTilesRemaining = 15;
			consumedBoosts.add(unconsumedBoost);
		}
		else if (speedBoostTilesRemaining > 0)
		{
			cost /= 2.0; // Double speed
			speedBoostTilesRemaining--;
		}

		if (isInBoatExclusionZone(to))
		{
			cost += 100; // Discouraged but allowed for pathmaking
		}
		else if (rockLocations.contains(to))
		{
			cost = maxTileCost;
		}
		else if (closeToRocks.contains(to))
		{
			cost += 3;
		}
		else if (isInExclusionZone(to))
		{
			cost = maxTileCost;
		}
		else if (cloudDangerZones.contains(to))
		{
			cost += 200;
			speedBoostTilesRemaining = 0;
		}
		else if (fetidPoolLocations.contains(to))
		{
			cost += 100;
		}
		else if (toadPillarLocations.contains(to))
		{
			cost = maxTileCost;
		}
		else if (closeToFetidPoolsAndToadPillars.contains(to))
		{
			cost += 3;
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

		return cost;
	}

	private WorldPoint getUnconsumedBoost(WorldPoint tile)
	{
		List<WorldPoint> boosts = boostGrabbableTiles.get(tile);
		if (boosts != null && boosts.contains(tile) && !consumedBoosts.contains(tile))
		{
			return tile;
		}
		return null;
	}

	public boolean isInDangerZone(WorldPoint tile)
	{
		return cloudDangerZones.contains(tile)
			|| rockLocations.contains(tile)
			|| fetidPoolLocations.contains(tile);
	}

	/**
	 * Get a snapshot of all current danger zones for path stability tracking
	 */
	public Set<WorldPoint> getDangerZoneSnapshot()
	{
		Set<WorldPoint> snapshot = new HashSet<>();
		snapshot.addAll(cloudDangerZones);
		snapshot.addAll(rockLocations);
		snapshot.addAll(fetidPoolLocations);
		return snapshot;
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
			&& isInRectangularZone(point, primaryObjectiveLocation, boatExclusionWidth, boatExclusionHeight);

		boolean inSecondaryZone = secondaryObjectiveLocation != null
			&& isInRectangularZone(point, secondaryObjectiveLocation, boatExclusionWidth, boatExclusionHeight);

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

	private Set<WorldPoint> precomputeTileProximity(Set<WorldPoint> locations, int maxDistance)
	{
		Set<WorldPoint> proximityTiles = new HashSet<>();
		int maxDistSq = maxDistance * maxDistance;

		for (WorldPoint location : locations)
		{
			int baseX = location.getX();
			int baseY = location.getY();
			int plane = location.getPlane();

			for (int dx = -maxDistance; dx <= maxDistance; dx++)
			{
				int dxSq = dx * dx;

				for (int dy = -maxDistance; dy <= maxDistance; dy++)
				{
					if (dx == 0 && dy == 0)
					{
						continue; // skip the location tile itself
					}

					int distSq = dxSq + dy * dy;
					if (distSq > maxDistSq)
					{
						continue;
					}

					WorldPoint tile = new WorldPoint(baseX + dx, baseY + dy, plane);

					// Don't consider tiles that are themselves location tiles
					if (!locations.contains(tile))
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
