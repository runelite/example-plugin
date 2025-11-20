package com.barracudatrial.pathfinding;

import net.runelite.api.coords.WorldPoint;

import java.util.*;

/**
 * A* pathfinding algorithm for finding optimal routes between points
 * considering variable tile costs (speed boosts, clouds, rocks, etc.)
 */
public class AStarPathfinder
{
	private final TileCostCalculator costCalculator;

	public AStarPathfinder(TileCostCalculator costCalculator)
	{
		this.costCalculator = costCalculator;
	}

	/**
	 * Finds the optimal path from start to goal using A* algorithm
	 * @param start Starting position
	 * @param goal Goal position
	 * @param maxSearchDistance Maximum tiles to search (prevents infinite loops)
	 * @return List of WorldPoints representing the path, or empty list if no path found
	 */
	public List<WorldPoint> findPath(WorldPoint start, WorldPoint goal, int maxSearchDistance)
	{
		// Priority queue ordered by f-score (g + h), with tie-breaking towards goal
		// When f-scores are equal, prefer nodes closer to goal (Manhattan distance)
		PriorityQueue<Node> openSet = new PriorityQueue<>(
			Comparator.comparingDouble((Node n) -> n.fScore)
				.thenComparingDouble(n -> manhattanDistance(n.position, goal))
		);
		Map<WorldPoint, Node> allNodes = new HashMap<>();

		// Create start node
		Node startNode = new Node(start);
		startNode.gScore = 0;
		startNode.hScore = heuristic(start, goal);
		startNode.fScore = startNode.hScore;

		openSet.add(startNode);
		allNodes.put(start, startNode);

		Set<WorldPoint> closedSet = new HashSet<>();
		int nodesExplored = 0;

		while (!openSet.isEmpty())
		{
			Node current = openSet.poll();

			// Skip if already processed (can happen with duplicates in openSet)
			if (closedSet.contains(current.position))
			{
				continue;
			}

			// Check if we've reached the goal
			if (current.position.equals(goal))
			{
				return reconstructPath(current);
			}

			closedSet.add(current.position);
			nodesExplored++;

			// Prevent infinite search
			if (nodesExplored > maxSearchDistance * maxSearchDistance)
			{
				break;
			}

			// Check all 8 neighbors (including diagonals)
			for (WorldPoint neighbor : getNeighbors(current.position))
			{
				if (closedSet.contains(neighbor))
				{
					continue;
				}

				// Get tile cost for this neighbor
				double tileCost = costCalculator.getTileCost(current.position, neighbor);

				// If tile is impassable (very high cost), skip it
				if (tileCost > 1000)
				{
					continue;
				}

				// Calculate turning cost (wasted movement while turning)
				double turningCost = calculateTurningCost(current, neighbor);

				double tentativeGScore = current.gScore + tileCost + turningCost;

				Node neighborNode = allNodes.get(neighbor);
				if (neighborNode == null)
				{
					neighborNode = new Node(neighbor);
					allNodes.put(neighbor, neighborNode);
				}

				// If this path to neighbor is better than previous, update it
				if (tentativeGScore < neighborNode.gScore)
				{
					neighborNode.parent = current;
					neighborNode.gScore = tentativeGScore;
					neighborNode.hScore = heuristic(neighbor, goal);
					neighborNode.fScore = neighborNode.gScore + neighborNode.hScore;

					// Always add to openSet (allows duplicates)
					// The closedSet check at the top of loop handles duplicates efficiently
					openSet.add(neighborNode);
				}
			}
		}

		// No path found - return empty list
		return new ArrayList<>();
	}

	/**
	 * Calculates turning cost based on wasted movement while turning
	 * Boat turns max 15°/tick and always moves forward
	 * Large turns waste distance traveling in wrong direction
	 */
	private double calculateTurningCost(Node current, WorldPoint neighbor)
	{
		// No turning cost for first move (no previous direction)
		if (current.parent == null)
		{
			return 0.0;
		}

		// Calculate previous direction vector (parent → current)
		double prevDx = current.position.getX() - current.parent.position.getX();
		double prevDy = current.position.getY() - current.parent.position.getY();

		// Calculate new direction vector (current → neighbor)
		double newDx = neighbor.getX() - current.position.getX();
		double newDy = neighbor.getY() - current.position.getY();

		// Normalize vectors
		double prevLength = Math.sqrt(prevDx * prevDx + prevDy * prevDy);
		double newLength = Math.sqrt(newDx * newDx + newDy * newDy);

		if (prevLength == 0 || newLength == 0)
		{
			return 0.0;
		}

		prevDx /= prevLength;
		prevDy /= prevLength;
		newDx /= newLength;
		newDy /= newLength;

		// Calculate dot product (cosine of angle)
		double dotProduct = prevDx * newDx + prevDy * newDy;

		// Clamp to [-1, 1] to avoid floating point errors
		dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));

		// Calculate turn angle in radians
		double turnAngleRadians = Math.acos(dotProduct);
		double turnAngleDegrees = Math.toDegrees(turnAngleRadians);

		// Calculate ticks needed to turn (15°/tick turn rate)
		double ticksToTurn = turnAngleDegrees / 15.0;

		// Wasted movement = ticks spent turning * average inefficiency
		// During a 180° turn, you spend 12 ticks going wrong directions
		// Approximate wasted distance as half the ticks (conservative estimate)
		double wastedMovement = ticksToTurn * 0.5;

		return wastedMovement;
	}

	/**
	 * Heuristic function
	 * Returns 0 (Dijkstra mode) because we have negative edge costs (speed boosts = -5.0)
	 * Manhattan distance is NOT admissible with negative costs
	 */
	private double heuristic(WorldPoint from, WorldPoint to)
	{
		return 0; // Dijkstra mode - guarantees optimal paths with negative costs
	}

	/**
	 * Manhattan distance for tie-breaking
	 * Not used as heuristic (due to negative costs), but used to break ties
	 * towards the goal when f-scores are equal
	 */
	private double manhattanDistance(WorldPoint from, WorldPoint to)
	{
		return Math.abs(from.getX() - to.getX()) + Math.abs(from.getY() - to.getY());
	}

	/**
	 * Gets all 8 neighboring tiles (including diagonals)
	 */
	private List<WorldPoint> getNeighbors(WorldPoint point)
	{
		List<WorldPoint> neighbors = new ArrayList<>();
		int x = point.getX();
		int y = point.getY();
		int plane = point.getPlane();

		// 8 directions: N, NE, E, SE, S, SW, W, NW
		neighbors.add(new WorldPoint(x, y + 1, plane));     // N
		neighbors.add(new WorldPoint(x + 1, y + 1, plane)); // NE
		neighbors.add(new WorldPoint(x + 1, y, plane));     // E
		neighbors.add(new WorldPoint(x + 1, y - 1, plane)); // SE
		neighbors.add(new WorldPoint(x, y - 1, plane));     // S
		neighbors.add(new WorldPoint(x - 1, y - 1, plane)); // SW
		neighbors.add(new WorldPoint(x - 1, y, plane));     // W
		neighbors.add(new WorldPoint(x - 1, y + 1, plane)); // NW

		return neighbors;
	}

	/**
	 * Reconstructs the path from start to goal by following parent pointers
	 */
	private List<WorldPoint> reconstructPath(Node goalNode)
	{
		List<WorldPoint> path = new ArrayList<>();
		Node current = goalNode;

		while (current != null)
		{
			path.add(current.position);
			current = current.parent;
		}

		// Reverse to get path from start to goal
		Collections.reverse(path);
		return path;
	}

	/**
	 * Node class for A* algorithm
	 */
	private static class Node
	{
		WorldPoint position;
		Node parent;
		double gScore = Double.POSITIVE_INFINITY; // Cost from start to this node
		double hScore = 0; // Heuristic cost from this node to goal
		double fScore = Double.POSITIVE_INFINITY; // Total cost (g + h)

		Node(WorldPoint position)
		{
			this.position = position;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (!(obj instanceof Node)) return false;
			Node other = (Node) obj;
			return position.equals(other.position);
		}

		@Override
		public int hashCode()
		{
			return position.hashCode();
		}
	}
}
