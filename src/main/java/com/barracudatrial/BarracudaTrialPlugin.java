package com.barracudatrial;

import com.barracudatrial.pathfinding.AStarPathfinder;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import com.barracudatrial.pathfinding.MultiLapOptimizer;
import com.barracudatrial.pathfinding.StrategicTileCostCalculator;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;

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
	private boolean inTrialArea = false;

	@Getter
	private final Set<GameObject> lostSupplies = new HashSet<>();

	@Getter
	private final Set<NPC> lightningClouds = new HashSet<>();

	@Getter
	private final Set<GameObject> rocks = new HashSet<>();

	@Getter
	private final Set<GameObject> speedBoosts = new HashSet<>();

	@Getter
	private List<WorldPoint> optimalPath = new ArrayList<>();

	@Getter
	private WorldPoint rumPickupLocation = null;

	@Getter
	private WorldPoint rumReturnLocation = null;

	// The actual pickup is impassable, so we target 2 tiles away
	public WorldPoint getPathfindingPickupLocation()
	{
		if (rumPickupLocation == null)
		{
			return null;
		}
		return new WorldPoint(
			rumPickupLocation.getX(),
			rumPickupLocation.getY() + 2,
			rumPickupLocation.getPlane()
		);
	}

	// The actual dropoff is impassable, so we target 2 tiles away
	public WorldPoint getPathfindingDropoffLocation()
	{
		if (rumReturnLocation == null)
		{
			return null;
		}
		return new WorldPoint(
			rumReturnLocation.getX(),
			rumReturnLocation.getY() - 2,
			rumReturnLocation.getPlane()
		);
	}

	@Getter
	private int rumsCollected = 0;

	@Getter
	private int rumsNeeded = 0;

	@Getter
	private int cratesCollected = 0;

	@Getter
	private int cratesTotal = 0;

	private int lastRumCount = 0;

	@Getter
	private boolean hasRumOnUs = false;

	@Getter
	private WorldPoint boatLocation = null;

	@Getter
	private int currentLap = 0;

	@Getter
	private List<List<WorldPoint>> plannedLaps = new ArrayList<>();

	@Getter
	private List<WorldPoint> currentSegmentPath = new ArrayList<>();

	@Getter
	private List<WorldPoint> nextSegmentPath = new ArrayList<>();

	private Set<GameObject> remainingLostSupplies = new HashSet<>();

	private Set<GameObject> lostSuppliesForCurrentLap = new HashSet<>();
	private Set<GameObject> lostSuppliesForFutureLaps = new HashSet<>();

	private final Set<GameObject> allRocksInScene = new HashSet<>();

	@Getter
	private long lastAStarTimeMs = 0;
	@Getter
	private long lastLostSuppliesUpdateTimeMs = 0;
	@Getter
	private long lastCloudUpdateTimeMs = 0;
	@Getter
	private long lastPathPlanningTimeMs = 0;
	@Getter
	private long lastRockUpdateTimeMs = 0;
	@Getter
	private long lastTotalGameTickTimeMs = 0;
	@Getter
	private String lastPathRecalcCaller = "none";

	// Rocks never move
	@Getter
	private final Set<WorldPoint> knownRockLocations = new HashSet<>();

	// Speed boosts never move
	@Getter
	private final Set<WorldPoint> knownSpeedBoostLocations = new HashSet<>();

	@Getter
	private final Set<WorldPoint> knownLostSuppliesSpawnLocations = new HashSet<>();

	private int lastKnownDifficulty = 0;

	private int ticksSinceLastPathRecalc = 0;
	private static final int PATH_RECALC_INTERVAL = 2;

	private static final int BARRACUDA_TRIALS_HUD = 931;
	private static final int WIDGET_BT_HUD = 3;
	private static final int WIDGET_RUM_PROGRESS = 24;
	private static final int WIDGET_CRATE_PROGRESS = 25;

	private static final int LIGHTNING_CLOUD_IDLE = 15490;
	private static final int LIGHTNING_CLOUD_ATTACKING = 15491;

	private static final int CLOUD_ANIM_HARMLESS = -1;
	// Alternative safe animation
	private static final int CLOUD_ANIM_HARMLESS_ALT = 8879;
	private static final int CLOUD_ANIM_DANGEROUS = 8877;
	private static final int CLOUD_ANIM_ATTACKING = 13141;

	// Can pick up from 2 tiles away
	private static final int LOST_SUPPLIES_PICKUP_RANGE = 2;

	private static final int RUM_RETURN_BASE_OBJECT_ID = 59237;
	private static final int RUM_RETURN_IMPOSTOR_ID = 59239;
	private static final int RUM_PICKUP_BASE_OBJECT_ID = 59240;
	private static final int RUM_PICKUP_IMPOSTOR_ID = 59242;

	// These offsets are constant across all trials
	private static final int PICKUP_OFFSET_X = 24;
	private static final int PICKUP_OFFSET_Y = -128;

	// The exclusion zone is the center island area we circle around
	private static final int EXCLUSION_MIN_X_OFFSET = 47;
	private static final int EXCLUSION_MAX_X_OFFSET = 102;
	private static final int EXCLUSION_MIN_Y_OFFSET = -51;
	private static final int EXCLUSION_MAX_Y_OFFSET = 12;

	private int exclusionZoneMinX = 0;
	private int exclusionZoneMaxX = 0;
	private int exclusionZoneMinY = 0;
	private int exclusionZoneMaxY = 0;
	public int getExclusionZoneMinX() { return exclusionZoneMinX; }
	public int getExclusionZoneMaxX() { return exclusionZoneMaxX; }
	public int getExclusionZoneMinY() { return exclusionZoneMinY; }
	public int getExclusionZoneMaxY() { return exclusionZoneMaxY; }
	public boolean isPointInExclusionZone(WorldPoint point) { return isInExclusionZone(point); }

	public List<GameObject> getAllRocksInScene()
	{
		List<GameObject> allObjects = new ArrayList<>();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return allObjects;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return allObjects;
		}

		// Check regular tiles
		Tile[][][] tiles = scene.getTiles();
		if (tiles != null)
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
							if (gameObject != null)
							{
								int id = gameObject.getId();
								if (!LOST_SUPPLIES_BASE_IDS.contains(id) && id != LOST_SUPPLIES_IMPOSTOR_ID && !SPEED_BOOST_IDS.contains(id))
								{
									allObjects.add(gameObject);
								}
							}
						}
					}
				}
			}
		}

		// Check extended tiles
		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			for (int plane = 0; plane < extendedTiles.length; plane++)
			{
				if (extendedTiles[plane] == null)
				{
					continue;
				}

				for (int x = 0; x < extendedTiles[plane].length; x++)
				{
					if (extendedTiles[plane][x] == null)
					{
						continue;
					}

					for (int y = 0; y < extendedTiles[plane][x].length; y++)
					{
						Tile tile = extendedTiles[plane][x][y];
						if (tile == null)
						{
							continue;
						}

						for (GameObject gameObject : tile.getGameObjects())
						{
							if (gameObject != null)
							{
								int id = gameObject.getId();
								if (!LOST_SUPPLIES_BASE_IDS.contains(id) && id != LOST_SUPPLIES_IMPOSTOR_ID && !SPEED_BOOST_IDS.contains(id))
								{
									allObjects.add(gameObject);
								}
							}
						}
					}
				}
			}
		}

		return allObjects;
	}

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

	@Override
	protected void startUp() throws Exception
	{
		log.info("Barracuda Trial plugin started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Barracuda Trial plugin stopped!");
		overlayManager.remove(overlay);
		reset();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		long tickStart = config.debugMode() ? System.currentTimeMillis() : 0;

		checkTrialArea();
		updateBoatLocation();

		long cloudStart = config.debugMode() ? System.currentTimeMillis() : 0;
		updateCloudTracking();
		if (config.debugMode())
		{
			lastCloudUpdateTimeMs = System.currentTimeMillis() - cloudStart;
		}

		updateRumObjects();

		long rockStart = config.debugMode() ? System.currentTimeMillis() : 0;
		updateRocksAndSpeedBoosts();
		if (config.debugMode())
		{
			lastRockUpdateTimeMs = System.currentTimeMillis() - rockStart;
		}

		updateTrialProgress();

		long lostSuppliesStart = config.debugMode() ? System.currentTimeMillis() : 0;
		updateLostSupplies();
		if (config.debugMode())
		{
			lastLostSuppliesUpdateTimeMs = System.currentTimeMillis() - lostSuppliesStart;
		}

		if (config.showIDs())
		{
			updateAllRocks();
		}

		// Recalculate path periodically to account for moving clouds
		if (inTrialArea)
		{
			ticksSinceLastPathRecalc++;
			if (ticksSinceLastPathRecalc >= PATH_RECALC_INTERVAL)
			{
				ticksSinceLastPathRecalc = 0;
				recalculateOptimalPath("periodic (game tick)");
			}
		}

		if (config.debugMode())
		{
			lastTotalGameTickTimeMs = System.currentTimeMillis() - tickStart;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!inTrialArea)
		{
			return;
		}

		String message = event.getMessage();

		if (message.startsWith("You collect the rum shipment"))
		{
			log.debug("Rum collected! Message: {}", message);
			hasRumOnUs = true;
			recalculateOptimalPath("chat: rum collected");
		}
		else if (message.startsWith("You deliver the rum shipment"))
		{
			log.debug("Rum delivered! Message: {}", message);
			hasRumOnUs = false;

			currentLap++;
			log.debug("Advanced to lap {}", currentLap);

			lostSuppliesForCurrentLap.clear();
			lostSuppliesForFutureLaps.clear();

			recalculateOptimalPath("chat: rum delivered");
		}
	}

	private void checkTrialArea()
	{
		// Check if Barracuda Trial HUD widget is visible
		Widget hudWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_BT_HUD);

		boolean wasInTrialArea = inTrialArea;
		inTrialArea = hudWidget != null && !hudWidget.isHidden();

		if (!wasInTrialArea && inTrialArea)
		{
			log.debug("Entered Barracuda Trial");
			initializeTrialLocations();
		}
		else if (wasInTrialArea && !inTrialArea)
		{
			log.debug("Left Barracuda Trial");
			reset();
		}
	}

	private void initializeTrialLocations()
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

	private void updateRumLocations(WorldPoint rumReturnLoc)
	{
		// Update rum return location
		rumReturnLocation = rumReturnLoc;

		// Calculate rum pickup location relative to return
		rumPickupLocation = new WorldPoint(
			rumReturnLoc.getX() + PICKUP_OFFSET_X,
			rumReturnLoc.getY() + PICKUP_OFFSET_Y,
			rumReturnLoc.getPlane()
		);

		// Calculate exclusion zone boundaries relative to return location
		exclusionZoneMinX = rumReturnLoc.getX() + EXCLUSION_MIN_X_OFFSET;
		exclusionZoneMaxX = rumReturnLoc.getX() + EXCLUSION_MAX_X_OFFSET;
		exclusionZoneMinY = rumReturnLoc.getY() + EXCLUSION_MIN_Y_OFFSET;
		exclusionZoneMaxY = rumReturnLoc.getY() + EXCLUSION_MAX_Y_OFFSET;

		log.debug("Updated rum locations - Return: {}, Pickup: {}", rumReturnLocation, rumPickupLocation);
		log.debug("Exclusion zone: ({}, {}) to ({}, {})",
			exclusionZoneMinX, exclusionZoneMinY, exclusionZoneMaxX, exclusionZoneMaxY);
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
							if (rumReturnLocation == null || !rumReturnLocation.equals(newLocation))
							{
								rumReturnLocation = newLocation;
								log.info("Found rum return location: {}", newLocation);
								// Calculate exclusion zone from return location
								updateExclusionZone(newLocation);
							}
						}
						else if (isPickup)
						{
							WorldPoint newLocation = gameObject.getWorldLocation();
							if (rumPickupLocation == null || !rumPickupLocation.equals(newLocation))
							{
								rumPickupLocation = newLocation;
								log.info("Found rum pickup location: {}", newLocation);
							}
						}
					}
				}
			}
		}
	}

	private void updateExclusionZone(WorldPoint rumReturnLoc)
	{
		// Exclusion zone is the area between rum locations (impassable water/island)
		// Calculate based on rum return location (can adjust as needed)
		int centerX = rumReturnLoc.getX();
		int centerY = rumReturnLoc.getY() - 64; // Midpoint between north and south

		exclusionZoneMinX = centerX - 8;
		exclusionZoneMaxX = centerX + 8;
		exclusionZoneMinY = centerY - 8;
		exclusionZoneMaxY = centerY + 8;

		log.debug("Updated exclusion zone from rum return: ({}, {}) to ({}, {})",
			exclusionZoneMinX, exclusionZoneMinY, exclusionZoneMaxX, exclusionZoneMaxY);
	}

	private void updateTrialProgress()
	{
		if (!inTrialArea)
		{
			return;
		}

		// Parse rum progress widget (format: "0 / 1")
		Widget rumWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_RUM_PROGRESS);
		if (rumWidget != null && !rumWidget.isHidden())
		{
			String rumText = rumWidget.getText();
			parseRumProgress(rumText);
		}

		// Parse crate progress widget
		Widget crateWidget = client.getWidget(BARRACUDA_TRIALS_HUD, WIDGET_CRATE_PROGRESS);
		if (crateWidget != null && !crateWidget.isHidden())
		{
			String crateText = crateWidget.getText();
			parseCrateProgress(crateText);
		}

		// Detect difficulty change - clear persistent storage if rumsNeeded changes
		if (lastKnownDifficulty > 0 && rumsNeeded > 0 && rumsNeeded != lastKnownDifficulty)
		{
			log.info("Difficulty changed from {} to {} rums - clearing persistent storage",
				lastKnownDifficulty, rumsNeeded);
			clearPersistentStorage();
			currentLap = 0; // Reset lap counter
		}
		lastKnownDifficulty = rumsNeeded;

		// Detect when rum count increases - need to return rum to north
		if (rumsCollected > lastRumCount)
		{
			log.debug("Rum collected! Need to return to north");
			recalculateOptimalPath("widget: rum count increased");
		}
		lastRumCount = rumsCollected;
	}

	private void parseRumProgress(String text)
	{
		// Format: "0 / 1"
		try
		{
			String[] parts = text.split("/");
			if (parts.length == 2)
			{
				rumsCollected = Integer.parseInt(parts[0].trim());
				rumsNeeded = Integer.parseInt(parts[1].trim());
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse rum progress: {}", text);
		}
	}

	private void parseCrateProgress(String text)
	{
		// Format: "2 / 14" (collected / total)
		try
		{
			String[] parts = text.split("/");
			if (parts.length == 2)
			{
				cratesCollected = Integer.parseInt(parts[0].trim());
				cratesTotal = Integer.parseInt(parts[1].trim());
			}
		}
		catch (NumberFormatException e)
		{
			log.debug("Failed to parse crate progress: {}", text);
		}
	}

	/**
	 * Returns the number of crates still remaining to collect
	 */
	public int getCratesRemaining()
	{
		return cratesTotal - cratesCollected;
	}

	private boolean isInTrialRegion(WorldPoint point)
	{
		// TODO: Add proper region detection for all three trial locations
		// This is a placeholder implementation

		// Example region IDs - update with actual region IDs:
		// Tempor Tantrum (Storm Tempor)
		// Jubbly Jive (Backwater)
		// Gwenith Glide (Porth Gwenith)

		int regionId = point.getRegionID();

		// Placeholder region IDs - replace with actual values
		Set<Integer> trialRegions = Set.of(
			// Add actual region IDs here
		);

		return trialRegions.contains(regionId);
	}

	/**
	 * Checks if a point is inside the center exclusion zone
	 * We want to path AROUND this area, never through it
	 */
	private boolean isInExclusionZone(WorldPoint point)
	{
		return point.getX() >= exclusionZoneMinX
			&& point.getX() <= exclusionZoneMaxX
			&& point.getY() >= exclusionZoneMinY
			&& point.getY() <= exclusionZoneMaxY;
	}

	/**
	 * Calculates the distance from a point to the nearest edge of the exclusion zone
	 * Returns 0 if the point is inside the zone
	 */
	private double getDistanceToExclusionZone(WorldPoint point)
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
		if (x < exclusionZoneMinX)
		{
			distanceX = exclusionZoneMinX - x;
		}
		else if (x > exclusionZoneMaxX)
		{
			distanceX = x - exclusionZoneMaxX;
		}
		else
		{
			distanceX = 0; // X is within the zone's X range
		}

		double distanceY;
		if (y < exclusionZoneMinY)
		{
			distanceY = exclusionZoneMinY - y;
		}
		else if (y > exclusionZoneMaxY)
		{
			distanceY = y - exclusionZoneMaxY;
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

	private void updateBoatLocation()
	{
		if (!inTrialArea)
		{
			boatLocation = null;
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			boatLocation = null;
			return;
		}

		try
		{
			// Get the player's WorldView ID (the boat they're in)
			WorldView playerWorldView = localPlayer.getWorldView();
			if (playerWorldView == null)
			{
				// Fallback to player location if not in a boat
				boatLocation = localPlayer.getWorldLocation();
				return;
			}

			int worldViewId = playerWorldView.getId();

			// Get the boat's WorldEntity from the top-level world view
			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				boatLocation = localPlayer.getWorldLocation();
				return;
			}

			WorldEntity worldEntity = topLevelWorldView.worldEntities().byIndex(worldViewId);
			if (worldEntity == null)
			{
				// Fallback to player location
				boatLocation = localPlayer.getWorldLocation();
				return;
			}

			// Convert the boat's LocalLocation to WorldPoint
			var localLocation = worldEntity.getLocalLocation();
			if (localLocation != null)
			{
				boatLocation = WorldPoint.fromLocalInstance(client, localLocation);
			}
			else
			{
				boatLocation = localPlayer.getWorldLocation();
			}
		}
		catch (Exception e)
		{
			// Fallback to player location on any error
			boatLocation = localPlayer.getWorldLocation();
			log.debug("Error getting boat location: {}", e.getMessage());
		}
	}

	private void updateCloudTracking()
	{
		if (!inTrialArea)
		{
			lightningClouds.clear();
			return;
		}

		// Actively scan for lightning cloud NPCs instead of relying on spawn/despawn events
		// Spawn/despawn events are unreliable for some objects
		lightningClouds.clear();

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
				lightningClouds.add(npc);
			}
		}
	}

	private void updateRumObjects()
	{
		if (!inTrialArea)
		{
			return;
		}

		// Search for both rum locations in WorldEntity scenes
		updateRumLocationsBySearch();
	}

	private void updateRocksAndSpeedBoosts()
	{
		if (!inTrialArea)
		{
			rocks.clear();
			speedBoosts.clear();
			return;
		}

		rocks.clear();
		speedBoosts.clear();

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

	private void scanSceneForRocksAndBoosts(Scene scene)
	{
		// Check regular tiles
		Tile[][][] tiles = scene.getTiles();
		if (tiles != null)
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
								rocks.add(gameObject);
								knownRockLocations.add(gameObject.getWorldLocation());
							}
							else if (SPEED_BOOST_IDS.contains(id))
							{
								speedBoosts.add(gameObject);
								knownSpeedBoostLocations.add(gameObject.getWorldLocation());
							}
						}
					}
				}
			}
		}

		// Check extended tiles
		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			for (int plane = 0; plane < extendedTiles.length; plane++)
			{
				if (extendedTiles[plane] == null)
				{
					continue;
				}

				for (int x = 0; x < extendedTiles[plane].length; x++)
				{
					if (extendedTiles[plane][x] == null)
					{
						continue;
					}

					for (int y = 0; y < extendedTiles[plane][x].length; y++)
					{
						Tile tile = extendedTiles[plane][x][y];
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
								rocks.add(gameObject);
								knownRockLocations.add(gameObject.getWorldLocation());
							}
							else if (SPEED_BOOST_IDS.contains(id))
							{
								speedBoosts.add(gameObject);
								knownSpeedBoostLocations.add(gameObject.getWorldLocation());
							}
						}
					}
				}
			}
		}
	}

	private void updateAllRocks()
	{
		if (!inTrialArea)
		{
			allRocksInScene.clear();
			return;
		}

		// Scan entire scene for all GameObjects that might be rocks
		allRocksInScene.clear();

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
							allRocksInScene.add(obj);
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
						if (id == RUM_RETURN_BASE_OBJECT_ID || id == RUM_RETURN_IMPOSTOR_ID ||
							id == RUM_PICKUP_BASE_OBJECT_ID || id == RUM_PICKUP_IMPOSTOR_ID)
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
									allRocksInScene.add(obj);
								}
							}
						}
					}
				}
			}
		}
	}

	private void updateLostSupplies()
	{
		if (!inTrialArea)
		{
			return;
		}

		Set<GameObject> newLostSupplies = new HashSet<>();

		Scene scene = client.getScene();
		if (scene == null)
		{
			return;
		}

		scanSceneForLostSupplies(scene, newLostSupplies);

		if (!lostSupplies.equals(newLostSupplies))
		{
			lostSupplies.clear();
			lostSupplies.addAll(newLostSupplies);
			updateLostSuppliesAssignments(newLostSupplies);
			recalculateOptimalPath("lost supplies changed");
		}
	}

	private void updateLostSuppliesAssignments(Set<GameObject> newLostSupplies)
	{
		// Remove supplies that no longer exist from our tracking sets
		lostSuppliesForCurrentLap.retainAll(newLostSupplies);
		lostSuppliesForFutureLaps.retainAll(newLostSupplies);
	}

	private void scanSceneForLostSupplies(Scene scene, Set<GameObject> newLostSupplies)
	{
		// Check regular tiles
		Tile[][][] tiles = scene.getTiles();
		if (tiles != null)
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

		// Check extended tiles
		Tile[][][] extendedTiles = scene.getExtendedTiles();
		if (extendedTiles != null)
		{
			for (int plane = 0; plane < extendedTiles.length; plane++)
			{
				if (extendedTiles[plane] == null)
				{
					continue;
				}

				for (int x = 0; x < extendedTiles[plane].length; x++)
				{
					if (extendedTiles[plane][x] == null)
					{
						continue;
					}

					for (int y = 0; y < extendedTiles[plane][x].length; y++)
					{
						Tile tile = extendedTiles[plane][x][y];
						if (tile == null)
						{
							continue;
						}

						processLostSupplyTile(tile, newLostSupplies);
					}
				}
			}
		}
	}

	private void processLostSupplyTile(Tile tile, Set<GameObject> newLostSupplies)
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

			boolean isNew = knownLostSuppliesSpawnLocations.add(supplyLocation);
			if (isNew)
			{
				log.debug("Discovered supply spawn location at {}, total known: {}",
					supplyLocation, knownLostSuppliesSpawnLocations.size());
			}

			if (isLostSupplyCollectible(obj))
			{
				newLostSupplies.add(obj);
			}
		}
	}

	private boolean isLostSupplyCollectible(GameObject obj)
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

	private void recalculateOptimalPath(String caller)
	{
		lastPathRecalcCaller = caller;
		log.debug("Path recalculation triggered by: {}", caller);

		if (!inTrialArea)
		{
			optimalPath.clear();
			currentSegmentPath.clear();
			nextSegmentPath.clear();
			plannedLaps.clear();
			return;
		}

		// Reset tick counter when manually recalculating
		ticksSinceLastPathRecalc = 0;

		// Use boat location as the start point (not player location!)
		WorldPoint startPoint = boatLocation;
		if (startPoint == null)
		{
			return;
		}

		// Plan optimal laps for collecting lost supplies (handles all cases including no supplies)
		long planStart = config.debugMode() ? System.currentTimeMillis() : 0;
		planOptimalLaps();
		if (config.debugMode())
		{
			lastPathPlanningTimeMs = System.currentTimeMillis() - planStart;
		}

		// Special case: If no supplies AND we have rum, go straight to dropoff
		if (lostSupplies.isEmpty() && hasRumOnUs)
		{
			currentSegmentPath = new ArrayList<>();
			if (rumReturnLocation != null)
			{
				currentSegmentPath.add(getPathfindingDropoffLocation());
			}
			nextSegmentPath.clear();
			optimalPath = new ArrayList<>(currentSegmentPath);
			log.debug("No supplies and have rum, pathing to dropoff");
			return;
		}

		// Set current and next segment paths
		if (currentLap < plannedLaps.size())
		{
			// Optimizer returns complete route: Lost Supplies → Pickup → Lost Supplies
			// with pickup inserted at optimal position
			List<WorldPoint> waypoints = new ArrayList<>(plannedLaps.get(currentLap));

			// Add rum dropoff at end (mandatory lap endpoint)
			// Target 2 tiles south of dropoff (actual dropoff is impassable)
			if (rumReturnLocation != null)
			{
				waypoints.add(getPathfindingDropoffLocation());
			}

			// Expand waypoints into full A* path (tactical navigation with speed boosts)
			long astarStart = config.debugMode() ? System.currentTimeMillis() : 0;
			currentSegmentPath = expandWaypointsWithAStar(boatLocation, waypoints);
			if (config.debugMode())
			{
				lastAStarTimeMs = System.currentTimeMillis() - astarStart;
			}
		}
		else
		{
			currentSegmentPath.clear();
		}

		// Show next segment preview (if more laps needed)
		if (currentLap + 1 < plannedLaps.size())
		{
			// Optimizer returns complete route: Lost Supplies → Pickup → Lost Supplies
			// with pickup inserted at optimal position
			List<WorldPoint> nextWaypoints = new ArrayList<>(plannedLaps.get(currentLap + 1));

			// Add rum dropoff at end (mandatory lap endpoint)
			// Target 2 tiles south of dropoff (actual dropoff is impassable)
			if (rumReturnLocation != null)
			{
				nextWaypoints.add(getPathfindingDropoffLocation());
			}

			// Expand waypoints into full A* path
			// Start from pathfinding dropoff location (2 tiles south of actual)
			WorldPoint nextStart = rumReturnLocation != null ? getPathfindingDropoffLocation() : boatLocation;
			nextSegmentPath = expandWaypointsWithAStar(nextStart, nextWaypoints);
		}
		else
		{
			nextSegmentPath.clear();
		}

		// Update optimalPath for backward compatibility
		optimalPath = new ArrayList<>(currentSegmentPath);

		log.debug("Planned {} laps, currently on lap {}/{}", plannedLaps.size(), currentLap + 1, rumsNeeded);
	}

	/**
	 * Expands waypoints into full A* paths using tactical pathfinding
	 * Only expands the next 3 waypoints for performance (user only needs immediate path)
	 * Uses BarracudaTileCostCalculator to route through speed boosts and avoid clouds/rocks
	 */
	private List<WorldPoint> expandWaypointsWithAStar(WorldPoint start, List<WorldPoint> waypoints)
	{
		if (waypoints.isEmpty())
		{
			return new ArrayList<>();
		}

		// Filter out safe clouds (only pass dangerous ones to cost calculator)
		Set<NPC> dangerousClouds = new HashSet<>();
		for (NPC cloud : lightningClouds)
		{
			if (!isCloudSafe(cloud.getAnimation()))
			{
				dangerousClouds.add(cloud);
			}
		}

		// Create tactical cost calculator (includes clouds, speed boosts, rocks, exclusion zone)
		BarracudaTileCostCalculator costCalculator = new BarracudaTileCostCalculator(
			knownSpeedBoostLocations,
			knownRockLocations,
			rocks,
			dangerousClouds,
			exclusionZoneMinX,
			exclusionZoneMaxX,
			exclusionZoneMinY,
			exclusionZoneMaxY
		);

		// Create A* pathfinder
		AStarPathfinder pathfinder = new AStarPathfinder(costCalculator);

		List<WorldPoint> fullPath = new ArrayList<>();
		WorldPoint current = start;
		int maxSearchDistance = 100;

		// Only expand next N waypoints with A* (configurable for performance and visual clarity)
		// Remaining waypoints will be expanded when closer on future recalcs
		int waypointsToExpand = Math.min(config.pathLookahead(), waypoints.size());

		// Expand first N waypoints with A*
		for (int i = 0; i < waypointsToExpand; i++)
		{
			WorldPoint waypoint = waypoints.get(i);
			List<WorldPoint> segment = pathfinder.findPath(current, waypoint, maxSearchDistance);

			if (segment.isEmpty())
			{
				// No path found, fallback to direct waypoint
				fullPath.add(waypoint);
			}
			else
			{
				// Add all tiles in this segment (except first, which is current position)
				for (int j = 1; j < segment.size(); j++)
				{
					fullPath.add(segment.get(j));
				}
			}

			current = waypoint;
		}

		// Do NOT add remaining waypoints - they will be calculated when closer
		// This reduces both computation time and visual clutter

		return fullPath;
	}

	/**
	 * Plans optimal laps for collecting all lost supplies
	 * Uses multi-lap optimization to minimize TOTAL time, not just first lap
	 */
	private void planOptimalLaps()
	{
		plannedLaps.clear();

		if (boatLocation == null || lostSupplies.isEmpty())
		{
			return;
		}

		// Calculate how many laps we need
		int lapsNeeded = Math.max(1, rumsNeeded - currentLap);

		// If we already have supply assignments, only replan current lap to avoid going backwards
		if (!lostSuppliesForCurrentLap.isEmpty() || !lostSuppliesForFutureLaps.isEmpty())
		{
			// Just replan the current lap with its assigned supplies
			replanCurrentLapOnly();
			return;
		}

		// Initial planning: use multi-lap optimizer for GLOBAL optimization
		// This ensures we minimize TOTAL time, not just first lap time
		lostSuppliesForCurrentLap.clear();
		lostSuppliesForFutureLaps.clear();

		// Create multi-lap optimizer (uses straight-line distance with rock and exclusion zone penalties)
		boolean preferWestStart = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer optimizer = new MultiLapOptimizer(
			knownRockLocations,
			preferWestStart,
			exclusionZoneMinX,
			exclusionZoneMaxX,
			exclusionZoneMinY,
			exclusionZoneMaxY
		);

		// Use fallback locations if we haven't seen the rum objects yet
		WorldPoint pickupLoc = rumPickupLocation;
		WorldPoint returnLoc = rumReturnLocation;

		// Calculate missing locations using known offsets
		if (returnLoc == null && pickupLoc != null)
		{
			// Calculate return from pickup (reverse offset)
			returnLoc = new WorldPoint(
				pickupLoc.getX() - PICKUP_OFFSET_X,
				pickupLoc.getY() - PICKUP_OFFSET_Y,
				pickupLoc.getPlane()
			);
		}
		else if (pickupLoc == null && returnLoc != null)
		{
			// Calculate pickup from return (forward offset)
			pickupLoc = new WorldPoint(
				returnLoc.getX() + PICKUP_OFFSET_X,
				returnLoc.getY() + PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}
		else if (returnLoc == null && pickupLoc == null)
		{
			// Fallback: use boat location as dropoff and calculate pickup
			returnLoc = boatLocation;
			pickupLoc = new WorldPoint(
				returnLoc.getX() + PICKUP_OFFSET_X,
				returnLoc.getY() + PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}

		// Plan all laps considering total time
		// Use pathfinding offsets: pickup +2 north, dropoff +2 south
		WorldPoint pathfindingPickup = pickupLoc != null ? new WorldPoint(pickupLoc.getX(), pickupLoc.getY() + 2, pickupLoc.getPlane()) : null;
		WorldPoint pathfindingDropoff = returnLoc != null ? new WorldPoint(returnLoc.getX(), returnLoc.getY() - 2, returnLoc.getPlane()) : null;

		int maxSearchDistance = 100;
		plannedLaps = optimizer.planMultipleLaps(
			boatLocation,
			lostSupplies,
			lapsNeeded,
			pathfindingPickup,
			pathfindingDropoff,
			maxSearchDistance,
			hasRumOnUs,
			currentLap
		);

		// Track which supplies are assigned to which laps
		for (int lap = 0; lap < plannedLaps.size(); lap++)
		{
			List<WorldPoint> lapPath = plannedLaps.get(lap);

			// Find which supplies correspond to this lap's waypoints
			for (WorldPoint waypoint : lapPath)
			{
				for (GameObject supply : lostSupplies)
				{
					if (supply.getWorldLocation().equals(waypoint))
					{
						if (lap == 0)
						{
							lostSuppliesForCurrentLap.add(supply);
						}
						else
						{
							lostSuppliesForFutureLaps.add(supply);
						}
						break;
					}
				}
			}
		}

		log.debug("Planned {} laps with {} total supplies ({} current, {} future) - TOTAL TIME OPTIMIZED",
			plannedLaps.size(), lostSupplies.size(), lostSuppliesForCurrentLap.size(), lostSuppliesForFutureLaps.size());
	}

	/**
	 * Replans only the current lap using assigned supplies (doesn't go backwards for future lap supplies)
	 * Uses strategic pathfinding (rocks + speed boosts, no clouds)
	 */
	private void replanCurrentLapOnly()
	{
		if (boatLocation == null)
		{
			return;
		}

		// Only consider supplies assigned to current lap
		Set<GameObject> currentLapSupplies = new HashSet<>(lostSuppliesForCurrentLap);
		currentLapSupplies.retainAll(lostSupplies); // Only supplies still visible

		if (currentLapSupplies.isEmpty())
		{
			// No supplies for current lap, path to rum pickup
			plannedLaps.clear();
			plannedLaps.add(new ArrayList<>());
			return;
		}

		// Create multi-lap optimizer (uses straight-line distance with rock and exclusion zone penalties)
		boolean preferWestStart = config.startingDirection() == BarracudaTrialConfig.StartingDirection.WEST;
		MultiLapOptimizer optimizer = new MultiLapOptimizer(
			knownRockLocations,
			preferWestStart,
			exclusionZoneMinX,
			exclusionZoneMaxX,
			exclusionZoneMinY,
			exclusionZoneMaxY
		);

		// Use fallback locations if we haven't seen the rum objects yet
		WorldPoint pickupLoc = rumPickupLocation;
		WorldPoint returnLoc = rumReturnLocation;

		// Calculate missing locations using known offsets
		if (returnLoc == null && pickupLoc != null)
		{
			// Calculate return from pickup (reverse offset)
			returnLoc = new WorldPoint(
				pickupLoc.getX() - PICKUP_OFFSET_X,
				pickupLoc.getY() - PICKUP_OFFSET_Y,
				pickupLoc.getPlane()
			);
		}
		else if (pickupLoc == null && returnLoc != null)
		{
			// Calculate pickup from return (forward offset)
			pickupLoc = new WorldPoint(
				returnLoc.getX() + PICKUP_OFFSET_X,
				returnLoc.getY() + PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}
		else if (returnLoc == null && pickupLoc == null)
		{
			// Fallback: use boat location as dropoff and calculate pickup
			returnLoc = boatLocation;
			pickupLoc = new WorldPoint(
				returnLoc.getX() + PICKUP_OFFSET_X,
				returnLoc.getY() + PICKUP_OFFSET_Y,
				returnLoc.getPlane()
			);
		}

		// Replan current lap
		// Use pathfinding offsets: pickup +2 north, dropoff +2 south
		WorldPoint pathfindingPickup = pickupLoc != null ? new WorldPoint(pickupLoc.getX(), pickupLoc.getY() + 2, pickupLoc.getPlane()) : null;
		WorldPoint pathfindingDropoff = returnLoc != null ? new WorldPoint(returnLoc.getX(), returnLoc.getY() - 2, returnLoc.getPlane()) : null;

		int maxSearchDistance = 100;
		List<List<WorldPoint>> replanned = optimizer.planMultipleLaps(
			boatLocation,
			currentLapSupplies,
			1, // Just current lap
			pathfindingPickup,
			pathfindingDropoff,
			maxSearchDistance,
			hasRumOnUs,
			currentLap
		);

		plannedLaps.clear();
		if (!replanned.isEmpty())
		{
			plannedLaps.add(replanned.get(0));
		}

		// Also plan next lap as preview (using future lap supplies)
		if (currentLap + 1 < rumsNeeded && !lostSuppliesForFutureLaps.isEmpty())
		{
			Set<GameObject> futureLapSupplies = new HashSet<>(lostSuppliesForFutureLaps);
			futureLapSupplies.retainAll(lostSupplies);

			if (!futureLapSupplies.isEmpty() && rumReturnLocation != null)
			{
				List<List<WorldPoint>> nextLapPlan = optimizer.planMultipleLaps(
					rumReturnLocation,
					futureLapSupplies,
					1,
					rumPickupLocation,
					rumReturnLocation,
					maxSearchDistance,
					false, // Next lap starts without rum
					currentLap + 1
				);

				if (!nextLapPlan.isEmpty())
				{
					plannedLaps.add(nextLapPlan.get(0));
				}
			}
		}

		log.debug("Replanned current lap only with {} supplies (avoiding {} future lap supplies)",
			currentLapSupplies.size(), lostSuppliesForFutureLaps.size());
	}

	// Removed: All lap path calculation methods - now handled by MultiLapOptimizer

	private List<WorldPoint> generateCircularPath(WorldPoint start)
	{
		List<WorldPoint> path = new ArrayList<>();

		// If we don't have rum return location yet, can't calculate circular path
		if (rumReturnLocation == null)
		{
			return path;
		}

		// Create waypoints traveling west around the island
		// These waypoints circle around the exclusion zone
		int centerX = (exclusionZoneMinX + exclusionZoneMaxX) / 2;
		int centerY = (exclusionZoneMinY + exclusionZoneMaxY) / 2;

		// Calculate radius to stay outside exclusion zone
		int radiusX = (exclusionZoneMaxX - exclusionZoneMinX) / 2 + 10; // 10 tile buffer
		int radiusY = (exclusionZoneMaxY - exclusionZoneMinY) / 2 + 10;

		// Generate 8 waypoints around the circle, starting from current position
		// and going counter-clockwise (west)
		double startAngle = Math.atan2(start.getY() - centerY, start.getX() - centerX);

		for (int i = 1; i <= 8; i++)
		{
			// Travel counter-clockwise (subtract angle)
			double angle = startAngle - (i * Math.PI / 4); // 45 degrees per waypoint

			int waypointX = (int) (centerX + radiusX * Math.cos(angle));
			int waypointY = (int) (centerY + radiusY * Math.sin(angle));

			path.add(new WorldPoint(waypointX, waypointY, start.getPlane()));
		}

		return path;
	}

	private boolean isCloudDangerous(NPC cloud)
	{
		int animation = cloud.getAnimation();

		// Cloud is dangerous if it's NOT in a safe animation state
		return animation != CLOUD_ANIM_HARMLESS && animation != CLOUD_ANIM_HARMLESS_ALT;
	}

	/**
	 * Public getter for safe animation IDs (used by overlay)
	 */
	public boolean isCloudSafe(int animationId)
	{
		return animationId == CLOUD_ANIM_HARMLESS || animationId == CLOUD_ANIM_HARMLESS_ALT;
	}

	private double calculateAngleDifference(WorldPoint dir1, WorldPoint dir2)
	{
		// Normalize directions
		double mag1 = Math.sqrt(dir1.getX() * dir1.getX() + dir1.getY() * dir1.getY());
		double mag2 = Math.sqrt(dir2.getX() * dir2.getX() + dir2.getY() * dir2.getY());

		if (mag1 == 0 || mag2 == 0)
		{
			return 0;
		}

		double nx1 = dir1.getX() / mag1;
		double ny1 = dir1.getY() / mag1;
		double nx2 = dir2.getX() / mag2;
		double ny2 = dir2.getY() / mag2;

		// Dot product gives cos(angle)
		double dotProduct = nx1 * nx2 + ny1 * ny2;
		dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct)); // Clamp to [-1, 1]

		// Convert to degrees
		double angleRadians = Math.acos(dotProduct);
		return Math.toDegrees(angleRadians);
	}

	// Removed: All custom pathfinding logic - now handled by A* pathfinding classes

	/**
	 * Clears persistent storage of static object locations
	 * Called when difficulty changes or leaving trial area
	 */
	private void clearPersistentStorage()
	{
		int totalStored = knownRockLocations.size() + knownSpeedBoostLocations.size() + knownLostSuppliesSpawnLocations.size();

		knownRockLocations.clear();
		knownSpeedBoostLocations.clear();
		knownLostSuppliesSpawnLocations.clear();

		log.info("Cleared {} persistent locations (rocks, speed boosts, supply spawns)", totalStored);
	}

	private void reset()
	{
		log.info("Resetting state");

		inTrialArea = false;
		lostSupplies.clear();
		lightningClouds.clear();
		rocks.clear();
		speedBoosts.clear();
		optimalPath.clear();
		rumPickupLocation = null;
		rumReturnLocation = null;
		rumsCollected = 0;
		rumsNeeded = 0;
		cratesCollected = 0;
		cratesTotal = 0;
		lastRumCount = 0;
		hasRumOnUs = false;
		lastPathRecalcCaller = "none";
		boatLocation = null;
		currentLap = 0;
		plannedLaps.clear();
		currentSegmentPath.clear();
		nextSegmentPath.clear();
		remainingLostSupplies.clear();
		lostSuppliesForCurrentLap.clear();
		lostSuppliesForFutureLaps.clear();
		allRocksInScene.clear();
		lastKnownDifficulty = 0;
		ticksSinceLastPathRecalc = 0;

		// Clear persistent storage when leaving trial area
		clearPersistentStorage();
	}

	@Provides
	BarracudaTrialConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarracudaTrialConfig.class);
	}
}
