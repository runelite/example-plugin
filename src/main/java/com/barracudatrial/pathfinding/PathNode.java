package com.barracudatrial.pathfinding;

import com.barracudatrial.game.route.RouteWaypoint;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class PathNode
{
	private final WorldPoint position;
	private final double cumulativeCost;
	private final RouteWaypoint.WaypointType waypointType;

	public PathNode(WorldPoint position, double cumulativeCost)
	{
		this.position = position;
		this.cumulativeCost = cumulativeCost;
		this.waypointType = null;
	}

	public PathNode(RouteWaypoint waypoint, double cumulativeCost)
	{
		this.position = waypoint.getLocation();
		this.cumulativeCost = cumulativeCost;
		this.waypointType = waypoint.getType();
	}
}
