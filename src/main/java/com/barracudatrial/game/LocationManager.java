package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * Manages rum locations and exclusion zone calculations for Barracuda Trial
 */
@Slf4j
public class LocationManager
{
	private final Client client;
	private final State state;

	private static final int RUM_RETURN_BASE_OBJECT_ID = 59237; // No constant available
	private static final int RUM_RETURN_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD;
	private static final int RUM_PICKUP_BASE_OBJECT_ID = 59240; // No constant available
	private static final int RUM_PICKUP_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD;

	private static final int EXCLUSION_MIN_X_OFFSET = -26;
	private static final int EXCLUSION_MAX_X_OFFSET = 22;
	private static final int EXCLUSION_MIN_Y_OFFSET = -106;
	private static final int EXCLUSION_MAX_Y_OFFSET = -53;

	public LocationManager(Client client, State state)
	{
		this.client = client;
		this.state = state;
	}

	/**
	 * Updates rum locations by searching WorldEntity scenes
	 * Should be called every game tick while in trial area
	 */
	public void updateRumLocations()
	{
		if (!state.isInTrialArea())
		{
			return;
		}

		searchForRumLocationsInWorldEntities();
	}

	private void searchForRumLocationsInWorldEntities()
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || localPlayer.getWorldView() == null)
		{
			return;
		}

		// Scan all WorldEntities to find rum boats (they're on separate boats from player)
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

			// Pass the WorldEntity so we can get its real world location
			scanSceneForRumReturnAndPickupLocations(entityScene, worldEntity);
		}
	}

	private void scanSceneForRumReturnAndPickupLocations(Scene scene, WorldEntity worldEntity)
	{
		Tile[][][] tileArray = scene.getTiles();
		if (tileArray == null)
		{
			return;
		}

		// Get the boat's real world location once (it's the same for all objects on this boat)
		WorldPoint boatWorldLocation = null;
		try
		{
			var boatLocalLocation = worldEntity.getLocalLocation();
			if (boatLocalLocation != null)
			{
				boatWorldLocation = WorldPoint.fromLocalInstance(client, boatLocalLocation);
			}
		}
		catch (Exception e)
		{
			log.debug("Error getting boat world location: {}", e.getMessage());
		}

		if (boatWorldLocation == null)
		{
			return; // Can't determine boat location, skip this entity
		}

		// Scan all planes in this scene (WorldEntity scenes need all planes scanned)
		for (int planeIndex = 0; planeIndex < tileArray.length; planeIndex++)
		{
			if (tileArray[planeIndex] == null)
			{
				continue;
			}

			for (int xIndex = 0; xIndex < tileArray[planeIndex].length; xIndex++)
			{
				if (tileArray[planeIndex][xIndex] == null)
				{
					continue;
				}

				for (int yIndex = 0; yIndex < tileArray[planeIndex][xIndex].length; yIndex++)
				{
					Tile tile = tileArray[planeIndex][xIndex][yIndex];
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
						boolean isRumReturnObject = false;
						boolean isRumPickupObject = false;

						if (objectId == RUM_RETURN_BASE_OBJECT_ID)
						{
							isRumReturnObject = true;
						}
						else if (objectId == RUM_PICKUP_BASE_OBJECT_ID)
						{
							isRumPickupObject = true;
						}

						if (!isRumReturnObject && !isRumPickupObject)
						{
							try
							{
								ObjectComposition objectComposition = client.getObjectDefinition(objectId);
								if (objectComposition != null)
								{
									ObjectComposition activeImpostor = objectComposition.getImpostor();
									if (activeImpostor != null)
									{
										int impostorId = activeImpostor.getId();
										if (impostorId == RUM_RETURN_IMPOSTOR_ID)
										{
											isRumReturnObject = true;
										}
										else if (impostorId == RUM_PICKUP_IMPOSTOR_ID)
										{
											isRumPickupObject = true;
										}
									}
								}
							}
							catch (Exception e)
							{
							}
						}

						if (isRumReturnObject)
						{
							// Use the boat's real world location, not the gameObject's location
							if (state.getRumReturnLocation() == null || !state.getRumReturnLocation().equals(boatWorldLocation))
							{
								state.setRumReturnLocation(boatWorldLocation);
								log.info("Found rum return location: {} (ObjectID: {}, Plane: {}, SceneTile: [{},{}])",
									boatWorldLocation, objectId, planeIndex, xIndex, yIndex);
								calculateExclusionZoneBounds(boatWorldLocation);
							}
						}
						else if (isRumPickupObject)
						{
							// Use the boat's real world location, not the gameObject's location
							if (state.getRumPickupLocation() == null || !state.getRumPickupLocation().equals(boatWorldLocation))
							{
								state.setRumPickupLocation(boatWorldLocation);
								log.info("Found rum pickup location: {} (ObjectID: {}, Plane: {}, SceneTile: [{},{}])",
									boatWorldLocation, objectId, planeIndex, xIndex, yIndex);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Calculates exclusion zone boundaries from rum return location
	 * The exclusion zone is the center island area we circle around
	 */
	private void calculateExclusionZoneBounds(WorldPoint rumReturnWorldLocation)
	{
		int exclusionZoneMinX = rumReturnWorldLocation.getX() + EXCLUSION_MIN_X_OFFSET;
		int exclusionZoneMaxX = rumReturnWorldLocation.getX() + EXCLUSION_MAX_X_OFFSET;
		int exclusionZoneMinY = rumReturnWorldLocation.getY() + EXCLUSION_MIN_Y_OFFSET;
		int exclusionZoneMaxY = rumReturnWorldLocation.getY() + EXCLUSION_MAX_Y_OFFSET;

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
	public boolean isPointInsideExclusionZone(WorldPoint worldPoint)
	{
		return worldPoint.getX() >= state.getExclusionZoneMinX()
			&& worldPoint.getX() <= state.getExclusionZoneMaxX()
			&& worldPoint.getY() >= state.getExclusionZoneMinY()
			&& worldPoint.getY() <= state.getExclusionZoneMaxY();
	}
}
