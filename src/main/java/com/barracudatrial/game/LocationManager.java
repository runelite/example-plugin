package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

/**
 * Manages rum locations and exclusion zone calculations for Barracuda Trial
 */
@Slf4j
public class LocationManager implements PathPlanner.LocationHelper
{
	private final Client client;
	private final State state;
	private final SceneScanner sceneScanner;

	// Rum object IDs
	private static final int RUM_RETURN_BASE_OBJECT_ID = 59237;
	private static final int RUM_RETURN_IMPOSTOR_ID = 59239;
	private static final int RUM_PICKUP_BASE_OBJECT_ID = 59240;
	private static final int RUM_PICKUP_IMPOSTOR_ID = 59242;

	// Location offsets (constant across all trials)
	private static final int PICKUP_OFFSET_X = 24;
	private static final int PICKUP_OFFSET_Y = -128;

	// Exclusion zone offsets (the center island area we circle around)
	private static final int EXCLUSION_MIN_X_OFFSET = 47;
	private static final int EXCLUSION_MAX_X_OFFSET = 102;
	private static final int EXCLUSION_MIN_Y_OFFSET = -51;
	private static final int EXCLUSION_MAX_Y_OFFSET = 12;

	// Pathfinding offsets (actual pickup/dropoff are impassable)
	private static final int PATHFINDING_PICKUP_OFFSET_Y = 2;
	private static final int PATHFINDING_DROPOFF_OFFSET_Y = -2;

	public LocationManager(Client client, State state, SceneScanner sceneScanner)
	{
		this.client = client;
		this.state = state;
		this.sceneScanner = sceneScanner;
	}

	/**
	 * Updates rum locations by searching WorldEntity scenes
	 * Should be called every game tick while in trial area
	 */
	public void updateRumObjects()
	{
		if (!state.isInTrialArea())
		{
			return;
		}

		updateRumLocationsBySearch();
	}

	/**
	 * Searches for both rum location objects (return and pickup) in WorldEntity scenes
	 * Updates both locations when found
	 */
	private void updateRumLocationsBySearch()
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		// Rum objects are on WorldEntity boats, so only check those
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || localPlayer.getWorldView() == null)
		{
			return;
		}

		for (WorldEntity worldEntity : topLevelWorldView.worldEntities())
		{
			if (worldEntity == null)
			{
				continue;
			}

			WorldView entityWorldView = worldEntity.getWorldView();
			if (entityWorldView == null)
			{
				continue;
			}

			Scene entityScene = entityWorldView.getScene();
			if (entityScene == null)
			{
				continue;
			}

			// Scan this WorldEntity scene for rum locations
			scanSceneForRumLocations(entityScene);
		}
	}

	/**
	 * Scans a scene for rum location objects (base ID 59237 or 59240)
	 * Updates rumReturnLocation and rumPickupLocation when found
	 */
	private void scanSceneForRumLocations(Scene scene)
	{
		Tile[][][] tiles = scene.getTiles();
		if (tiles == null)
		{
			return;
		}

		for (int plane = 0; plane < tiles.length; plane++)
		{
			if (tiles[plane] == null)
			{
				continue;
			}

			for (int x = 0; x < tiles[plane].length; x++)
			{
				if (tiles[plane][x] == null)
				{
					continue;
				}

				for (int y = 0; y < tiles[plane][x].length; y++)
				{
					Tile tile = tiles[plane][x][y];
					if (tile == null)
					{
						continue;
					}

					for (GameObject gameObject : tile.getGameObjects())
					{
						if (gameObject == null)
						{
							continue;
						}

						int objectId = gameObject.getId();
						boolean isReturn = false;
						boolean isPickup = false;

						// Check base IDs
						if (objectId == RUM_RETURN_BASE_OBJECT_ID)
						{
							isReturn = true;
						}
						else if (objectId == RUM_PICKUP_BASE_OBJECT_ID)
						{
							isPickup = true;
						}

						// Check impostor IDs
						if (!isReturn && !isPickup)
						{
							try
							{
								ObjectComposition comp = client.getObjectDefinition(objectId);
								if (comp != null)
								{
									ObjectComposition impostor = comp.getImpostor();
									if (impostor != null)
									{
										int impostorId = impostor.getId();
										if (impostorId == RUM_RETURN_IMPOSTOR_ID)
										{
											isReturn = true;
										}
										else if (impostorId == RUM_PICKUP_IMPOSTOR_ID)
										{
											isPickup = true;
										}
									}
								}
							}
							catch (Exception e)
							{
								// Ignore impostor check failures
							}
						}

						// Update locations when found
						if (isReturn)
						{
							WorldPoint newLocation = gameObject.getWorldLocation();
							if (state.getRumReturnLocation() == null || !state.getRumReturnLocation().equals(newLocation))
							{
								state.setRumReturnLocation(newLocation);
								log.info("Found rum return location: {}", newLocation);
								// Calculate exclusion zone from return location
								calculateExclusionZone(newLocation);
							}
						}
						else if (isPickup)
						{
							WorldPoint newLocation = gameObject.getWorldLocation();
							if (state.getRumPickupLocation() == null || !state.getRumPickupLocation().equals(newLocation))
							{
								state.setRumPickupLocation(newLocation);
								log.info("Found rum pickup location: {}", newLocation);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Initializes trial locations by searching for rum objects
	 * Called when entering trial area
	 */
	public void initializeTrialLocations()
	{
		// Scan the scene for rum return object (ID 59239)
		// Everything else is calculated relative to this anchor point
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			log.debug("Cannot initialize trial locations - no world view");
			return;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			log.debug("Cannot initialize trial locations - no scene");
			return;
		}

		// Scan for rum locations (return base 59237/impostor 59239, pickup base 59240/impostor 59242)
		updateRumLocationsBySearch();
	}

	/**
	 * Calculates exclusion zone boundaries from rum return location
	 * The exclusion zone is the center island area we circle around
	 */
	private void calculateExclusionZone(WorldPoint rumReturnLoc)
	{
		// Calculate exclusion zone boundaries relative to return location
		int exclusionZoneMinX = rumReturnLoc.getX() + EXCLUSION_MIN_X_OFFSET;
		int exclusionZoneMaxX = rumReturnLoc.getX() + EXCLUSION_MAX_X_OFFSET;
		int exclusionZoneMinY = rumReturnLoc.getY() + EXCLUSION_MIN_Y_OFFSET;
		int exclusionZoneMaxY = rumReturnLoc.getY() + EXCLUSION_MAX_Y_OFFSET;

		state.setExclusionZoneMinX(exclusionZoneMinX);
		state.setExclusionZoneMaxX(exclusionZoneMaxX);
		state.setExclusionZoneMinY(exclusionZoneMinY);
		state.setExclusionZoneMaxY(exclusionZoneMaxY);

		log.debug("Exclusion zone: ({}, {}) to ({}, {})",
			exclusionZoneMinX, exclusionZoneMinY, exclusionZoneMaxX, exclusionZoneMaxY);
	}

	/**
	 * Checks if a point is inside the center exclusion zone
	 * We want to path AROUND this area, never through it
	 */
	public boolean isInExclusionZone(WorldPoint point)
	{
		return point.getX() >= state.getExclusionZoneMinX()
			&& point.getX() <= state.getExclusionZoneMaxX()
			&& point.getY() >= state.getExclusionZoneMinY()
			&& point.getY() <= state.getExclusionZoneMaxY();
	}

	/**
	 * Calculates the distance from a point to the nearest edge of the exclusion zone
	 * Returns 0 if the point is inside the zone
	 */
	public double getDistanceToExclusionZone(WorldPoint point)
	{
		int x = point.getX();
		int y = point.getY();

		// If inside the zone, return 0
		if (isInExclusionZone(point))
		{
			return 0;
		}

		// Calculate distance to nearest edge
		double distanceX;
		if (x < state.getExclusionZoneMinX())
		{
			distanceX = state.getExclusionZoneMinX() - x;
		}
		else if (x > state.getExclusionZoneMaxX())
		{
			distanceX = x - state.getExclusionZoneMaxX();
		}
		else
		{
			distanceX = 0; // X is within the zone's X range
		}

		double distanceY;
		if (y < state.getExclusionZoneMinY())
		{
			distanceY = state.getExclusionZoneMinY() - y;
		}
		else if (y > state.getExclusionZoneMaxY())
		{
			distanceY = y - state.getExclusionZoneMaxY();
		}
		else
		{
			distanceY = 0; // Y is within the zone's Y range
		}

		// Return the minimum distance (edge distance, not diagonal)
		if (distanceX == 0)
		{
			return distanceY;
		}
		else if (distanceY == 0)
		{
			return distanceX;
		}
		else
		{
			// Point is diagonal to the zone - return Euclidean distance
			return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
		}
	}

	/**
	 * Gets the pathfinding-safe pickup location
	 * The actual pickup is impassable, so we target 2 tiles north
	 */
	public WorldPoint getPathfindingPickupLocation()
	{
		if (state.getRumPickupLocation() == null)
		{
			return null;
		}
		return new WorldPoint(
			state.getRumPickupLocation().getX(),
			state.getRumPickupLocation().getY() + PATHFINDING_PICKUP_OFFSET_Y,
			state.getRumPickupLocation().getPlane()
		);
	}

	/**
	 * Gets the pathfinding-safe dropoff location
	 * The actual dropoff is impassable, so we target 2 tiles south
	 */
	public WorldPoint getPathfindingDropoffLocation()
	{
		if (state.getRumReturnLocation() == null)
		{
			return null;
		}
		return new WorldPoint(
			state.getRumReturnLocation().getX(),
			state.getRumReturnLocation().getY() + PATHFINDING_DROPOFF_OFFSET_Y,
			state.getRumReturnLocation().getPlane()
		);
	}
}
