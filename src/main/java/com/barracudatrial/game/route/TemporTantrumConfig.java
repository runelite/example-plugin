package com.barracudatrial.game.route;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class TemporTantrumConfig implements TrialConfig
{
	private static final WorldPoint RUM_PICKUP_LOCATION = new WorldPoint(3037, 2767, 0);
	private static final WorldPoint RUM_DROPOFF_LOCATION = new WorldPoint(3035, 2926, 0);

	public static final int BOAT_EXCLUSION_WIDTH = 8;
	public static final int BOAT_EXCLUSION_HEIGHT = 3;

	public static final int RUM_PICKUP_BASE_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_PARENT;
	public static final int RUM_PICKUP_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_SOUTH_LOC_CHILD;
	public static final int RUM_DROPOFF_BASE_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_PARENT;
	public static final int RUM_DROPOFF_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_NORTH_LOC_CHILD;

	private static final Set<Integer> SHIPMENT_IDS = Set.of(
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_1,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_2,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_3,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_4,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_5,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_6,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_7,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_8,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_9,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_10,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_11,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_12,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_13,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_14,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_15,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_16,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_17,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_18,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_19,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_20,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_21,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_22,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_23,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_24,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_25,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_26,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_27,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_28,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_29,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_30,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_31,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_32,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_33,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_34,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_35,
		ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_36
	);
	private static final int SHIPMENT_IMPOSTOR_ID = ObjectID.SAILING_BT_TEMPOR_TANTRUM_COLLECTABLE_SUPPLIES;

	private static final Set<Integer> ROCK_IDS = Set.of(
		59314, 59315, 60437, 60438, 60440, 60441, 60442, 60443, 60444
	);

	private static final Set<Integer> SPEED_BOOST_IDS = Set.of(
		ObjectID.SAILING_RAPIDS,
		ObjectID.SAILING_RAPIDS_STRONG,
		ObjectID.SAILING_RAPIDS_POWERFUL,
		ObjectID.SAILING_RAPIDS_DEADLY
	);

	public static final Set<Integer> LIGHTNING_CLOUD_NPC_IDS = Set.of(
		NpcID.SAILING_SEA_STORMY_CLOUD,
		NpcID.SAILING_SEA_STORMY_LIGHTNING_STRIKE
	);

	// Exclusion zone offsets relative to rum dropoff location
	public static final int EXCLUSION_MIN_X_OFFSET = -26;
	public static final int EXCLUSION_MAX_X_OFFSET = 22;
	public static final int EXCLUSION_MIN_Y_OFFSET = -106;
	public static final int EXCLUSION_MAX_Y_OFFSET = -53;

	@Override
	public TrialType getTrialType()
	{
		return TrialType.TEMPOR_TANTRUM;
	}

	@Override
	public Set<Integer> getShipmentBaseIds()
	{
		return SHIPMENT_IDS;
	}

	@Override
	public int getShipmentImpostorId()
	{
		return SHIPMENT_IMPOSTOR_ID;
	}

	public WorldPoint getRumPickupLocation()
	{
		return RUM_PICKUP_LOCATION;
	}

	public WorldPoint getRumDropoffLocation()
	{
		return RUM_DROPOFF_LOCATION;
	}

	@Override
	public Set<Integer> getRockIds()
	{
		return ROCK_IDS;
	}

	@Override
	public Set<Integer> getSpeedBoostIds()
	{
		return SPEED_BOOST_IDS;
	}

	@Override
	public List<RouteWaypoint> getRoute(Difficulty difficulty)
	{
		return TemporTantrumRoutes.getRoute(difficulty);
	}
}
