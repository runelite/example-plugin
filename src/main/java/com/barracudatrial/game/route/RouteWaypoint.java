package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Represents a waypoint in a Barracuda Trial route.
 * Can be a shipment, rum pickup, or rum dropoff.
 * Rum waypoints don't store coordinates - they reference constants instead.
 */
public class RouteWaypoint
{
	@Getter
	private final WaypointType type;
	private final WorldPoint storedLocation;

	public RouteWaypoint(WaypointType type, WorldPoint location)
	{
		this.type = type;
		this.storedLocation = location;
	}

	public RouteWaypoint(WaypointType type)
	{
		this.type = type;
		this.storedLocation = null;
	}

	/**
	 * Gets the world location for this waypoint.
	 * For shipments, returns stored location.
	 * For rum waypoints, returns hardcoded constants from RumLocations.
	 */
	public WorldPoint getLocation()
	{
		if (storedLocation != null)
		{
			return storedLocation;
		}

		if (type == WaypointType.RUM_PICKUP)
		{
			return RumLocations.TEMPOR_TANTRUM_PICKUP;
		}
		else if (type == WaypointType.RUM_DROPOFF)
		{
			return RumLocations.TEMPOR_TANTRUM_DROPOFF;
		}

		return null;
	}

	@Getter
	public enum WaypointType
	{
		SHIPMENT(2),
		RUM_PICKUP(7),
		RUM_DROPOFF(7);

		private final int toleranceTiles;

		WaypointType(int toleranceTiles)
		{
			this.toleranceTiles = toleranceTiles;
		}
	}

	@Override
	public String toString()
	{
		WorldPoint loc = getLocation();
		return String.format("%s at %s", type, loc != null ? loc : "unknown");
	}
}
