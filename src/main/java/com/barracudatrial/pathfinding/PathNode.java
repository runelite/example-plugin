package com.barracudatrial.pathfinding;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class PathNode
{
	private final WorldPoint position;
	private final double cumulativeCost;

	public PathNode(WorldPoint position, double cumulativeCost)
	{
		this.position = position;
		this.cumulativeCost = cumulativeCost;
	}
}
