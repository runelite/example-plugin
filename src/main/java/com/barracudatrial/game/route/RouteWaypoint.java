package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class RouteWaypoint
{
	@Getter
	private final WaypointType type;
	@Getter
	private final int lap;
	@Getter
	private final WorldPoint location;

	public RouteWaypoint(WaypointType type, WorldPoint location)
	{
		this.lap = 1;
		this.type = type;
		this.location = location;
	}

	public RouteWaypoint(int lap, WaypointType type, WorldPoint location)
	{
		this.lap = lap;
		this.type = type;
		this.location = location;
	}

	@Getter
	public enum WaypointType
	{
		SHIPMENT(1),
		RUM_PICKUP(4),
		RUM_DROPOFF(4),
		TOAD_PICKUP(4),
		TOAD_PILLAR(4);

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
