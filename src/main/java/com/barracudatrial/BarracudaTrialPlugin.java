package com.barracudatrial;

import com.barracudatrial.game.*;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

	private SceneScanner sceneScanner;
	private ObjectTracker objectTracker;
	private LocationManager locationManager;
	private ProgressTracker progressTracker;
	private PathPlanner pathPlanner;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Barracuda Trial plugin started!");
		overlayManager.add(overlay);

		sceneScanner = new SceneScanner(client);
		objectTracker = new ObjectTracker(client, gameState, sceneScanner);
		locationManager = new LocationManager(client, gameState, sceneScanner);
		progressTracker = new ProgressTracker(client, gameState);
		pathPlanner = new PathPlanner(client, gameState, config, locationManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Barracuda Trial plugin stopped!");
		overlayManager.remove(overlay);
		gameState.resetAllTemporaryState();
		gameState.clearPersistentStorage();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long gameTickStartTimeMs = config.debugMode() ? System.currentTimeMillis() : 0;

		progressTracker.checkIfPlayerIsInTrialArea();
		objectTracker.updatePlayerBoatLocation();

		long cloudUpdateStartTimeMs = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateLightningCloudTracking();
		if (config.debugMode())
		{
			gameState.setLastCloudUpdateTimeMs(System.currentTimeMillis() - cloudUpdateStartTimeMs);
		}

		locationManager.updateRumLocations();

		long rockUpdateStartTimeMs = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateRocksAndSpeedBoosts();
		if (config.debugMode())
		{
			gameState.setLastRockUpdateTimeMs(System.currentTimeMillis() - rockUpdateStartTimeMs);
		}

		progressTracker.updateTrialProgressFromWidgets();

		long lostSuppliesUpdateStartTimeMs = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateLostSuppliesTracking();
		if (config.debugMode())
		{
			gameState.setLastLostSuppliesUpdateTimeMs(System.currentTimeMillis() - lostSuppliesUpdateStartTimeMs);
		}

		if (config.showIDs())
		{
			objectTracker.updateAllRocksInScene();
		}

		// Recalculate path periodically to account for moving clouds
		if (gameState.isInTrialArea())
		{
			int ticksSinceLastPathRecalculation = gameState.getTicksSinceLastPathRecalc() + 1;
			gameState.setTicksSinceLastPathRecalc(ticksSinceLastPathRecalculation);

			if (ticksSinceLastPathRecalculation >= State.PATH_RECALC_INTERVAL)
			{
				gameState.setTicksSinceLastPathRecalc(0);
				pathPlanner.recalculateOptimalPathFromCurrentState("periodic (game tick)");
			}
		}

		if (config.debugMode())
		{
			gameState.setLastTotalGameTickTimeMs(System.currentTimeMillis() - gameTickStartTimeMs);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!gameState.isInTrialArea())
		{
			return;
		}

		String chatMessage = event.getMessage();

		if (chatMessage.startsWith("You collect the rum shipment"))
		{
			log.debug("Rum collected! Message: {}", chatMessage);
			gameState.setHasRumOnUs(true);
			pathPlanner.recalculateOptimalPathFromCurrentState("chat: rum collected");
		}
		else if (chatMessage.startsWith("You deliver the rum shipment"))
		{
			log.debug("Rum delivered! Message: {}", chatMessage);
			gameState.setHasRumOnUs(false);

			int nextLapNumber = gameState.getCurrentLap() + 1;
			gameState.setCurrentLap(nextLapNumber);
			log.debug("Advanced to lap {}", nextLapNumber);

			gameState.getLostSuppliesForCurrentLap().clear();
			gameState.getLostSuppliesForFutureLaps().clear();

			pathPlanner.recalculateOptimalPathFromCurrentState("chat: rum delivered");
		}
	}

	public WorldPoint getPathfindingPickupLocation()
	{
		return locationManager.getPathfindingPickupLocation();
	}

	public WorldPoint getPathfindingDropoffLocation()
	{
		return locationManager.getPathfindingDropoffLocation();
	}

	public int getCratesRemaining()
	{
		return gameState.getCratesTotal() - gameState.getCratesCollected();
	}

	public boolean isPointInExclusionZone(WorldPoint point)
	{
		return locationManager.isPointInsideExclusionZone(point);
	}

	public List<GameObject> getAllRocksInScene()
	{
		return new java.util.ArrayList<>(gameState.getAllRocksInScene());
	}

	public boolean isCloudSafe(int animationId)
	{
		return animationId == State.CLOUD_ANIM_HARMLESS || animationId == State.CLOUD_ANIM_HARMLESS_ALT;
	}

	@Provides
	BarracudaTrialConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarracudaTrialConfig.class);
	}
}
