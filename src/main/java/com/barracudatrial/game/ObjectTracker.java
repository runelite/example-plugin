package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles tracking of game objects in the Barracuda Trial minigame
 * Tracks clouds, rocks, speed boosts, lost supplies, and boat location
 */
@Slf4j
public class ObjectTracker
{
	private final Client client;
	private final State state;
	private final SceneScanner sceneScanner;

	private static final int LIGHTNING_CLOUD_IDLE = 15490;
	private static final int LIGHTNING_CLOUD_ATTACKING = 15491;

	// Lost supplies use a multiloc/impostor system: the active impostor (59244) indicates collectible state
	private static final Set<Integer> LOST_SUPPLIES_BASE_IDS = Set.of(
		59240, 59241, 59242, 59243, 59244, 59245, 59246, 59247, 59248, 59249,
		59250, 59251, 59252, 59253, 59254, 59255, 59256, 59257, 59258, 59259, 59260,
		59261, 59262, 59263, 59264, 59265, 59266, 59267, 59268, 59269, 59270
	);
	private static final int LOST_SUPPLIES_IMPOSTOR_ID = 59244;

	private static final Set<Integer> ROCK_IDS = Set.of(
		59314, 59315, 60437, 60438, 60440, 60441, 60442, 60443, 60444, 60453
	);

	// 2x speed boost
	private static final Set<Integer> SPEED_BOOST_IDS = Set.of(
		60352
	);

	public ObjectTracker(Client client, State state, SceneScanner sceneScanner)
	{
		this.client = client;
		this.state = state;
		this.sceneScanner = sceneScanner;
	}

	/**
	 * Updates lightning cloud tracking by scanning all NPCs
	 * Spawn/despawn events are unreliable, so we actively scan instead
	 */
	public void updateLightningCloudTracking()
	{
		if (!state.isInTrialArea())
		{
			state.getLightningClouds().clear();
			return;
		}

		state.getLightningClouds().clear();

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		for (NPC npc : topLevelWorldView.npcs())
		{
			if (npc == null)
			{
				continue;
			}

			int npcId = npc.getId();
			if (npcId == LIGHTNING_CLOUD_IDLE || npcId == LIGHTNING_CLOUD_ATTACKING)
			{
				state.getLightningClouds().add(npc);
			}
		}
	}

	/**
	 * Updates rocks and speed boosts by scanning the scene
	 * Also maintains persistent storage of known locations
	 */
	public void updateRocksAndSpeedBoosts()
	{
		if (!state.isInTrialArea())
		{
			state.getRocks().clear();
			state.getSpeedBoosts().clear();
			return;
		}

		state.getRocks().clear();
		state.getSpeedBoosts().clear();

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		Scene scene = topLevelWorldView.getScene();
		if (scene == null)
		{
			return;
		}

		scanSceneForRocksAndSpeedBoosts(scene);
	}

	private void scanSceneForRocksAndSpeedBoosts(Scene scene)
	{
		Tile[][][] regularTiles = scene.getTiles();
		if (regularTiles != null)
		{
			scanTileArrayForRocksAndSpeedBoosts(regularTiles);
		}

		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			scanTileArrayForRocksAndSpeedBoosts(extendedTiles);
		}
	}

	private void scanTileArrayForRocksAndSpeedBoosts(Tile[][][] tileArray)
	{
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

						if (ROCK_IDS.contains(objectId))
						{
							state.getRocks().add(gameObject);
							state.getKnownRockLocations().add(gameObject.getWorldLocation());
						}
						else if (SPEED_BOOST_IDS.contains(objectId))
						{
							state.getSpeedBoosts().add(gameObject);
							state.getKnownSpeedBoostLocations().add(gameObject.getWorldLocation());
						}
					}
				}
			}
		}
	}

	/**
	 * Updates all rocks in scene for debug/ID display purposes
	 * Includes both known rocks and potential unknown obstacle objects
	 */
	public void updateAllRocksInScene()
	{
		if (!state.isInTrialArea())
		{
			state.getAllRocksInScene().clear();
			return;
		}

		state.getAllRocksInScene().clear();

		Scene scene = client.getScene();
		Tile[][][] tileArray = scene.getTiles();
		int currentPlane = client.getPlane();

		if (tileArray != null && tileArray[currentPlane] != null)
		{
			for (int xIndex = 0; xIndex < tileArray[currentPlane].length; xIndex++)
			{
				for (int yIndex = 0; yIndex < tileArray[currentPlane][xIndex].length; yIndex++)
				{
					Tile tile = tileArray[currentPlane][xIndex][yIndex];
					if (tile == null)
					{
						continue;
					}

					GameObject[] gameObjects = tile.getGameObjects();
					if (gameObjects == null)
					{
						continue;
					}

					for (GameObject gameObject : gameObjects)
					{
						if (gameObject == null)
						{
							continue;
						}

						int objectId = gameObject.getId();

						if (ROCK_IDS.contains(objectId))
						{
							state.getAllRocksInScene().add(gameObject);
							continue;
						}

						if (LOST_SUPPLIES_BASE_IDS.contains(objectId) || objectId == LOST_SUPPLIES_IMPOSTOR_ID)
						{
							continue;
						}
						if (SPEED_BOOST_IDS.contains(objectId))
						{
							continue;
						}
						if (objectId == State.RUM_RETURN_BASE_OBJECT_ID || objectId == State.RUM_RETURN_IMPOSTOR_ID ||
							objectId == State.RUM_PICKUP_BASE_OBJECT_ID || objectId == State.RUM_PICKUP_IMPOSTOR_ID)
						{
							continue;
						}

						ObjectComposition objectComposition = client.getObjectDefinition(objectId);
						if (objectComposition != null)
						{
							String objectName = objectComposition.getName();
							if (objectName != null)
							{
								String objectNameLowerCase = objectName.toLowerCase();
								if (objectNameLowerCase.contains("rock") || objectNameLowerCase.contains("boulder") ||
									objectNameLowerCase.contains("obstacle") || objectNameLowerCase.contains("debris"))
								{
									state.getAllRocksInScene().add(gameObject);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Updates lost supplies by scanning the scene
	 * Returns true if supplies changed (requiring path recalculation)
	 */
	public boolean updateLostSuppliesTracking()
	{
		if (!state.isInTrialArea())
		{
			return false;
		}

		Set<GameObject> newlyFoundLostSupplies = new HashSet<>();

		Scene scene = client.getScene();
		if (scene == null)
		{
			return false;
		}

		scanSceneForLostSupplies(scene, newlyFoundLostSupplies);

		if (!state.getLostSupplies().equals(newlyFoundLostSupplies))
		{
			state.getLostSupplies().clear();
			state.getLostSupplies().addAll(newlyFoundLostSupplies);
			updateLostSuppliesLapAssignments(newlyFoundLostSupplies);
			return true;
		}

		return false;
	}

	private void updateLostSuppliesLapAssignments(Set<GameObject> newlyFoundLostSupplies)
	{
		state.getLostSuppliesForCurrentLap().retainAll(newlyFoundLostSupplies);
		state.getLostSuppliesForFutureLaps().retainAll(newlyFoundLostSupplies);
	}

	public void scanSceneForLostSupplies(Scene scene, Set<GameObject> newlyFoundLostSupplies)
	{
		Tile[][][] regularTiles = scene.getTiles();
		if (regularTiles != null)
		{
			scanTileArrayForLostSupplies(regularTiles, newlyFoundLostSupplies);
		}

		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			scanTileArrayForLostSupplies(extendedTiles, newlyFoundLostSupplies);
		}
	}

	private void scanTileArrayForLostSupplies(Tile[][][] tileArray, Set<GameObject> newlyFoundLostSupplies)
	{
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

					processLostSupplyTile(tile, newlyFoundLostSupplies);
				}
			}
		}
	}

	public void processLostSupplyTile(Tile tile, Set<GameObject> newlyFoundLostSupplies)
	{
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects == null)
		{
			return;
		}

		for (GameObject gameObject : gameObjects)
		{
			if (gameObject == null)
			{
				continue;
			}

			if (!LOST_SUPPLIES_BASE_IDS.contains(gameObject.getId()))
			{
				continue;
			}

			WorldPoint supplyWorldLocation = gameObject.getWorldLocation();

			boolean isNewSpawnLocation = state.getKnownLostSuppliesSpawnLocations().add(supplyWorldLocation);
			if (isNewSpawnLocation)
			{
				log.debug("Discovered supply spawn location at {}, total known: {}",
					supplyWorldLocation, state.getKnownLostSuppliesSpawnLocations().size());
			}

			if (isLostSupplyCurrentlyCollectible(gameObject))
			{
				newlyFoundLostSupplies.add(gameObject);
			}
		}
	}

	/**
	 * Checks if a lost supply object is in collectible state
	 * Uses the multiloc/impostor system to determine collectibility
	 */
	public boolean isLostSupplyCurrentlyCollectible(GameObject gameObject)
	{
		try
		{
			ObjectComposition objectComposition = client.getObjectDefinition(gameObject.getId());
			if (objectComposition == null)
			{
				return false;
			}

			int[] impostorIds = objectComposition.getImpostorIds();
			if (impostorIds == null)
			{
				return gameObject.getId() == LOST_SUPPLIES_IMPOSTOR_ID;
			}

			ObjectComposition activeImpostor = objectComposition.getImpostor();
			if (activeImpostor == null)
			{
				return false;
			}

			return activeImpostor.getId() == LOST_SUPPLIES_IMPOSTOR_ID;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Updates the boat location (player's boat WorldEntity)
	 * Falls back to player location if boat cannot be found
	 */
	public void updatePlayerBoatLocation()
	{
		if (!state.isInTrialArea())
		{
			state.setBoatLocation(null);
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			state.setBoatLocation(null);
			return;
		}

		try
		{
			WorldView playerWorldView = localPlayer.getWorldView();
			if (playerWorldView == null)
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			int playerWorldViewId = playerWorldView.getId();

			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			WorldEntity boatWorldEntity = topLevelWorldView.worldEntities().byIndex(playerWorldViewId);
			if (boatWorldEntity == null)
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			var boatLocalLocation = boatWorldEntity.getLocalLocation();
			if (boatLocalLocation != null)
			{
				state.setBoatLocation(WorldPoint.fromLocalInstance(client, boatLocalLocation));
			}
			else
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
			}
		}
		catch (Exception e)
		{
			state.setBoatLocation(localPlayer.getWorldLocation());
			log.debug("Error getting boat location: {}", e.getMessage());
		}
	}
}
