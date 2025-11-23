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
	public PathResult findPath(BarracudaTileCostCalculator costCalculator, RouteOptimization routeOptimization, WorldPoint start, WorldPoint goal, int maxSearchDistance, int boatDirectionDx, int boatDirectionDy)
	{
		PriorityQueue<Node> openSet = new PriorityQueue<>(
			Comparator.comparingDouble((Node n) -> n.fScore)
				.thenComparingDouble(n -> manhattanDistance(n.position, goal))
		);
		Map<WorldPoint, Node> allNodes = new HashMap<>();

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

			if (closedSet.contains(current.position))
			{
				continue;
			}

			if (current.position.equals(goal))
			{
				return new PathResult(reconstructPath(current), current.gScore);
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

				// For first move from start: enforce boat's forward direction constraint
				// The boat cannot turn in place - it must move forward first
				if (current.parent == null && (boatDirectionDx != 0 || boatDirectionDy != 0))
				{
					if (!isInForwardCone(current.position, neighbor, boatDirectionDx, boatDirectionDy))
					{
						continue; // Skip neighbors not in forward cone
					}
				}

				// Get tile cost for this neighbor
				double tileCost = costCalculator.getTileCost(current.position, neighbor);

				// If tile is impassable (very high cost), skip it
				if (tileCost > 1000)
				{
					continue;
				}

				// Boat moves in continuous space at constant speed, so distance matters
				boolean isDiagonal = Math.abs(neighbor.getX() - current.position.getX()) == 1
								  && Math.abs(neighbor.getY() - current.position.getY()) == 1;
				double geometricDistance = isDiagonal ? Math.sqrt(2) : 1.0;

				double turningCost = calculateTurningCost(routeOptimization, current, neighbor);

				double tentativeGScore = current.gScore + (tileCost * geometricDistance) + turningCost;

                Node neighborNode = allNodes.computeIfAbsent(neighbor, Node::new);

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

		return new PathResult(new ArrayList<PathNode>(), Double.POSITIVE_INFINITY);
	}

	private double calculateTurningCost(RouteOptimization routeOptimization, Node current, WorldPoint neighbor)
	{
		// No turning cost for first move (no previous direction)
		if (current.parent == null)
		{
			return 0.0;
		}

		double prevDx = current.position.getX() - current.parent.position.getX();
		double prevDy = current.position.getY() - current.parent.position.getY();

		double newDx = neighbor.getX() - current.position.getX();
		double newDy = neighbor.getY() - current.position.getY();

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

		double dotProduct = prevDx * newDx + prevDy * newDy;

		// Clamp to [-1, 1] to avoid floating point errors
		dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));

		return getWastedMovement(routeOptimization, dotProduct);
	}

	private double getWastedMovement(RouteOptimization routeOptimization, double dotProduct) {
		double turnAngleRadians = Math.acos(dotProduct);
		double turnAngleDegrees = Math.toDegrees(turnAngleRadians);

		// Calculate ticks needed to turn (15°/tick turn rate)
		double ticksToTurn = turnAngleDegrees / 15.0;

		double wastedMovement;
		if (turnAngleDegrees <= 90)
		{
			if (routeOptimization == RouteOptimization.EFFICIENT)
			{
				// Efficient: Gentle linear cost (good for grabbing nearby boosts)
				wastedMovement = ticksToTurn * 0.17;
			}
			else
			{
				// Relaxed: Higher cost to discourage all turns (smoother routes)
				wastedMovement = 0.3 + ticksToTurn * 0.7;
			}
		}
		else
		{
			// Explosive cost for turns over 90° (same for both modes)
			double ticksOver90 = ticksToTurn - 6.0; // 90° = 6 ticks
			wastedMovement = 1.0 + Math.pow(ticksOver90, 2.5) * 8.0;
		}
		return wastedMovement;
	}

	/**
	 * Checks if a neighbor tile is within the boat's forward cone of movement
	 * The boat must move forward - it cannot turn in place
	 * We use a generous cone (90 degrees / dot product > 0) to account for grid quantization
	 *
	 * @param current Current position
	 * @param neighbor Neighbor being evaluated
	 * @param boatDx Boat's forward direction X component
	 * @param boatDy Boat's forward direction Y component
	 * @return true if neighbor is "forward enough" to be reachable on first move
	 */
	private boolean isInForwardCone(WorldPoint current, WorldPoint neighbor, int boatDx, int boatDy)
	{
		// Calculate movement direction from current to neighbor
		int moveDx = neighbor.getX() - current.getX();
		int moveDy = neighbor.getY() - current.getY();

		// Calculate dot product between boat direction and movement direction
		// Dot product > 0 means angle < 90 degrees (forward-ish)
		// Dot product = 0 means perpendicular
		// Dot product < 0 means angle > 90 degrees (backwards)
		int dotProduct = moveDx * boatDx + moveDy * boatDy;

		// Allow any tile in the forward hemisphere (dot product > 0)
		// This is a 180-degree cone, which is generous but accounts for:
		// - Grid quantization errors (boat rotates in 15° but tiles are 90° aligned)
		// - Estimation errors in front tile position
		// We still reject pure backwards movement (dot product <= 0)
		return dotProduct > 0;
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
