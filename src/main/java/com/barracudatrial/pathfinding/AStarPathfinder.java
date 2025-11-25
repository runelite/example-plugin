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
		// Start heading: map provided 8-way boat direction into a 24-heading index (15° steps)
		startNode.headingIdx = -1;
		if (boatDirectionDx != 0 || boatDirectionDy != 0)
		{
			int snappedDx = Integer.signum(boatDirectionDx);
			int snappedDy = Integer.signum(boatDirectionDy);
			int baseDir8 = dirIndex(snappedDx, snappedDy);
			if (baseDir8 != -1)
			{
				startNode.headingIdx = DIR8_TO_HEADING24[baseDir8];
			}
		}

		openSet.add(startNode);
		allNodes.put(new StateKey(start, startNode.headingIdx), startNode);

		Set<StateKey> closedSet = new HashSet<>();
		int nodesExplored = 0;

		while (!openSet.isEmpty())
		{
			Node current = openSet.poll();

			StateKey currentKey = new StateKey(current.position, current.headingIdx);
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

			// Steering neighbors: delta heading -1,0,+1 (±15°) and move one tile in the heading's dominant 8-way direction
			for (int deltaH : new int[] { -1, 0, 1 })
			{
				int currentHeading = current.headingIdx;

				// If unknown heading, treat each 8-way direction as a possible base heading
				if (currentHeading == -1)
				{
					for (int baseDir8 = 0; baseDir8 < 8; baseDir8++)
					{
						int nextHeading = DIR8_TO_HEADING24[baseDir8];
						int moveDir = headingToDir8(nextHeading);
						int nx = current.position.getX() + DIRS[moveDir][0];
						int ny = current.position.getY() + DIRS[moveDir][1];
						WorldPoint neighbor = new WorldPoint(nx, ny, current.position.getPlane());

						StateKey neighborKey = new StateKey(neighbor, nextHeading);
						if (closedSet.contains(neighborKey))
						{
							continue;
						}

						double tileCost = costCalculator.getTileCost(current.position, neighbor);
						if (tileCost > 50000)
						{
							continue;
						}

						boolean isDiagonal = Math.abs(nx - current.position.getX()) == 1 && Math.abs(ny - current.position.getY()) == 1;
						double geometricDistance = isDiagonal ? Math.sqrt(2) : 1.0;

						int absDelta = 0; // starting from unknown, count as no extra turning penalty for first move
						double turningCost = calculateTurningCost(routeOptimization, absDelta);

						double tentativeGScore = current.gScore + (tileCost * geometricDistance) + turningCost;

						Node neighborNode = allNodes.get(neighborKey);
						if (neighborNode == null)
						{
							neighborNode = new Node(neighbor);
							neighborNode.headingIdx = nextHeading;
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

					continue; // processed all initial headings
				}

				int nextHeading = (currentHeading + deltaH + 24) % 24;
				int moveDir = headingToDir8(nextHeading);
				int nx = current.position.getX() + DIRS[moveDir][0];
				int ny = current.position.getY() + DIRS[moveDir][1];
				WorldPoint neighbor = new WorldPoint(nx, ny, current.position.getPlane());

				StateKey neighborKey = new StateKey(neighbor, nextHeading);
				if (closedSet.contains(neighborKey))
				{
					continue;
				}

				double tileCost = costCalculator.getTileCost(current.position, neighbor);
				if (tileCost > 50000)
				{
					continue;
				}

				boolean isDiagonal = Math.abs(nx - current.position.getX()) == 1 && Math.abs(ny - current.position.getY()) == 1;
				double geometricDistance = isDiagonal ? Math.sqrt(2) : 1.0;

				int absDelta = Math.abs(deltaH);
				double turningCost = calculateTurningCost(routeOptimization, absDelta);

				double tentativeGScore = current.gScore + (tileCost * geometricDistance) + turningCost;

				Node neighborNode = allNodes.get(neighborKey);
				if (neighborNode == null)
				{
					neighborNode = new Node(neighbor);
					neighborNode.headingIdx = nextHeading;
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

	private double calculateTurningCost(RouteOptimization routeOptimization, int absDelta)
	{
		// absDelta is the absolute heading step change (in 24-heading units: 0 or 1 here)
		if (absDelta == 0)
		{
			return 0.0;
		}

		// Single 15° step cost (tunable)
		return routeOptimization == RouteOptimization.EFFICIENT ? 1.0 : 2.0;
	}

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

	// Map 24 headings (15° each) to the dominant 8-way movement direction
	private static final int[] HEADING_TO_DIR8 = {
		0, 0,    // 0°, 15° -> E
		1, 1, 1, // 30°,45°,60° -> NE
		2, 2, 2, // 75°,90°,105° -> N
		3, 3, 3, // 120°,135°,150° -> NW
		4, 4, 4, // 165°,180°,195° -> W
		5, 5, 5, // 210°,225°,240° -> SW
		6, 6, 6, // 255°,270°,285° -> S
		7, 7, 7, // 300°,315°,330° -> SE
		0        // 345° -> E
	};

	private static final int[] DIR8_TO_HEADING24 = {0, 2, 5, 8, 12, 15, 18, 21};

	private int headingToDir8(int headingIdx)
	{
		int idx = (headingIdx % 24 + 24) % 24;
		return HEADING_TO_DIR8[idx];
	}

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
		private final int headingIdx;

		StateKey(WorldPoint pos, int headingIdx)
		{
			this.pos = pos;
			this.headingIdx = headingIdx;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			StateKey stateKey = (StateKey) o;
			return headingIdx == stateKey.headingIdx && pos.equals(stateKey.pos);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(pos, headingIdx);
		}
	}
	
	private static class Node
	{
		WorldPoint position;
		Node parent;
		int headingIdx = -1; // 0..23 or -1 for unknown
		double gScore = Double.POSITIVE_INFINITY; // Cost from start to this node
		double hScore = 0; // Heuristic cost from this node to goal
		double fScore = Double.POSITIVE_INFINITY; // Total cost (g + h)

		Node(WorldPoint position)
		{
			this.position = position;
		}
	}
}
