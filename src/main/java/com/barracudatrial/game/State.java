package com.barracudatrial.game;

import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.game.route.TrialConfig;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

import java.util.*;

/**
 * Holds all game state for Barracuda Trial
 */
@Getter
public class State
{
	@Setter
	private TrialConfig currentTrial = null;

	public static final int CLOUD_ANIM_HARMLESS = -1;
	public static final int CLOUD_ANIM_HARMLESS_ALT = 8879;

	public static final int PATH_RECALC_INTERVAL = 2;

	public static final int RUM_RETURN_BASE_OBJECT_ID = 59237; // No constant available
	public static final int RUM_RETURN_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD;
	public static final int RUM_PICKUP_BASE_OBJECT_ID = 59240; // No constant available
	public static final int RUM_PICKUP_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD;

	@Setter
	private boolean inTrialArea = false;

	private final Set<GameObject> lostSupplies = new HashSet<>();

	@Setter
	private Set<WorldPoint> routeCaptureSupplyLocations = new HashSet<>();

	private final Set<NPC> lightningClouds = new HashSet<>();

	private final Set<GameObject> speedBoosts = new HashSet<>();

	private final Set<GameObject> fetidPools = new HashSet<>();

	@Setter
	private WorldPoint rumPickupLocation = null;

	@Setter
	private WorldPoint rumReturnLocation = null;

	@Setter
	private int rumsCollected = 0;

	@Setter
	private int rumsNeeded = 0;

	@Setter
	private int lostSuppliesCollected = 0;

	@Setter
	private int lostSuppliesTotal = 0;

	@Setter
	private boolean hasThrowableObjective = false;

	@Setter
	private int lastKnownDifficulty = 0;

	@Setter
	private String currentTrialName = null;

	@Setter
	private WorldPoint boatLocation = null;

	@Setter
	private WorldPoint frontBoatTileEstimatedActual = null;

	@Setter
	private LocalPoint frontBoatTileLocal = null;

	@Setter
	private int currentLap = 1;

	@Setter
	private List<WorldPoint> optimalPath = new ArrayList<>();

	@Setter
	private List<WorldPoint> currentSegmentPath = new ArrayList<>();

	@Setter
	private List<WorldPoint> nextSegmentPath = new ArrayList<>();

	@Setter
	private String lastPathRecalcCaller = "none";

	private final Set<WorldPoint> knownRockLocations = new HashSet<>();

	private final Map<WorldPoint, List<WorldPoint>> knownSpeedBoostLocations = new HashMap<>();

	private final Set<WorldPoint> knownFetidPoolLocations = new HashSet<>();

	private final Set<WorldPoint> knownToadPillarLocations = new HashSet<>();

	// True if interacted with
	private final Map<WorldPoint, Boolean> knownToadPillars = new HashMap<>();

	private final Set<WorldPoint> knownLostSuppliesSpawnLocations = new HashSet<>();

	@Setter
	private int ticksSinceLastPathRecalc = 0;

	@Setter
	private int exclusionZoneMinX = 0;

	@Setter
	private int exclusionZoneMaxX = 0;

	@Setter
	private int exclusionZoneMinY = 0;

	@Setter
	private int exclusionZoneMaxY = 0;

	@Setter
	private List<RouteWaypoint> currentStaticRoute = null;

	@Setter
	private int nextWaypointIndex = 0;

	private final Set<Integer> completedWaypointIndices = new HashSet<>();

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
		currentTrial = null;
		inTrialArea = false;
		lostSupplies.clear();
		routeCaptureSupplyLocations.clear();
		lightningClouds.clear();
		knownToadPillars.clear();
		rumPickupLocation = null;
		rumReturnLocation = null;
		rumsCollected = 0;
		rumsNeeded = 0;
		lostSuppliesCollected = 0;
		lostSuppliesTotal = 0;
		hasThrowableObjective = false;
		currentTrialName = null;
		boatLocation = null;
		currentLap = 1;
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
		completedWaypointIndices.clear();
	}

	public void markWaypointCompleted(int waypointIndex)
	{
		completedWaypointIndices.add(waypointIndex);
	}

	public boolean isWaypointCompleted(int waypointIndex)
	{
		return completedWaypointIndices.contains(waypointIndex);
	}

	/**
	 * Find the first waypoint index that matches the given location
	 * @return waypoint index, or -1 if not found
	 */
	public int findWaypointIndexByLocation(WorldPoint location)
	{
		if (currentStaticRoute == null)
		{
			return -1;
		}

		for (int i = 0; i < currentStaticRoute.size(); i++)
		{
			RouteWaypoint waypoint = currentStaticRoute.get(i);
			if (waypoint.getLocation() != null && waypoint.getLocation().equals(location))
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Clears persistent storage (called when difficulty changes)
	 */
	public void clearPersistentStorage()
	{
		knownLostSuppliesSpawnLocations.clear();
	}
}
