package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.List;

public class PathResult
{
	private final List<WorldPoint> path;
	private final double cost;

	public PathResult(List<WorldPoint> path, double cost)
	{
		this.path = path;
		this.cost = cost;
	}

	public List<WorldPoint> getPath()
	{
		return path;
	}

	public double getCost()
	{
		return cost;
	}
}
