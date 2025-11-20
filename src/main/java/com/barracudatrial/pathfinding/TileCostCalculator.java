package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

public interface TileCostCalculator
{
	double getTileCost(WorldPoint from, WorldPoint to);
}
