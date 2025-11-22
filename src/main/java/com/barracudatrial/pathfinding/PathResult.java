package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

public class PathResult
{
	private final List<PathNode> pathNodes;
	private final double totalCost;

	public PathResult(List<PathNode> pathNodes, double totalCost)
	{
		this.pathNodes = pathNodes;
		this.totalCost = totalCost;
	}

	public List<WorldPoint> getPath()
	{
		List<WorldPoint> path = new ArrayList<>();
		for (PathNode node : pathNodes)
		{
			path.add(node.getPosition());
		}
		return path;
	}

	public double getCost()
	{
		return totalCost;
	}

	public double getCostFromIndex(int index)
	{
		if (index < 0 || index >= pathNodes.size())
		{
			return totalCost;
		}
		return totalCost - pathNodes.get(index).getCumulativeCost();
	}

	public List<PathNode> getPathNodes()
	{
		return pathNodes;
	}
}

class PathNode
{
	private final WorldPoint position;
	private final double cumulativeCost;

	public PathNode(WorldPoint position, double cumulativeCost)
	{
		this.position = position;
		this.cumulativeCost = cumulativeCost;
	}

	public WorldPoint getPosition()
	{
		return position;
	}

	public double getCumulativeCost()
	{
		return cumulativeCost;
	}
}
