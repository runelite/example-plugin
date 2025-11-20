package com.barracudatrial;

import com.barracudatrial.game.*;
import com.google.inject.Provides;
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

	// State holder
	private final State state = new State();

	// Helper classes
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

		// Initialize helper classes
		sceneScanner = new SceneScanner(client);
		objectTracker = new ObjectTracker(client, state, sceneScanner);
		locationManager = new LocationManager(client, state, sceneScanner);
		progressTracker = new ProgressTracker(client, state);
		pathPlanner = new PathPlanner(client, state, config, locationManager);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Barracuda Trial plugin stopped!");
		overlayManager.remove(overlay);
		state.reset();
		state.clearPersistentStorage();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long tickStart = config.debugMode() ? System.currentTimeMillis() : 0;

		// Check trial area and update state
		progressTracker.checkTrialArea();
		objectTracker.updateBoatLocation();

		// Update cloud tracking
		long cloudStart = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateCloudTracking();
		if (config.debugMode())
		{
			state.setLastCloudUpdateTimeMs(System.currentTimeMillis() - cloudStart);
		}

		// Update rum locations
		locationManager.updateRumObjects();

		// Update rocks and speed boosts
		long rockStart = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateRocksAndSpeedBoosts();
		if (config.debugMode())
		{
			state.setLastRockUpdateTimeMs(System.currentTimeMillis() - rockStart);
		}

		// Update trial progress
		progressTracker.updateTrialProgress();

		// Update lost supplies
		long lostSuppliesStart = config.debugMode() ? System.currentTimeMillis() : 0;
		objectTracker.updateLostSupplies();
		if (config.debugMode())
		{
			state.setLastLostSuppliesUpdateTimeMs(System.currentTimeMillis() - lostSuppliesStart);
		}

		// Update all rocks for debug mode
		if (config.showIDs())
		{
			objectTracker.updateAllRocks();
		}

		// Recalculate path periodically to account for moving clouds
		if (state.isInTrialArea())
		{
			int ticksSince = state.getTicksSinceLastPathRecalc() + 1;
			state.setTicksSinceLastPathRecalc(ticksSince);

			if (ticksSince >= State.PATH_RECALC_INTERVAL)
			{
				state.setTicksSinceLastPathRecalc(0);
				pathPlanner.recalculateOptimalPath("periodic (game tick)");
			}
		}

		if (config.debugMode())
		{
			state.setLastTotalGameTickTimeMs(System.currentTimeMillis() - tickStart);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!state.isInTrialArea())
		{
			return;
		}

		String message = event.getMessage();

		if (message.startsWith("You collect the rum shipment"))
		{
			log.debug("Rum collected! Message: {}", message);
			state.setHasRumOnUs(true);
			pathPlanner.recalculateOptimalPath("chat: rum collected");
		}
		else if (message.startsWith("You deliver the rum shipment"))
		{
			log.debug("Rum delivered! Message: {}", message);
			state.setHasRumOnUs(false);

			int newLap = state.getCurrentLap() + 1;
			state.setCurrentLap(newLap);
			log.debug("Advanced to lap {}", newLap);

			state.getLostSuppliesForCurrentLap().clear();
			state.getLostSuppliesForFutureLaps().clear();

			pathPlanner.recalculateOptimalPath("chat: rum delivered");
		}
	}

	// Getter methods for overlay (delegate to state)
	public boolean isInTrialArea()
	{
		return state.isInTrialArea();
	}

	public java.util.Set<GameObject> getLostSupplies()
	{
		return state.getLostSupplies();
	}

	public java.util.Set<NPC> getLightningClouds()
	{
		return state.getLightningClouds();
	}

	public java.util.Set<GameObject> getRocks()
	{
		return state.getRocks();
	}

	public java.util.Set<GameObject> getSpeedBoosts()
	{
		return state.getSpeedBoosts();
	}

	public List<WorldPoint> getOptimalPath()
	{
		return state.getOptimalPath();
	}

	public WorldPoint getRumPickupLocation()
	{
		return state.getRumPickupLocation();
	}

	public WorldPoint getRumReturnLocation()
	{
		return state.getRumReturnLocation();
	}

	public WorldPoint getPathfindingPickupLocation()
	{
		return locationManager.getPathfindingPickupLocation();
	}

	public WorldPoint getPathfindingDropoffLocation()
	{
		return locationManager.getPathfindingDropoffLocation();
	}

	public int getRumsCollected()
	{
		return state.getRumsCollected();
	}

	public int getRumsNeeded()
	{
		return state.getRumsNeeded();
	}

	public int getCratesCollected()
	{
		return state.getCratesCollected();
	}

	public int getCratesTotal()
	{
		return state.getCratesTotal();
	}

	public int getCratesRemaining()
	{
		return state.getCratesTotal() - state.getCratesCollected();
	}

	public boolean isHasRumOnUs()
	{
		return state.isHasRumOnUs();
	}

	public WorldPoint getBoatLocation()
	{
		return state.getBoatLocation();
	}

	public int getCurrentLap()
	{
		return state.getCurrentLap();
	}

	public List<List<WorldPoint>> getPlannedLaps()
	{
		return state.getPlannedLaps();
	}

	public List<WorldPoint> getCurrentSegmentPath()
	{
		return state.getCurrentSegmentPath();
	}

	public List<WorldPoint> getNextSegmentPath()
	{
		return state.getNextSegmentPath();
	}

	public long getLastAStarTimeMs()
	{
		return state.getLastAStarTimeMs();
	}

	public long getLastLostSuppliesUpdateTimeMs()
	{
		return state.getLastLostSuppliesUpdateTimeMs();
	}

	public long getLastCloudUpdateTimeMs()
	{
		return state.getLastCloudUpdateTimeMs();
	}

	public long getLastPathPlanningTimeMs()
	{
		return state.getLastPathPlanningTimeMs();
	}

	public long getLastRockUpdateTimeMs()
	{
		return state.getLastRockUpdateTimeMs();
	}

	public long getLastTotalGameTickTimeMs()
	{
		return state.getLastTotalGameTickTimeMs();
	}

	public String getLastPathRecalcCaller()
	{
		return state.getLastPathRecalcCaller();
	}

	public java.util.Set<WorldPoint> getKnownRockLocations()
	{
		return state.getKnownRockLocations();
	}

	public java.util.Set<WorldPoint> getKnownSpeedBoostLocations()
	{
		return state.getKnownSpeedBoostLocations();
	}

	public java.util.Set<WorldPoint> getKnownLostSuppliesSpawnLocations()
	{
		return state.getKnownLostSuppliesSpawnLocations();
	}

	public int getExclusionZoneMinX()
	{
		return state.getExclusionZoneMinX();
	}

	public int getExclusionZoneMaxX()
	{
		return state.getExclusionZoneMaxX();
	}

	public int getExclusionZoneMinY()
	{
		return state.getExclusionZoneMinY();
	}

	public int getExclusionZoneMaxY()
	{
		return state.getExclusionZoneMaxY();
	}

	public boolean isPointInExclusionZone(WorldPoint point)
	{
		return locationManager.isInExclusionZone(point);
	}

	public List<GameObject> getAllRocksInScene()
	{
		return new java.util.ArrayList<>(state.getAllRocksInScene());
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
