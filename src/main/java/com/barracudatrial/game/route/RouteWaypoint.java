package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Represents a waypoint in a Barracuda Trial route.
 * Can be a shipment, rum pickup, or rum dropoff.
 */
@Getter
public class RouteWaypoint
{
	private final WaypointType type;
	private final WorldPoint location;

	public RouteWaypoint(WaypointType type, WorldPoint location)
	{
		this.type = type;
		this.location = location;
	}

	public enum WaypointType
	{
		SHIPMENT,
		RUM_PICKUP,
		RUM_DROPOFF
	}

	@Override
	public String toString()
	{
		return String.format("%s at %s", type, location);
	}
}
