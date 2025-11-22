package com.barracudatrial.game;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.barracudatrial.game.RouteCapture.formatWorldPoint;

/**
 * Handles tracking of game objects in the Barracuda Trial minigame
 * Tracks clouds, rocks, speed boosts, lost supplies, and boat location
 */
@Slf4j
public class ObjectTracker
{
	private final Client client;
	private final State state;

	private static final int LIGHTNING_CLOUD_IDLE = NpcID.SAILING_SEA_STORMY_CLOUD;
	private static final int LIGHTNING_CLOUD_ATTACKING = NpcID.SAILING_SEA_STORMY_LIGHTNING_STRIKE;

	// Lost supplies use a multiloc/impostor system: the active impostor (59244) indicates collectible state
	// Most IDs don't have constants, only 59244 has ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_SUPPLIES
	public static final Set<Integer> LOST_SUPPLIES_BASE_IDS = Set.of(
		59234, 59235, 59240, 59241, 59242, 59243, ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_SUPPLIES, 59245, 59246, 59247, 59248, 59249,
		59250, 59251, 59252, 59253, 59254, 59255, 59256, 59257, 59258, 59259,
		59260, 59261, 59262, 59263, 59264, 59265, 59266, 59267, 59268, 59269,
		59270, 59271, 59272, 59273, 59274, 59275, 59276, 59277, 59278, 59279,
		59280
	);
	public static final int LOST_SUPPLIES_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_SUPPLIES;

	// Most rock IDs don't have constants available
	private static final Set<Integer> ROCK_IDS = Set.of(
		59314, 59315, 60437, 60438, 60440, 60441, 60442, 60443, 60444
	);

	private static final Set<Integer> SPEED_BOOST_IDS = Set.of(
		ObjectID.SAILING_RAPIDS, ObjectID.SAILING_RAPIDS_STRONG,
		ObjectID.SAILING_RAPIDS_POWERFUL, ObjectID.SAILING_RAPIDS_DEADLY
	);

	public ObjectTracker(Client client, State state)
	{
		this.client = client;
		this.state = state;
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

	public static boolean IsCloudSafe(int animationId)
	{
		return animationId == State.CLOUD_ANIM_HARMLESS || animationId == State.CLOUD_ANIM_HARMLESS_ALT;
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
        for (Tile[][] tiles : tileArray) {
            if (tiles == null) {
                continue;
            }

            for (Tile[] value : tiles) {
                if (value == null) {
                    continue;
                }

                for (Tile tile : value) {
                    if (tile == null) {
                        continue;
                    }

                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null) {
                            continue;
                        }

                        int objectId = gameObject.getId();

                        if (ROCK_IDS.contains(objectId)) {
                            state.getRocks().add(gameObject);
                            state.getKnownRockLocations().add(gameObject.getWorldLocation());
                        } else if (SPEED_BOOST_IDS.contains(objectId)) {
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
	 */
	public void updateLostSuppliesTracking()
	{
		if (!state.isInTrialArea())
		{
			return;
		}

		Set<GameObject> newlyFoundLostSupplies = new HashSet<>();

		Scene scene = client.getScene();
		if (scene == null)
		{
			return;
		}

		scanSceneForLostSupplies(scene, newlyFoundLostSupplies);

		if (!state.getLostSupplies().equals(newlyFoundLostSupplies))
		{
			// Detect collected shipments: supplies that disappeared while the collected count increased
			detectAndMarkCollectedShipments(newlyFoundLostSupplies);

			state.getLostSupplies().clear();
			state.getLostSupplies().addAll(newlyFoundLostSupplies);
		}
	}

	/**
	 * Detects shipments that were collected (disappeared from scene while count increased)
	 * DEPRECATED: This is the old scene-scanning approach. Use updateRouteWaypointShipmentTracking() instead.
	 */
	private void detectAndMarkCollectedShipments(Set<GameObject> currentSupplies)
	{
		Set<WorldPoint> disappearedSupplyLocations = new HashSet<>();
		for (GameObject previousSupply : state.getLostSupplies())
		{
			if (!currentSupplies.contains(previousSupply))
			{
				disappearedSupplyLocations.add(previousSupply.getWorldLocation());
			}
		}

		// Game quirk: Shipments only disappear when collected
		if (!disappearedSupplyLocations.isEmpty())
		{
			for (WorldPoint location : disappearedSupplyLocations)
			{
				state.markWaypointCompleted(location);
			}
			log.debug("Marked {} shipments as collected: {}", disappearedSupplyLocations.size(), disappearedSupplyLocations);
		}
	}

	/**
	 * Checks route waypoint tiles for shipment collection.
	 * Detection method: If a base shipment object exists on a tile BUT the impostor ID (59244)
	 * is missing, the shipment has been collected.
	 * Only checks waypoints within 7 tiles of the player - close enough that impostor ID would be visible.
	 * @return true if any shipments were collected this tick
	 */
	public boolean updateRouteWaypointShipmentTracking()
	{
		if (!state.isInTrialArea() || state.getCurrentStaticRoute() == null)
		{
			return false;
		}

		Set<WorldPoint> waypointsToCheck = new HashSet<>();
		for (com.barracudatrial.game.route.RouteWaypoint waypoint : state.getCurrentStaticRoute())
		{
			if (waypoint.getType() == com.barracudatrial.game.route.RouteWaypoint.WaypointType.SHIPMENT)
			{
				WorldPoint waypointLocation = waypoint.getLocation();
				if (!state.isWaypointCompleted(waypointLocation))
				{
					waypointsToCheck.add(waypointLocation);
				}
			}
		}

		List<WorldPoint> collectedShipments = checkShipmentsForCollection(waypointsToCheck);

		for (WorldPoint collected : collectedShipments)
		{
			state.markWaypointCompleted(collected);
			log.debug("Shipment collected at route waypoint: {}", collected);
		}

		return !collectedShipments.isEmpty();
	}

	/**
	 * Core shipment collection detection logic.
	 * Checks a set of shipment locations and returns which ones were collected this tick.
	 * Detection method: If a base shipment object exists BUT the impostor ID (59244) is missing,
	 * the shipment has been collected.
	 * Only checks locations within 7 tiles of the player (impostor ID only visible when close).
	 *
	 * @param shipmentsToCheck Set of shipment locations to check for collection
	 * @return List of shipments that were collected this tick
	 */
	public List<WorldPoint> checkShipmentsForCollection(Set<WorldPoint> shipmentsToCheck)
	{
		List<WorldPoint> collectedShipments = new ArrayList<>();

		if (shipmentsToCheck.isEmpty())
		{
			return collectedShipments;
		}

		Scene scene = client.getScene();
		if (scene == null)
		{
			return collectedShipments;
		}

		WorldPoint playerBoatLocation = state.getBoatLocation();
		if (playerBoatLocation == null)
		{
			return collectedShipments;
		}

		for (WorldPoint shipmentLocation : shipmentsToCheck)
		{
			// Only check waypoints within 7 tiles (impostor ID only visible when close)
			double distance = Math.sqrt(
				Math.pow(shipmentLocation.getX() - playerBoatLocation.getX(), 2) +
				Math.pow(shipmentLocation.getY() - playerBoatLocation.getY(), 2)
			);
			if (distance > 7)
			{
				continue;
			}

			if (hasBaseShipmentButNoImpostor(scene, shipmentLocation))
			{
				collectedShipments.add(shipmentLocation);
			}
		}

		return collectedShipments;
	}

	/**
	 * Updates visible supply locations by scanning all tiles (including extended tiles).
	 * This populates state.visibleSupplyLocations with all shipment WorldPoints currently visible in the scene.
	 * Optimized to skip tiles we already know have shipments.
	 */
	public void updateVisibleSupplyLocations()
	{
		if (!state.isInTrialArea())
		{
			state.setVisibleSupplyLocations(new HashSet<>());
			return;
		}

		Scene scene = client.getScene();
		if (scene == null)
		{
			state.setVisibleSupplyLocations(new HashSet<>());
			return;
		}

		Set<WorldPoint> oldSupplyLocations = state.getVisibleSupplyLocations();
		Set<WorldPoint> newVisibleSupplyLocations = scanTileArrayForShipments(scene.getTiles(), oldSupplyLocations);

		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			newVisibleSupplyLocations.addAll(scanTileArrayForShipments(extendedTiles, oldSupplyLocations));
		}

		state.setVisibleSupplyLocations(newVisibleSupplyLocations);
	}

	/**
	 * Checks all currently visible shipments for collection.
	 * Used during route capture mode to detect shipments without a predefined route.
	 * Assumes updateVisibleSupplyLocations() has already been called this tick.
	 *
	 * @return List of shipments that were collected this tick
	 */
	public List<WorldPoint> checkAllVisibleShipmentsForCollection()
	{
		return checkShipmentsForCollection(state.getVisibleSupplyLocations());
	}

	/**
	 * Scans a tile array for shipment objects and returns their locations.
	 * Skips logging for locations already in the old set for efficiency.
	 */
	private Set<WorldPoint> scanTileArrayForShipments(Tile[][][] tileArray, Set<WorldPoint> oldSupplyLocations)
	{
		Set<WorldPoint> foundSupplyLocations = new HashSet<>();

		if (tileArray == null)
		{
			return foundSupplyLocations;
		}

        for (Tile[][] tiles : tileArray) {
            if (tiles == null) {
                continue;
            }

            for (Tile[] value : tiles) {
                if (value == null) {
                    continue;
                }

                for (Tile tile : value) {
                    if (tile == null) {
                        continue;
                    }

                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null) {
                            continue;
                        }

                        int objectId = gameObject.getId();
                        if (LOST_SUPPLIES_BASE_IDS.contains(objectId) ||
                                objectId == LOST_SUPPLIES_IMPOSTOR_ID) {
                            var worldLocation = gameObject.getWorldLocation();
                            // Skip logging if we already know about this location
                            if (oldSupplyLocations.contains(worldLocation)) {
                                foundSupplyLocations.add(worldLocation);
                                continue;
                            }
                            log.info("[ROUTE CAPTURE] Found shipment id {} we can pick up on {}", gameObject.getId(), formatWorldPoint(worldLocation));
                            foundSupplyLocations.add(worldLocation);
                        }
                    }
                }
            }
        }

		return foundSupplyLocations;
	}

	/**
	 * Checks if a base shipment object exists at a location BUT the impostor ID does not.
	 * This indicates the shipment has been collected (base remains, impostor disappears).
	 */
	private boolean hasBaseShipmentButNoImpostor(Scene scene, WorldPoint worldLocation)
	{
		int plane = worldLocation.getPlane();
		int sceneX = worldLocation.getX() - scene.getBaseX();
		int sceneY = worldLocation.getY() - scene.getBaseY();

		if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104)
		{
			return false;
		}

		Tile[][][] tiles = scene.getTiles();
		if (tiles == null || tiles[plane] == null)
		{
			return false;
		}

		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}

		boolean hasBaseShipment = false;
		boolean hasImpostor = false;

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject == null)
			{
				continue;
			}

			int objectId = gameObject.getId();

			if (LOST_SUPPLIES_BASE_IDS.contains(objectId))
			{
				hasBaseShipment = true;
			}

			if (objectId == LOST_SUPPLIES_IMPOSTOR_ID)
			{
				hasImpostor = true;
			}
		}

		return hasBaseShipment && !hasImpostor;
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
        for (Tile[][] tiles : tileArray) {
            if (tiles == null) {
                continue;
            }

            for (Tile[] value : tiles) {
                if (value == null) {
                    continue;
                }

                for (Tile tile : value) {
                    if (tile == null) {
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

	/**
	 * Updates the front boat tile position for pathfinding
	 * The front is calculated as 3 tiles ahead of the boat center in the direction of travel
	 */
	public void updateFrontBoatTile()
	{
		if (!state.isInTrialArea())
		{
			state.setFrontBoatTileEstimatedActual(null);
			state.setFrontBoatTileLocal(null);
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			state.setFrontBoatTileEstimatedActual(null);
			state.setFrontBoatTileLocal(null);
			return;
		}

		try
		{
			WorldView playerWorldView = localPlayer.getWorldView();
			if (playerWorldView == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			int playerWorldViewId = playerWorldView.getId();
			WorldEntity boatWorldEntity = topLevelWorldView.worldEntities().byIndex(playerWorldViewId);
			if (boatWorldEntity == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			WorldView boatWorldView = boatWorldEntity.getWorldView();
			if (boatWorldView == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			Scene boatScene = boatWorldView.getScene();
			if (boatScene == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			// Find the local player in the boat's WorldView
			Player boatPlayer = null;
			for (Player p : boatWorldView.players())
			{
				if (p != null && p.equals(localPlayer))
				{
					boatPlayer = p;
					break;
				}
			}

			if (boatPlayer == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			// Find the NPC on the boat (center marker)
			NPC boatNpc = null;
			for (NPC npc : boatWorldView.npcs())
			{
				if (npc != null)
				{
					boatNpc = npc;
					break;
				}
			}

			if (boatNpc == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			LocalPoint npcLocalPoint = boatNpc.getLocalLocation();
			LocalPoint boatPlayerLocalPoint = boatPlayer.getLocalLocation();

			if (npcLocalPoint == null || boatPlayerLocalPoint == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				state.setFrontBoatTileLocal(null);
				return;
			}

			// Calculate direction from player (back) to NPC (middle) in scene tiles
			int npcSceneX = npcLocalPoint.getSceneX();
			int npcSceneY = npcLocalPoint.getSceneY();
			int playerSceneX = boatPlayerLocalPoint.getSceneX();
			int playerSceneY = boatPlayerLocalPoint.getSceneY();

			int deltaX = npcSceneX - playerSceneX;
			int deltaY = npcSceneY - playerSceneY;

			// Front of boat: extend 3 tiles from NPC
			int frontSceneX = npcSceneX + (deltaX * 3);
			int frontSceneY = npcSceneY + (deltaY * 3);

			// Convert to LocalPoint in boat's coordinate system (for rendering)
			int baseX = boatScene.getBaseX();
			int baseY = boatScene.getBaseY();
			LocalPoint frontLocalPoint = LocalPoint.fromScene(baseX + frontSceneX, baseY + frontSceneY, boatScene);

			// Store the boat-relative LocalPoint (smooth sub-tile positioning, for visual rendering)
			state.setFrontBoatTileLocal(frontLocalPoint);

			// Transform from boat's coordinate system to main world (for tile-based pathfinding)
			LocalPoint frontMainWorldLocal = boatWorldEntity.transformToMainWorld(frontLocalPoint);
			if (frontMainWorldLocal == null)
			{
				state.setFrontBoatTileEstimatedActual(null);
				return;
			}

			// Convert to WorldPoint (for pathfinding A* algorithm)
			WorldPoint frontWorldPoint = WorldPoint.fromLocalInstance(client, frontMainWorldLocal);
			state.setFrontBoatTileEstimatedActual(frontWorldPoint);
		}
		catch (Exception e)
		{
			state.setFrontBoatTileEstimatedActual(null);
			state.setFrontBoatTileLocal(null);
			log.debug("Error calculating front boat tile: {}", e.getMessage());
		}
	}
}
