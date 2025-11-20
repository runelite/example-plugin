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

	// Object IDs
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
	public void updateCloudTracking()
	{
		if (!state.isInTrialArea())
		{
			state.getLightningClouds().clear();
			return;
		}

		// Actively scan for lightning cloud NPCs instead of relying on spawn/despawn events
		state.getLightningClouds().clear();

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		// Scan all NPCs in the world view
		for (NPC npc : worldView.npcs())
		{
			if (npc == null)
			{
				continue;
			}

			int id = npc.getId();
			if (id == LIGHTNING_CLOUD_IDLE || id == LIGHTNING_CLOUD_ATTACKING)
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

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return;
		}

		scanSceneForRocksAndBoosts(scene);
	}

	/**
	 * Scans a scene for rocks and speed boosts
	 */
	private void scanSceneForRocksAndBoosts(Scene scene)
	{
		// Check regular tiles
		Tile[][][] tiles = scene.getTiles();
		if (tiles != null)
		{
			scanTilesForRocksAndBoosts(tiles);
		}

		// Check extended tiles
		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			scanTilesForRocksAndBoosts(extendedTiles);
		}
	}

	/**
	 * Helper method to scan a tile array for rocks and speed boosts
	 */
	private void scanTilesForRocksAndBoosts(Tile[][][] tiles)
	{
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

						int id = gameObject.getId();

						if (ROCK_IDS.contains(id))
						{
							state.getRocks().add(gameObject);
							state.getKnownRockLocations().add(gameObject.getWorldLocation());
						}
						else if (SPEED_BOOST_IDS.contains(id))
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
	public void updateAllRocks()
	{
		if (!state.isInTrialArea())
		{
			state.getAllRocksInScene().clear();
			return;
		}

		// Scan entire scene for all GameObjects that might be rocks
		state.getAllRocksInScene().clear();

		Scene scene = client.getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();

		if (tiles != null && tiles[plane] != null)
		{
			for (int x = 0; x < tiles[plane].length; x++)
			{
				for (int y = 0; y < tiles[plane][x].length; y++)
				{
					Tile tile = tiles[plane][x][y];
					if (tile == null)
					{
						continue;
					}

					GameObject[] gameObjects = tile.getGameObjects();
					if (gameObjects == null)
					{
						continue;
					}

					for (GameObject obj : gameObjects)
					{
						if (obj == null)
						{
							continue;
						}

						int id = obj.getId();

						// Known rocks
						if (ROCK_IDS.contains(id))
						{
							state.getAllRocksInScene().add(obj);
							continue;
						}

						// Look for potential unknown rocks - objects that could be obstacles
						// Exclude known IDs (lost supplies, speed boosts, rum objects)
						if (LOST_SUPPLIES_BASE_IDS.contains(id) || id == LOST_SUPPLIES_IMPOSTOR_ID)
						{
							continue;
						}
						if (SPEED_BOOST_IDS.contains(id))
						{
							continue;
						}
						if (id == State.RUM_RETURN_BASE_OBJECT_ID || id == State.RUM_RETURN_IMPOSTOR_ID ||
							id == State.RUM_PICKUP_BASE_OBJECT_ID || id == State.RUM_PICKUP_IMPOSTOR_ID)
						{
							continue;
						}

						// Check if object has a name containing "rock" or similar obstacle keywords
						ObjectComposition comp = client.getObjectDefinition(id);
						if (comp != null)
						{
							String name = comp.getName();
							if (name != null)
							{
								String nameLower = name.toLowerCase();
								if (nameLower.contains("rock") || nameLower.contains("boulder") ||
									nameLower.contains("obstacle") || nameLower.contains("debris"))
								{
									state.getAllRocksInScene().add(obj);
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
	public boolean updateLostSupplies()
	{
		if (!state.isInTrialArea())
		{
			return false;
		}

		Set<GameObject> newLostSupplies = new HashSet<>();

		Scene scene = client.getScene();
		if (scene == null)
		{
			return false;
		}

		scanSceneForLostSupplies(scene, newLostSupplies);

		if (!state.getLostSupplies().equals(newLostSupplies))
		{
			state.getLostSupplies().clear();
			state.getLostSupplies().addAll(newLostSupplies);
			updateLostSuppliesAssignments(newLostSupplies);
			return true; // Supplies changed
		}

		return false; // No change
	}

	/**
	 * Updates lap-specific supply assignments when supplies change
	 */
	private void updateLostSuppliesAssignments(Set<GameObject> newLostSupplies)
	{
		// Remove supplies that no longer exist from our tracking sets
		state.getLostSuppliesForCurrentLap().retainAll(newLostSupplies);
		state.getLostSuppliesForFutureLaps().retainAll(newLostSupplies);
	}

	/**
	 * Scans scene for lost supplies
	 */
	public void scanSceneForLostSupplies(Scene scene, Set<GameObject> newLostSupplies)
	{
		// Check regular tiles
		Tile[][][] tiles = scene.getTiles();
		if (tiles != null)
		{
			scanTilesForLostSupplies(tiles, newLostSupplies);
		}

		// Check extended tiles
		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			scanTilesForLostSupplies(extendedTiles, newLostSupplies);
		}
	}

	/**
	 * Helper method to scan a tile array for lost supplies
	 */
	private void scanTilesForLostSupplies(Tile[][][] tiles, Set<GameObject> newLostSupplies)
	{
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

					processLostSupplyTile(tile, newLostSupplies);
				}
			}
		}
	}

	/**
	 * Processes a single tile for lost supply objects
	 */
	public void processLostSupplyTile(Tile tile, Set<GameObject> newLostSupplies)
	{
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects == null)
		{
			return;
		}

		for (GameObject obj : gameObjects)
		{
			if (obj == null)
			{
				continue;
			}

			if (!LOST_SUPPLIES_BASE_IDS.contains(obj.getId()))
			{
				continue;
			}

			WorldPoint supplyLocation = obj.getWorldLocation();

			boolean isNew = state.getKnownLostSuppliesSpawnLocations().add(supplyLocation);
			if (isNew)
			{
				log.debug("Discovered supply spawn location at {}, total known: {}",
					supplyLocation, state.getKnownLostSuppliesSpawnLocations().size());
			}

			if (isLostSupplyCollectible(obj))
			{
				newLostSupplies.add(obj);
			}
		}
	}

	/**
	 * Checks if a lost supply object is in collectible state
	 * Uses the multiloc/impostor system to determine collectibility
	 */
	public boolean isLostSupplyCollectible(GameObject obj)
	{
		try
		{
			ObjectComposition comp = client.getObjectDefinition(obj.getId());
			if (comp == null)
			{
				return false;
			}

			// Check if this has impostor IDs
			int[] impostorIds = comp.getImpostorIds();
			if (impostorIds == null)
			{
				// Not a multiloc - check if ID is directly 59244
				return obj.getId() == LOST_SUPPLIES_IMPOSTOR_ID;
			}

			// Get the active impostor
			ObjectComposition impostor = comp.getImpostor();
			if (impostor == null)
			{
				return false;
			}

			// Check if active impostor is 59244
			return impostor.getId() == LOST_SUPPLIES_IMPOSTOR_ID;
		}
		catch (Exception e)
		{
			// Error getting composition or impostor
			return false;
		}
	}

	/**
	 * Updates the boat location (player's boat WorldEntity)
	 * Falls back to player location if boat cannot be found
	 */
	public void updateBoatLocation()
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
			// Get the player's WorldView ID (the boat they're in)
			WorldView playerWorldView = localPlayer.getWorldView();
			if (playerWorldView == null)
			{
				// Fallback to player location if not in a boat
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			int worldViewId = playerWorldView.getId();

			// Get the boat's WorldEntity from the top-level world view
			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			WorldEntity worldEntity = topLevelWorldView.worldEntities().byIndex(worldViewId);
			if (worldEntity == null)
			{
				// Fallback to player location
				state.setBoatLocation(localPlayer.getWorldLocation());
				return;
			}

			// Convert the boat's LocalLocation to WorldPoint
			var localLocation = worldEntity.getLocalLocation();
			if (localLocation != null)
			{
				state.setBoatLocation(WorldPoint.fromLocalInstance(client, localLocation));
			}
			else
			{
				state.setBoatLocation(localPlayer.getWorldLocation());
			}
		}
		catch (Exception e)
		{
			// Fallback to player location on any error
			state.setBoatLocation(localPlayer.getWorldLocation());
			log.debug("Error getting boat location: {}", e.getMessage());
		}
	}
}
