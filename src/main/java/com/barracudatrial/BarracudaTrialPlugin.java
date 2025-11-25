package com.barracudatrial;

import com.barracudatrial.game.*;
import com.barracudatrial.game.route.JubblyJiveConfig;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.game.route.TrialType;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@PluginDescriptor(
	name = "Barracuda Trial",
	description = "Displays optimal paths and highlights for Barracuda Trial minigame",
	tags = {"sailing", "barracuda", "minigame", "overlay"}
)
public class BarracudaTrialPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BarracudaTrialConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BarracudaTrialOverlay overlay;

	@Getter
	private final State gameState = new State();

	@Getter
	private CachedConfig cachedConfig;

	private ObjectTracker objectTracker;
	private LocationManager locationManager;
	private ProgressTracker progressTracker;
	private PathPlanner pathPlanner;

	@Getter
	private RouteCapture routeCapture;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Barracuda Trial plugin started!");
		overlayManager.add(overlay);

		cachedConfig = new CachedConfig(config);

		objectTracker = new ObjectTracker(client, gameState);
		locationManager = new LocationManager(client, gameState);
		progressTracker = new ProgressTracker(client, gameState);
		pathPlanner = new PathPlanner(client, gameState, cachedConfig);
		routeCapture = new RouteCapture(gameState);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Barracuda Trial plugin stopped!");
		overlayManager.remove(overlay);
		gameState.resetAllTemporaryState();
		pathPlanner.reset();
		gameState.clearPersistentStorage();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		boolean trialAreaStateChanged = progressTracker.checkIfPlayerIsInTrialArea();
		if (trialAreaStateChanged && !gameState.isInTrialArea())
		{
			routeCapture.reset();
			pathPlanner.reset();
		}
		if (!gameState.isInTrialArea())
		{
			return;
		}

		var trial = gameState.getCurrentTrial();
		if (trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM
			&& (cachedConfig.isShowOptimalPath() || cachedConfig.isHighlightClouds()))
		{
			objectTracker.updateLightningCloudTracking();
		}

		if (trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM
			&& (cachedConfig.isShowOptimalPath() || cachedConfig.isHighlightObjectives()))
		{
			locationManager.updateTemporRumLocations();
		}

		if (cachedConfig.isShowOptimalPath()
			|| cachedConfig.isHighlightSpeedBoosts()
		 	|| routeCapture.isCapturing()
			|| cachedConfig.isHighlightObjectives())
		{
			objectTracker.updateHazardsSpeedBoostsAndToadPillars();
		}

		progressTracker.updateTrialProgressFromWidgets();
		
		if (cachedConfig.isShowOptimalPath())
		{
			objectTracker.updatePlayerBoatLocation();

			objectTracker.updateFrontBoatTile();
		
			boolean shipmentsCollected = objectTracker.updateRouteWaypointShipmentTracking();
			if (shipmentsCollected)
			{
				pathPlanner.recalculateOptimalPathFromCurrentState("shipment collected");
			}
		}

		if (cachedConfig.isShowOptimalPath() || routeCapture.isCapturing())
		{
			objectTracker.updateLostSuppliesTracking();
		}

		// Route capture mode: scan for all supply locations to build routes
		if (routeCapture.isCapturing())
		{
			objectTracker.updateRouteCaptureSupplyLocations();
			List<WorldPoint> collected = objectTracker.checkAllRouteCaptureShipmentsForCollection();
			routeCapture.onShipmentsCollected(collected);
		}

		if (cachedConfig.isShowOptimalPath())
		{
			// Recalculate path periodically to account for moving clouds
			int ticksSinceLastPathRecalculation = gameState.getTicksSinceLastPathRecalc() + 1;
			gameState.setTicksSinceLastPathRecalc(ticksSinceLastPathRecalculation);

			if (ticksSinceLastPathRecalculation >= State.PATH_RECALC_INTERVAL)
			{
				gameState.setTicksSinceLastPathRecalc(0);
				pathPlanner.recalculateOptimalPathFromCurrentState("periodic (game tick)");
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!gameState.isInTrialArea())
		{
			return;
		}

		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String chatMessage = event.getMessage();

		if (chatMessage.contains("You collect the rum"))
		{
			log.debug("Rum collected! Message: {}", chatMessage);
			gameState.setHasThrowableObjective(true);

			if (routeCapture.isCapturing())
			{
				routeCapture.onRumPickedUp();
			}

			var route = gameState.getCurrentStaticRoute();

			if (route != null)
			{
				for (int i = 0, n = route.size(); i < n; i++)
				{
					var waypoint = route.get(i);

					if (waypoint.getType() == RouteWaypoint.WaypointType.RUM_PICKUP
						&& !gameState.isWaypointCompleted(i))
					{
						gameState.markWaypointCompleted(i);
						log.info("Marked RUM_PICKUP waypoint as completed at index {}: {}", i, waypoint.getLocation());
						break;
					}
				}
			}

			pathPlanner.recalculateOptimalPathFromCurrentState("chat: rum collected");
		}
		else if (chatMessage.contains("You deliver the rum"))
		{
			log.debug("Rum delivered! Message: {}", chatMessage);
			gameState.setHasThrowableObjective(false);

			var route = gameState.getCurrentStaticRoute();

			if (route != null)
			{
				for (int i = 0; i < route.size(); i++)
				{
					RouteWaypoint waypoint = route.get(i);
					if (waypoint.getType() == RouteWaypoint.WaypointType.RUM_DROPOFF
						&& !gameState.isWaypointCompleted(i))
					{
						gameState.markWaypointCompleted(i);
						log.info("Marked RUM_DROPOFF waypoint as completed at index {}: {}", i, waypoint.getLocation());
						break;
					}
				}
			}

			var lapsRequired = gameState.getCurrentDifficulty().rumsRequired;
			var nextLapNumber = gameState.getCurrentLap() + 1;
			var isCompletingFinalLap = lapsRequired == nextLapNumber;

			if (isCompletingFinalLap)
			{
				// Reset will be handled by game, no need to reset here
				log.info("Completed all {} laps!", lapsRequired);
			}
			else
			{
				gameState.setCurrentLap(nextLapNumber);
				log.info("Advanced to lap {}/{}", nextLapNumber, lapsRequired);
			}

			pathPlanner.recalculateOptimalPathFromCurrentState("chat: rum delivered");

			if (routeCapture.isCapturing())
			{
				routeCapture.onRumDelivered(isCompletingFinalLap);
			}
		}
		else if (chatMessage.contains("balloon toads. Time to lure"))
		{
			log.debug("Toads collected! Message: {}", chatMessage);
			
			gameState.setHasThrowableObjective(true);

			if (routeCapture.isCapturing())
			{
				// TODO: Handle capturing
				// routeCapture.onRumPickedUp();
			}

			var route = gameState.getCurrentStaticRoute();

			if (route != null)
			{
				for (int i = 0, n = route.size(); i < n; i++)
				{
					var waypoint = route.get(i);

					if (waypoint.getType() == RouteWaypoint.WaypointType.TOAD_PICKUP
						&& !gameState.isWaypointCompleted(i))
					{
						gameState.markWaypointCompleted(i);
						log.info("Marked TOAD_PICKUP waypoint as completed at index {}: {}", i, waypoint.getLocation());
						break;
					}
				}
			}

			pathPlanner.recalculateOptimalPathFromCurrentState("chat: toads collected");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!gameState.isInTrialArea())
		{
			return;
		}
		
		if (!event.getGroup().equals("barracudatrial"))
		{
			return;
		}

		cachedConfig.updateCache();

		if (event.getKey().equals("routeOptimization") && gameState.isInTrialArea())
		{
			pathPlanner.recalculateOptimalPathFromCurrentState("config: route optimization changed");
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!cachedConfig.isDebugMode() || !gameState.isInTrialArea())
		{
			return;
		}

		if (!event.getMenuOption().equals("Examine"))
		{
			return;
		}

		int sceneX = event.getParam0();
		int sceneY = event.getParam1();
		if (sceneX < 0 || sceneY < 0)
		{
			return;
		}

		int plane = client.getPlane();
		WorldPoint worldPoint = WorldPoint.fromScene(client, sceneX, sceneY, plane);

		Scene scene = client.getScene();
		int sceneBaseX = scene != null ? scene.getBaseX() : -1;
		int sceneBaseY = scene != null ? scene.getBaseY() : -1;

		WorldPoint boatWorldLocation = getBoatWorldLocationForSceneTile(sceneX, sceneY, plane);

		int objectId = event.getId();

		String impostorInfo = "none";
		try
		{
			ObjectComposition objectComposition = client.getObjectDefinition(objectId);
			if (objectComposition != null)
			{
				impostorInfo = "none (with object comp)";

				var impostorIds = objectComposition.getImpostorIds();
				if (impostorIds == null)
				{
					impostorInfo = "none (null ids)";
				}
				else
				{
					ObjectComposition activeImpostor = objectComposition.getImpostor();
					if (activeImpostor != null) {
						impostorInfo = String.valueOf(activeImpostor.getId());
					}
				}
			}
		}
		catch (Exception e)
		{
			impostorInfo += "[error: " + e + "]";
		}

		log.info("[EXAMINE] ObjectID: {}, ImpostorID: {}, SceneXY: ({}, {}), SceneBase: ({}, {}), Plane: {}, WorldPoint: {}, BoatWorldLoc: {}",
			objectId, impostorInfo, sceneX, sceneY, sceneBaseX, sceneBaseY, plane, worldPoint,
			boatWorldLocation != null ? boatWorldLocation : "null");

		if (scene != null)
		{
			var tiles = scene.getTiles();
			var tile = tiles[plane][sceneX][sceneY];

			for (GameObject gameObject : tile.getGameObjects()) {
				if (gameObject == null)
					continue;

				var id = gameObject.getId();
				if (id == objectId)
					continue;

				log.info("[EXAMINE] Found another Object on this tile: {}", id);
			}

			if (objectId == State.RUM_RETURN_BASE_OBJECT_ID || objectId == State.RUM_RETURN_IMPOSTOR_ID) {
				WorldPoint rumLocation = boatWorldLocation != null ? boatWorldLocation : worldPoint;
				routeCapture.onExamineRumDropoff(rumLocation, sceneX, sceneY, sceneBaseX, sceneBaseY, objectId, impostorInfo);
			}
			// else if (JubblyJiveConfig.TOAD_PILLAR_IDS.contains(objectId))
			// {
			// 	routeCapture.onExamineToadPillar(worldPoint, objectId);
			// }
		}
	}

	public boolean isPointInExclusionZone(WorldPoint point)
	{
		return locationManager.isPointInsideExclusionZone(point);
	}

	/**
	 * Gets the boat/entity world location for an object at the given scene coordinates.
	 * Objects on boats have scene-local coordinates that shift as the boat moves.
	 */
	private WorldPoint getBoatWorldLocationForSceneTile(int sceneX, int sceneY, int plane)
	{
		try
		{
			Scene scene = client.getScene();
			if (scene == null || sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
			{
				return null;
			}

			Tile[][][] tiles = scene.getTiles();
			if (tiles == null || tiles[plane] == null)
			{
				return null;
			}

			Tile tile = tiles[plane][sceneX][sceneY];
			if (tile == null)
			{
				return null;
			}

			GameObject rumObject = null;
			for (GameObject gameObject : tile.getGameObjects())
			{
				if (gameObject == null)
				{
					continue;
				}

				int objectId = gameObject.getId();
				if (objectId == State.RUM_RETURN_BASE_OBJECT_ID || objectId == State.RUM_RETURN_IMPOSTOR_ID ||
					objectId == State.RUM_PICKUP_BASE_OBJECT_ID || objectId == State.RUM_PICKUP_IMPOSTOR_ID)
				{
					rumObject = gameObject;
					break;
				}
			}

			if (rumObject == null)
			{
				return null;
			}

			WorldPoint rumWorldLocation = rumObject.getWorldLocation();

			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				return null;
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

				int entitySceneX = rumWorldLocation.getX() - entityScene.getBaseX();
				int entitySceneY = rumWorldLocation.getY() - entityScene.getBaseY();

				if (entitySceneX >= 0 && entitySceneX < 104 && entitySceneY >= 0 && entitySceneY < 104)
				{
					Tile[][][] entityTiles = entityScene.getTiles();
					if (entityTiles == null || plane >= entityTiles.length || entityTiles[plane] == null)
					{
						continue;
					}

					Tile entityTile = entityTiles[plane][entitySceneX][entitySceneY];
					if (entityTile != null)
					{
						boolean foundRumOnThisTile = false;
						for (GameObject gameObject : entityTile.getGameObjects())
						{
							if (gameObject == null)
							{
								continue;
							}

							int objectId = gameObject.getId();
							if (objectId == State.RUM_RETURN_BASE_OBJECT_ID || objectId == State.RUM_RETURN_IMPOSTOR_ID ||
								objectId == State.RUM_PICKUP_BASE_OBJECT_ID || objectId == State.RUM_PICKUP_IMPOSTOR_ID)
							{
								foundRumOnThisTile = true;
								break;
							}
						}

						if (foundRumOnThisTile)
						{
							var boatLocalLocation = worldEntity.getLocalLocation();
							if (boatLocalLocation != null)
							{
								return WorldPoint.fromLocalInstance(client, boatLocalLocation);
							}
						}
					}
				}
			}

			return null;
		}
		catch (Exception e)
		{
			log.debug("Error getting boat world location: {}", e.getMessage());
			return null;
		}
	}

	@Provides
	BarracudaTrialConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarracudaTrialConfig.class);
	}
}
