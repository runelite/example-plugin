package com.barracudatrial.game;

import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds all game state for Barracuda Trial
 */
public class State
{
	// Constants - Widget IDs (kept for backwards compatibility, prefer using InterfaceID.SailingBtHud directly)
	public static final int BARRACUDA_TRIALS_HUD = InterfaceID.SAILING_BT_HUD;

	// Constants - NPC IDs
	public static final int LIGHTNING_CLOUD_IDLE = NpcID.SAILING_SEA_STORMY_CLOUD;
	public static final int LIGHTNING_CLOUD_ATTACKING = NpcID.SAILING_SEA_STORMY_LIGHTNING_STRIKE;

	// Constants - Animations
	public static final int CLOUD_ANIM_HARMLESS = -1;
	public static final int CLOUD_ANIM_HARMLESS_ALT = 8879;
	public static final int CLOUD_ANIM_DANGEROUS = 8877;
	public static final int CLOUD_ANIM_ATTACKING = 13141;

	// Can pick up lost supplies from 2 tiles away
	public static final int LOST_SUPPLIES_PICKUP_RANGE = 2;
	public static final int PATH_RECALC_INTERVAL = 2;

	// Constants - Object IDs
	public static final int RUM_RETURN_BASE_OBJECT_ID = 59237; // No constant available
	public static final int RUM_RETURN_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD;
	public static final int RUM_PICKUP_BASE_OBJECT_ID = 59240; // No constant available
	public static final int RUM_PICKUP_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD;

	// Constants - Offsets
	public static final int PICKUP_OFFSET_X = 24;
	public static final int PICKUP_OFFSET_Y = -128;
	public static final int EXCLUSION_MIN_X_OFFSET = 47;
	public static final int EXCLUSION_MAX_X_OFFSET = 102;
	public static final int EXCLUSION_MIN_Y_OFFSET = -51;
	public static final int EXCLUSION_MAX_Y_OFFSET = 12;

	@Getter @Setter
	private boolean inTrialArea = false;

	@Getter
	private final Set<GameObject> lostSupplies = new HashSet<>();

	@Getter @Setter
	private Set<WorldPoint> visibleSupplyLocations = new HashSet<>();

	@Getter
	private final Set<NPC> lightningClouds = new HashSet<>();

	@Getter
	private final Set<GameObject> rocks = new HashSet<>();

	@Getter
	private final Set<GameObject> speedBoosts = new HashSet<>();

	@Getter
	private final Set<GameObject> allRocksInScene = new HashSet<>();

	@Getter @Setter
	private WorldPoint rumPickupLocation = null;

	@Getter @Setter
	private WorldPoint rumReturnLocation = null;

	@Getter @Setter
	private int rumsCollected = 0;

	@Getter @Setter
	private int rumsNeeded = 0;

	@Getter @Setter
	private int lostSuppliesCollected = 0;

	@Getter @Setter
	private int lostSuppliesTotal = 0;

	@Getter @Setter
	private boolean hasRumOnUs = false;

	@Getter @Setter
	private int lastKnownDifficulty = 0;

	@Getter @Setter
	private String currentTrialName = null;

	@Getter @Setter
	private WorldPoint boatLocation = null;

	@Getter @Setter
	private WorldPoint frontBoatTileEstimatedActual = null;

	@Getter @Setter
	private LocalPoint frontBoatTileLocal = null;

	@Getter @Setter
	private int currentLap = 0;

	@Getter @Setter
	private List<WorldPoint> optimalPath = new ArrayList<>();

	@Getter @Setter
	private List<WorldPoint> currentSegmentPath = new ArrayList<>();

	@Getter @Setter
	private List<WorldPoint> nextSegmentPath = new ArrayList<>();

	@Getter @Setter
	private String lastPathRecalcCaller = "none";

	@Getter
	private final Set<WorldPoint> knownRockLocations = new HashSet<>();

	@Getter
	private final Set<WorldPoint> knownSpeedBoostLocations = new HashSet<>();

	@Getter
	private final Set<WorldPoint> knownLostSuppliesSpawnLocations = new HashSet<>();

	@Getter @Setter
	private int ticksSinceLastPathRecalc = 0;

	@Getter @Setter
	private int exclusionZoneMinX = 0;

	@Getter @Setter
	private int exclusionZoneMaxX = 0;

	@Getter @Setter
	private int exclusionZoneMinY = 0;

	@Getter @Setter
	private int exclusionZoneMaxY = 0;

	@Getter @Setter
	private List<RouteWaypoint> currentStaticRoute = null;

	@Getter @Setter
	private int nextWaypointIndex = 0;

	@Getter
	private final Set<WorldPoint> completedWaypoints = new HashSet<>();

	/**
	 * Maps rumsNeeded to Difficulty enum
	 * @return Difficulty level based on rumsNeeded (1=SWORDFISH, 2=SHARK, 3=MARLIN)
	 */
	public Difficulty getCurrentDifficulty()
	{
		switch (rumsNeeded)
		{
			case 1:
				return Difficulty.SWORDFISH;
			case 2:
				return Difficulty.SHARK;
			case 3:
				return Difficulty.MARLIN;
			default:
				return Difficulty.SWORDFISH; // Default to easiest difficulty
		}
	}

	/**
	 * Clears all temporary state (called when leaving trial area)
	 */
	public void resetAllTemporaryState()
	{
		inTrialArea = false;
		lostSupplies.clear();
		visibleSupplyLocations.clear();
		lightningClouds.clear();
		rocks.clear();
		speedBoosts.clear();
		allRocksInScene.clear();
		rumPickupLocation = null;
		rumReturnLocation = null;
		rumsCollected = 0;
		rumsNeeded = 0;
		lostSuppliesCollected = 0;
		lostSuppliesTotal = 0;
		hasRumOnUs = false;
		currentTrialName = null;
		boatLocation = null;
		currentLap = 0;
		optimalPath = new ArrayList<>();
		currentSegmentPath = new ArrayList<>();
		nextSegmentPath = new ArrayList<>();
		ticksSinceLastPathRecalc = 0;
		exclusionZoneMinX = 0;
		exclusionZoneMaxX = 0;
		exclusionZoneMinY = 0;
		exclusionZoneMaxY = 0;
		currentStaticRoute = null;
		nextWaypointIndex = 0;
		completedWaypoints.clear();
	}

	/**
	 * Mark a waypoint as completed
	 */
	public void markWaypointCompleted(WorldPoint location)
	{
		completedWaypoints.add(location);
	}

	/**
	 * Check if a waypoint at the given location has been completed
	 */
	public boolean isWaypointCompleted(WorldPoint location)
	{
		return completedWaypoints.contains(location);
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
