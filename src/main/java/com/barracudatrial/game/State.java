package com.barracudatrial.game;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds all game state for Barracuda Trial
 */
public class State
{
	// Constants - Widget IDs
	public static final int BARRACUDA_TRIALS_HUD = 931;
	public static final int WIDGET_BT_HUD = 3;
	public static final int WIDGET_RUM_PROGRESS = 24;
	public static final int WIDGET_CRATE_PROGRESS = 25;

	// Constants - NPC IDs
	public static final int LIGHTNING_CLOUD_IDLE = 15490;
	public static final int LIGHTNING_CLOUD_ATTACKING = 15491;

	// Constants - Animations
	public static final int CLOUD_ANIM_HARMLESS = -1;
	public static final int CLOUD_ANIM_HARMLESS_ALT = 8879;
	public static final int CLOUD_ANIM_DANGEROUS = 8877;
	public static final int CLOUD_ANIM_ATTACKING = 13141;

	// Constants - Gameplay
	public static final int LOST_SUPPLIES_PICKUP_RANGE = 2;
	public static final int PATH_RECALC_INTERVAL = 2;

	// Constants - Object IDs
	public static final int RUM_RETURN_BASE_OBJECT_ID = 59237;
	public static final int RUM_RETURN_IMPOSTOR_ID = 59239;
	public static final int RUM_PICKUP_BASE_OBJECT_ID = 59240;
	public static final int RUM_PICKUP_IMPOSTOR_ID = 59242;

	// Constants - Offsets
	public static final int PICKUP_OFFSET_X = 24;
	public static final int PICKUP_OFFSET_Y = -128;
	public static final int EXCLUSION_MIN_X_OFFSET = 47;
	public static final int EXCLUSION_MAX_X_OFFSET = 102;
	public static final int EXCLUSION_MIN_Y_OFFSET = -51;
	public static final int EXCLUSION_MAX_Y_OFFSET = 12;

	// Trial area state
	@Getter @Setter
	private boolean inTrialArea = false;

	// Game objects
	@Getter
	private final Set<GameObject> lostSupplies = new HashSet<>();

	@Getter
	private final Set<NPC> lightningClouds = new HashSet<>();

	@Getter
	private final Set<GameObject> rocks = new HashSet<>();

	@Getter
	private final Set<GameObject> speedBoosts = new HashSet<>();

	@Getter
	private final Set<GameObject> allRocksInScene = new HashSet<>();

	// Rum locations
	@Getter @Setter
	private WorldPoint rumPickupLocation = null;

	@Getter @Setter
	private WorldPoint rumReturnLocation = null;

	// Progress tracking
	@Getter @Setter
	private int rumsCollected = 0;

	@Getter @Setter
	private int rumsNeeded = 0;

	@Getter @Setter
	private int cratesCollected = 0;

	@Getter @Setter
	private int cratesTotal = 0;

	@Getter @Setter
	private int lastRumCount = 0;

	@Getter @Setter
	private boolean hasRumOnUs = false;

	@Getter @Setter
	private int lastKnownDifficulty = 0;

	// Player state
	@Getter @Setter
	private WorldPoint boatLocation = null;

	@Getter @Setter
	private int currentLap = 0;

	// Path planning
	@Getter @Setter
	private List<WorldPoint> optimalPath = new ArrayList<>();

	@Getter @Setter
	private List<List<WorldPoint>> plannedLaps = new ArrayList<>();

	@Getter @Setter
	private List<WorldPoint> currentSegmentPath = new ArrayList<>();

	@Getter @Setter
	private List<WorldPoint> nextSegmentPath = new ArrayList<>();

	// Lap-specific supply assignments
	@Getter
	private final Set<GameObject> remainingLostSupplies = new HashSet<>();

	@Getter
	private final Set<GameObject> lostSuppliesForCurrentLap = new HashSet<>();

	@Getter
	private final Set<GameObject> lostSuppliesForFutureLaps = new HashSet<>();

	// Performance metrics
	@Getter @Setter
	private long lastAStarTimeMs = 0;

	@Getter @Setter
	private long lastLostSuppliesUpdateTimeMs = 0;

	@Getter @Setter
	private long lastCloudUpdateTimeMs = 0;

	@Getter @Setter
	private long lastPathPlanningTimeMs = 0;

	@Getter @Setter
	private long lastRockUpdateTimeMs = 0;

	@Getter @Setter
	private long lastTotalGameTickTimeMs = 0;

	@Getter @Setter
	private String lastPathRecalcCaller = "none";

	// Persistent storage (survives between laps)
	@Getter
	private final Set<WorldPoint> knownRockLocations = new HashSet<>();

	@Getter
	private final Set<WorldPoint> knownSpeedBoostLocations = new HashSet<>();

	@Getter
	private final Set<WorldPoint> knownLostSuppliesSpawnLocations = new HashSet<>();

	// Path recalc tracking
	@Getter @Setter
	private int ticksSinceLastPathRecalc = 0;

	// Exclusion zone bounds
	@Getter @Setter
	private int exclusionZoneMinX = 0;

	@Getter @Setter
	private int exclusionZoneMaxX = 0;

	@Getter @Setter
	private int exclusionZoneMinY = 0;

	@Getter @Setter
	private int exclusionZoneMaxY = 0;

	/**
	 * Clears all temporary state (called when leaving trial area)
	 */
	public void reset()
	{
		inTrialArea = false;
		lostSupplies.clear();
		lightningClouds.clear();
		rocks.clear();
		speedBoosts.clear();
		allRocksInScene.clear();
		rumPickupLocation = null;
		rumReturnLocation = null;
		rumsCollected = 0;
		rumsNeeded = 0;
		cratesCollected = 0;
		cratesTotal = 0;
		lastRumCount = 0;
		hasRumOnUs = false;
		boatLocation = null;
		currentLap = 0;
		optimalPath = new ArrayList<>();
		plannedLaps = new ArrayList<>();
		currentSegmentPath = new ArrayList<>();
		nextSegmentPath = new ArrayList<>();
		remainingLostSupplies.clear();
		lostSuppliesForCurrentLap.clear();
		lostSuppliesForFutureLaps.clear();
		ticksSinceLastPathRecalc = 0;
		exclusionZoneMinX = 0;
		exclusionZoneMaxX = 0;
		exclusionZoneMinY = 0;
		exclusionZoneMaxY = 0;
	}

	/**
	 * Clears persistent storage (called when difficulty changes)
	 */
	public void clearPersistentStorage()
	{
		knownRockLocations.clear();
		knownSpeedBoostLocations.clear();
		knownLostSuppliesSpawnLocations.clear();
	}
}
