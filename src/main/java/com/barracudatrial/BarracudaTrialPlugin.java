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

		cachedConfig = new CachedConfig(config);

		sceneScanner = new SceneScanner(client);
		objectTracker = new ObjectTracker(client, gameState, sceneScanner);
		locationManager = new LocationManager(client, gameState, sceneScanner);
		progressTracker = new ProgressTracker(client, gameState);
		pathPlanner = new PathPlanner(client, gameState, cachedConfig, locationManager);
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
		long gameTickStartTimeMs = cachedConfig.isDebugMode() ? System.currentTimeMillis() : 0;

		progressTracker.checkIfPlayerIsInTrialArea();
		objectTracker.updatePlayerBoatLocation();

		long cloudUpdateStartTimeMs = cachedConfig.isDebugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateLightningCloudTracking();
		if (cachedConfig.isDebugMode())
		{
			gameState.setLastCloudUpdateTimeMs(System.currentTimeMillis() - cloudUpdateStartTimeMs);
		}

		locationManager.updateRumLocations();

		long rockUpdateStartTimeMs = cachedConfig.isDebugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateRocksAndSpeedBoosts();
		if (cachedConfig.isDebugMode())
		{
			gameState.setLastRockUpdateTimeMs(System.currentTimeMillis() - rockUpdateStartTimeMs);
		}

		progressTracker.updateTrialProgressFromWidgets();

		long lostSuppliesUpdateStartTimeMs = cachedConfig.isDebugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateLostSuppliesTracking();
		if (cachedConfig.isDebugMode())
		{
			gameState.setLastLostSuppliesUpdateTimeMs(System.currentTimeMillis() - lostSuppliesUpdateStartTimeMs);
		}

		if (cachedConfig.isShowIDs())
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

		if (cachedConfig.isDebugMode())
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

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("barracudatrial"))
		{
			return;
		}

		cachedConfig.updateCache();

		// Recalculate path when starting direction config changes
		if (event.getKey().equals("startingDirection") && gameState.isInTrialArea())
		{
			pathPlanner.recalculateOptimalPathFromCurrentState("config: starting direction changed");
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

	public int getLostSuppliesRemaining()
	{
		return gameState.getlostSuppliesTotal() - gameState.getLostSuppliesCollected();
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
