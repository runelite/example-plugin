package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class RouteWaypoint
{
	protected final WaypointType type;
	private final int lap;
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
		SHIPMENT(2),
		RUM_PICKUP(7),
		RUM_DROPOFF(7),
		TOAD_PICKUP(9),
		TOAD_PILLAR(10);

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
