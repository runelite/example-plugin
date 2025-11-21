package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * Manages rum locations and exclusion zone calculations for Barracuda Trial
 */
@Slf4j
public class LocationManager implements PathPlanner.LocationHelper
{
	private final Client client;
	private final State state;
	private final SceneScanner sceneScanner;

	private static final int RUM_RETURN_BASE_OBJECT_ID = 59237; // No constant available
	private static final int RUM_RETURN_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD;
	private static final int RUM_PICKUP_BASE_OBJECT_ID = 59240; // No constant available
	private static final int RUM_PICKUP_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD;

	private static final int PICKUP_OFFSET_X = 24;
	private static final int PICKUP_OFFSET_Y = -128;

	private static final int EXCLUSION_MIN_X_OFFSET = 47;
	private static final int EXCLUSION_MAX_X_OFFSET = 102;
	private static final int EXCLUSION_MIN_Y_OFFSET = -51;
	private static final int EXCLUSION_MAX_Y_OFFSET = 12;

	// Actual pickup is impassable, so we target 2 tiles north
	private static final int PATHFINDING_PICKUP_OFFSET_Y = 2;
	// Actual dropoff is impassable, so we target 2 tiles south
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

			scanSceneForRumReturnAndPickupLocations(entityScene);
		}
	}

	private void scanSceneForRumReturnAndPickupLocations(Scene scene)
	{
		Tile[][][] tileArray = scene.getTiles();
		if (tileArray == null)
		{
			return;
		}

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
							WorldPoint rumReturnWorldLocation = gameObject.getWorldLocation();
							if (state.getRumReturnLocation() == null || !state.getRumReturnLocation().equals(rumReturnWorldLocation))
							{
								state.setRumReturnLocation(rumReturnWorldLocation);
								log.info("Found rum return location: {}", rumReturnWorldLocation);
								calculateExclusionZoneBounds(rumReturnWorldLocation);
							}
						}
						else if (isRumPickupObject)
						{
							WorldPoint rumPickupWorldLocation = gameObject.getWorldLocation();
							if (state.getRumPickupLocation() == null || !state.getRumPickupLocation().equals(rumPickupWorldLocation))
							{
								state.setRumPickupLocation(rumPickupWorldLocation);
								log.info("Found rum pickup location: {}", rumPickupWorldLocation);
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
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			log.debug("Cannot initialize trial locations - no world view");
			return;
		}

		Scene scene = topLevelWorldView.getScene();
		if (scene == null)
		{
			log.debug("Cannot initialize trial locations - no scene");
			return;
		}

		searchForRumLocationsInWorldEntities();
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

	public double calculateDistanceFromPointToExclusionZone(WorldPoint worldPoint)
	{
		int pointX = worldPoint.getX();
		int pointY = worldPoint.getY();

		if (isPointInsideExclusionZone(worldPoint))
		{
			return 0;
		}

		double distanceToExclusionZoneInXDirection;
		if (pointX < state.getExclusionZoneMinX())
		{
			distanceToExclusionZoneInXDirection = state.getExclusionZoneMinX() - pointX;
		}
		else if (pointX > state.getExclusionZoneMaxX())
		{
			distanceToExclusionZoneInXDirection = pointX - state.getExclusionZoneMaxX();
		}
		else
		{
			distanceToExclusionZoneInXDirection = 0;
		}

		double distanceToExclusionZoneInYDirection;
		if (pointY < state.getExclusionZoneMinY())
		{
			distanceToExclusionZoneInYDirection = state.getExclusionZoneMinY() - pointY;
		}
		else if (pointY > state.getExclusionZoneMaxY())
		{
			distanceToExclusionZoneInYDirection = pointY - state.getExclusionZoneMaxY();
		}
		else
		{
			distanceToExclusionZoneInYDirection = 0;
		}

		if (distanceToExclusionZoneInXDirection == 0)
		{
			return distanceToExclusionZoneInYDirection;
		}
		else if (distanceToExclusionZoneInYDirection == 0)
		{
			return distanceToExclusionZoneInXDirection;
		}
		else
		{
			return Math.sqrt(distanceToExclusionZoneInXDirection * distanceToExclusionZoneInXDirection + distanceToExclusionZoneInYDirection * distanceToExclusionZoneInYDirection);
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
