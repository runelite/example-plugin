package com.barracudatrial.game.route;

import net.runelite.api.coords.WorldPoint;

/**
 * Hardcoded rum pickup and dropoff locations for Barracuda Trials.
 * These locations are fixed per trial and don't change with difficulty.
 */
public class RumLocations
{
	/**
	 * Tempor Tantrum rum pickup location (south boat)
	 */
	public static final WorldPoint TEMPOR_TANTRUM_PICKUP = new WorldPoint(3037, 2767, 0);

	/**
	 * Tempor Tantrum rum dropoff location (north boat)
	 */
	public static final WorldPoint TEMPOR_TANTRUM_DROPOFF = new WorldPoint(3035, 2926, 0);

	/**
	 * Boat exclusion zone dimensions (cannot navigate through boats)
	 * Width = east-west dimension, Height = north-south dimension
	 * These define a rectangle centered on the boat location
	 */
	public static final int BOAT_EXCLUSION_WIDTH = 8;  // East-West
	public static final int BOAT_EXCLUSION_HEIGHT = 3; // North-South
}
