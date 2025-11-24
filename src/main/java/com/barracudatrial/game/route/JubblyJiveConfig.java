package com.barracudatrial.game.route;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PLACEHOLDER: Jubbly Jive configuration
 * Currently uses Tempor Tantrum values as placeholders
 * Will be updated with actual Jubbly Jive object IDs and locations later
 */
public class JubblyJiveConfig implements TrialConfig
{
	// PLACEHOLDER: Using Tempor values temporarily
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

	// PLACEHOLDER: Tempor rock IDs, will need Jubbly-specific IDs
	private static final Set<Integer> ROCK_IDS = Set.of(
		59314, 59315, 60437, 60438, 60440, 60441, 60442, 60443, 60444
	);

	// PLACEHOLDER: Tempor locations, will need actual Jubbly locations
	private static final WorldPoint PLACEHOLDER_OBJECTIVE_LOCATION = new WorldPoint(3037, 2767, 0);

	// PLACEHOLDER: Tempor exclusion zone, will need Jubbly-specific values
	private static final int EXCLUSION_MIN_X_OFFSET = -26;
	private static final int EXCLUSION_MAX_X_OFFSET = 22;
	private static final int EXCLUSION_MIN_Y_OFFSET = -106;
	private static final int EXCLUSION_MAX_Y_OFFSET = -53;

	@Override
	public TrialType getTrialType()
	{
		return TrialType.JUBBLY_JIVE;
	}

	@Override
	public Set<Integer> getShipmentBaseIds()
	{
		return SHIPMENT_IDS; // PLACEHOLDER
	}

	@Override
	public int getShipmentImpostorId()
	{
		return SHIPMENT_IMPOSTOR_ID; // PLACEHOLDER
	}

	@Override
	public Set<Integer> getPrimaryObjectiveIds()
	{
		return Set.of(); // TODO: Toad pillar object IDs
	}

	@Override
	public Set<Integer> getSecondaryObjectiveIds()
	{
		return Set.of(); // TODO: Jubbly bird or related object IDs
	}

	@Override
	public WorldPoint getPrimaryObjectiveLocation()
	{
		return PLACEHOLDER_OBJECTIVE_LOCATION; // TODO: Actual Jubbly location
	}

	@Override
	public WorldPoint getSecondaryObjectiveLocation()
	{
		return PLACEHOLDER_OBJECTIVE_LOCATION; // TODO: Actual Jubbly location
	}

	@Override
	public Set<Integer> getRockIds()
	{
		return ROCK_IDS; // PLACEHOLDER
	}

	@Override
	public int getExclusionMinXOffset()
	{
		return EXCLUSION_MIN_X_OFFSET; // PLACEHOLDER
	}

	@Override
	public int getExclusionMaxXOffset()
	{
		return EXCLUSION_MAX_X_OFFSET; // PLACEHOLDER
	}

	@Override
	public int getExclusionMinYOffset()
	{
		return EXCLUSION_MIN_Y_OFFSET; // PLACEHOLDER
	}

	@Override
	public int getExclusionMaxYOffset()
	{
		return EXCLUSION_MAX_Y_OFFSET; // PLACEHOLDER
	}

	@Override
	public Pattern getPrimaryObjectivePickupPattern()
	{
		return null; // TODO: Jubbly-specific chat pattern
	}

	@Override
	public Pattern getSecondaryObjectiveCompletionPattern()
	{
		return null; // TODO: Jubbly-specific chat pattern
	}

	@Override
	public List<RouteWaypoint> getRoute(Difficulty difficulty)
	{
		return JubblyJiveRoutes.getRoute(difficulty);
	}
}
