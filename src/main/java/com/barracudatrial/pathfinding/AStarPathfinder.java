package com.barracudatrial.pathfinding;

import com.barracudatrial.RouteOptimization;
import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * A* pathfinding algorithm for finding optimal routes between points
 * considering variable tile costs (speed boosts, clouds, rocks, etc.)
 */
public class AStarPathfinder
{
	public PathResult findPath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, WorldPoint goal, int maxSearchDistance, int boatDirectionDx, int boatDirectionDy, int goalTolerance)
	{
		PriorityQueue<Node> openSet = new PriorityQueue<>(
			Comparator.comparingDouble((Node n) -> n.fScore)
		);
		Map<StateKey, Node> allNodes = new HashMap<>();

		Node startNode = new Node(start);
		startNode.gScore = 0;
		startNode.hScore = heuristic(start, goal);
		startNode.fScore = startNode.hScore;
		// Start heading: snap provided boat direction to a discrete 0..7 index; leave -1 if unknown
		startNode.dirIdx = -1;
		if (boatDirectionDx != 0 || boatDirectionDy != 0)
		{
			int snappedDx = Integer.signum(boatDirectionDx);
			int snappedDy = Integer.signum(boatDirectionDy);
			int startDirIdx = dirIndex(snappedDx, snappedDy);
			if (startDirIdx != -1)
			{
				startNode.dirIdx = startDirIdx;
			}
		}

		openSet.add(startNode);
		allNodes.put(new StateKey(start, startNode.dirIdx), startNode);

		Set<StateKey> closedSet = new HashSet<>();
		int nodesExplored = 0;

		while (!openSet.isEmpty())
		{
			Node current = openSet.poll();

			StateKey currentKey = new StateKey(current.position, current.dirIdx);
			if (closedSet.contains(currentKey))
			{
				continue;
			}

			// Goal check uses Chebyshev distance (max of dx, dy) so a tile radius counts as reached
			int dx = Math.abs(current.position.getX() - goal.getX());
			int dy = Math.abs(current.position.getY() - goal.getY());
			int distanceToGoal = Math.max(dx, dy);

			if (distanceToGoal <= goalTolerance)
			{
				return new PathResult(reconstructPath(current), current.gScore);
			}

			closedSet.add(currentKey);
			nodesExplored++;

			// Prevent runaway search
			if (nodesExplored > maxSearchDistance * maxSearchDistance)
			{
				break;
			}

			// Generate neighbors limited to the front-3 relative to current heading
			neighborLoop:
			for (WorldPoint neighbor : getForwardNeighbors(current.position, current.dirIdx))
			{
				int newDx = neighbor.getX() - current.position.getX();
				int newDy = neighbor.getY() - current.position.getY();
				int nextDirIdx = dirIndex(newDx, newDy);

				StateKey neighborKey = new StateKey(neighbor, nextDirIdx);

				if (closedSet.contains(neighborKey))
				{
					continue;
				}

				double tileCost = costCalculator.getTileCost(current.position, neighbor);
				if (tileCost > 50000)
				{
					continue;
				}

				int turnSteps = 0;
				if (current.dirIdx != -1)
				{
					int rawDiff = Math.abs(current.dirIdx - nextDirIdx);
					turnSteps = Math.min(rawDiff, 8 - rawDiff); // 0..4, front-3 -> 0 or 1
				}

				double extraMomentumCost = 0.0;

				// Turning 45°: approximate sweeping forward several tiles in current heading
				if (turnSteps == 1 && current.dirIdx != -1)
				{
					int radiusTiles = 15; // ~3 tiles forward for a 45° change

					for (int k = 1; k <= radiusTiles; k++)
					{
						int fx = current.position.getX() + k * DIRS[current.dirIdx][0];
						int fy = current.position.getY() + k * DIRS[current.dirIdx][1];
						WorldPoint forwardTile = new WorldPoint(fx, fy, current.position.getPlane());

						double forwardTileCost = costCalculator.getTileCost(current.position, forwardTile);

						// If any tile in the swept corridor is impassable, this turn is impossible
						if (forwardTileCost > 50000)
						{
							continue neighborLoop;
						}

						extraMomentumCost += forwardTileCost;
					}
				}

				boolean isDiagonal = Math.abs(newDx) == 1 && Math.abs(newDy) == 1;
				double geometricDistance = isDiagonal ? Math.sqrt(2) : 1.0;

				double turningCost = calculateTurningCost(routeOptimization, current, nextDirIdx);

				double tentativeGScore = current.gScore
					+ extraMomentumCost
					+ (tileCost * geometricDistance)
					+ turningCost;

				Node neighborNode = allNodes.get(neighborKey);
				if (neighborNode == null)
				{
					neighborNode = new Node(neighbor);
					neighborNode.dirIdx = nextDirIdx;
					allNodes.put(neighborKey, neighborNode);
				}

				if (tentativeGScore < neighborNode.gScore)
				{
					neighborNode.parent = current;
					neighborNode.gScore = tentativeGScore;
					neighborNode.hScore = heuristic(neighbor, goal);
					neighborNode.fScore = neighborNode.gScore + neighborNode.hScore;

					openSet.add(neighborNode);
				}
			}
		}

		return new PathResult(new ArrayList<>(), Double.POSITIVE_INFINITY);
	}

	private double calculateTurningCost(RouteOptimization routeOptimization, Node current, int nextDirIdx)
	{
		if (current.dirIdx == -1)
		{
			return 0.0;
		}

		int diff = Math.abs(current.dirIdx - nextDirIdx);
		diff = Math.min(diff, 8 - diff);

		switch (diff)
		{
			case 0: return 0.0;
			case 1: return routeOptimization == RouteOptimization.EFFICIENT ? 0.5 : 1.0;
			default: return 100000.0; // effectively forbidden by front-3 neighbor policy
		}
	}

	// Uses discrete headings and a front-3 neighbor policy (no trig math)

	/**
	 * Heuristic: returns 0 (Dijkstra mode).
	 * Rationale: tile costs may be negative (speed boosts), so admissible heuristics like
	 * Manhattan/Chebyshev are not safe; Dijkstra guarantees optimality.
	 */
	private double heuristic(WorldPoint from, WorldPoint to)
	{
		return 0;
	}

	private int dirIndex(int dx, int dy)
	{
		for (int i = 0; i < DIRS.length; i++)
		{
			if (DIRS[i][0] == dx && DIRS[i][1] == dy)
			{
				return i;
			}
		}
		return -1;
	}

	private List<WorldPoint> getForwardNeighbors(WorldPoint pos, int dirIdx)
	{
		List<WorldPoint> result = new ArrayList<>();
		int plane = pos.getPlane();
		if (dirIdx == -1)
		{
			for (int i = 0; i < DIRS.length; i++)
			{
				result.add(new WorldPoint(pos.getX() + DIRS[i][0], pos.getY() + DIRS[i][1], plane));
			}
			return result;
		}

		for (int step = -1; step <= 1; step++)
		{
			int nd = (dirIdx + step + 8) % 8;
			int nx = pos.getX() + DIRS[nd][0];
			int ny = pos.getY() + DIRS[nd][1];
			result.add(new WorldPoint(nx, ny, plane));
		}
		return result;
	}

	// getForwardNeighbors generates neighbors based on discrete heading

	private List<PathNode> reconstructPath(Node goalNode)
	{
		List<PathNode> pathNodes = new ArrayList<>();
		Node current = goalNode;

		while (current != null)
		{
			pathNodes.add(new PathNode(current.position, current.gScore));
			current = current.parent;
		}

		// Reverse to get path from start to goal
		Collections.reverse(pathNodes);
		return pathNodes;
	}

	/**
	 * Computes all tiles within a given tolerance distance from target locations.
	 * Uses Chebyshev distance (max of dx, dy) for square areas.
	 *
	 * @param locations Center points
	 * @param tolerance Distance in tiles (1 = 3x3 area, 2 = 5x5 area, etc.)
	 * @return Map from grabbable tile to its center point
	 */
	public static Map<WorldPoint, WorldPoint> computeGrabbableTiles(Set<WorldPoint> locations, int tolerance)
	{
		Map<WorldPoint, WorldPoint> grabbableTiles = new HashMap<>();

		for (WorldPoint center : locations)
		{
			int plane = center.getPlane();

			for (int dx = -tolerance; dx <= tolerance; dx++)
			{
				for (int dy = -tolerance; dy <= tolerance; dy++)
				{
					WorldPoint tile = new WorldPoint(center.getX() + dx, center.getY() + dy, plane);
					grabbableTiles.put(tile, center);
				}
			}
		}

		return grabbableTiles;
	}

	private static final int[][] DIRS = {
		{1, 0},   // 0: E
		{1, 1},   // 1: NE
		{0, 1},   // 2: N
		{-1, 1},  // 3: NW
		{-1, 0},  // 4: W
		{-1, -1}, // 5: SW
		{0, -1},  // 6: S
		{1, -1}   // 7: SE
	};

	private static final class StateKey
	{
		private final WorldPoint pos;
		private final int dirIdx;

		StateKey(WorldPoint pos, int dirIdx)
		{
			this.pos = pos;
			this.dirIdx = dirIdx;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StateKey stateKey = (StateKey) o;
			return dirIdx == stateKey.dirIdx && pos.equals(stateKey.pos);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(pos, dirIdx);
		}
	}
	
	private static class Node
	{
		WorldPoint position;
		Node parent;
        int dirIdx = -1; // 0..7 or -1 for unknown
		double gScore = Double.POSITIVE_INFINITY; // Cost from start to this node
		double hScore = 0; // Heuristic cost from this node to goal
		double fScore = Double.POSITIVE_INFINITY; // Total cost (g + h)

		Node(WorldPoint position)
		{
			this.position = position;
		}
	}
}
