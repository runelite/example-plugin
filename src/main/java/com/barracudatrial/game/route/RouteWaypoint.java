package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class RouteWaypoint
{
	@Getter
	private final WaypointType type;
	@Getter
	private final int lap;
	private final WorldPoint storedLocation;

	public RouteWaypoint(WaypointType type, WorldPoint location)
	{
		this.lap = 1;
		this.type = type;
		this.storedLocation = location;
	}

	public RouteWaypoint(int lap, WaypointType type, WorldPoint location)
	{
		this.lap = lap;
		this.type = type;
		this.storedLocation = location;
	}

	public WorldPoint getLocation()
	{
		return storedLocation;
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
