package com.example.KnudsUtils;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Walker {

    private static WorldPoint goal;
    private static List<WorldPoint> path;
    private static WorldPoint currentPathDestination;
    private static final Random rand = new Random();

    // Configurable step size
    private static int minStepSize = 10;
    private static int maxStepSize = 35;

    public static boolean walkTo(WorldPoint destination) {
        System.out.println("Inside walkTo method");
        goal = destination;
        WorldPoint playerPos = EthanApiPlugin.playerPosition();

        if (!destination.isInScene(EthanApiPlugin.getClient())) {
            System.out.println("Destination not in scene - finding intermediate point");

            List<WorldPoint> reachable = EthanApiPlugin.reachableTiles();
            System.out.println("reachable tiles size: " + reachable.size());

            WorldPoint closestToGoal = reachable.stream()
                    .filter(tile -> !tile.equals(playerPos))
                    .min(Comparator.comparingInt(tile -> tile.distanceTo(destination)))
                    .orElse(null);
            System.out.println("Tile closest to goal: " + closestToGoal);

            if (closestToGoal == null) {
                System.out.println("No reachable tiles found");
                goal = null;
                return false;
            }

            System.out.println("Walking to intermediate point: " + closestToGoal +
                    " (distance to goal: " + closestToGoal.distanceTo(destination) + ")");
            MousePackets.queueClickPacket();
            MovementPackets.queueMovement(closestToGoal);
            // Path to the intermediate point
            path = EthanApiPlugin.pathToGoalFromPlayerNoCustomTiles(closestToGoal);
        } else {
            // Destination is in scene, path directly to it
            path = EthanApiPlugin.pathToGoalFromPlayerNoCustomTiles(destination);
        }

        currentPathDestination = null;

        if (path == null) {
            System.out.println("No path found");
            goal = null;
            return false;
        }
        System.out.println("Path found with " + path.size() + " tiles");
        return true;
    }

    /**
     * Start walking with custom dangerous/impassible tiles
     * @param destination Target WorldPoint
     * @param dangerous Tiles to avoid (but can cross if needed)
     * @param impassible Tiles that cannot be crossed
     * @return true if path was found, false otherwise
     */
    public static boolean walkTo(WorldPoint destination, HashSet<WorldPoint> dangerous, HashSet<WorldPoint> impassible) {
        goal = destination;
        path = EthanApiPlugin.pathToGoalFromPlayerUsingReachableTiles(destination, dangerous, impassible);
        currentPathDestination = null;

        if (path == null) {
            System.out.println("No path found to destination");
            goal = null;
            return false;
        }
        return true;
    }

    /**
     * Main walking logic - call this from your plugin's onGameTick
     * @param client Client instance from your plugin
     * @return true if still walking, false if reached goal or no path
     */
    public static boolean handleWalking(Client client) {
        System.out.println("Inside handleWalking method");
        // If player is moving or animating, don't process
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            return isWalking();
        }

        // Check if we've reached the goal
        if (goal != null && goal.equals(EthanApiPlugin.playerPosition())) {
            System.out.println("Reached goal");
            reset();
            return false;
        }

        // If we have a path, process it
        if (path != null && !path.isEmpty()) {
            processPath(client);
            return true;
        }

        return false;
    }

    private static void processPath(Client client) {
        // If we stopped walking unexpectedly, click destination again
        if (currentPathDestination != null
                && !currentPathDestination.equals(EthanApiPlugin.playerPosition())
                && !EthanApiPlugin.isMoving()) {
            System.out.println("Stopped walking. Clicking destination again");
            MousePackets.queueClickPacket();
            MovementPackets.queueMovement(currentPathDestination);
            return;
        }

        // Take next step if we're at destination or not moving
        if (currentPathDestination == null
                || currentPathDestination.equals(EthanApiPlugin.playerPosition())
                || !EthanApiPlugin.isMoving()) {
            takeNextStep(client);
        }
    }

    private static void takeNextStep(Client client) {
        System.out.println("Taking next step in path");
        // Random step size between min and max
        int step = rand.nextInt((maxStepSize - minStepSize) + 1) + minStepSize;
        int max = step;

        // Check for doors along the path
        for (int i = 0; i < step && i < path.size() - 1; i++) {
            if (isDoored(client, path.get(i), path.get(i + 1))) {
                max = i;
                break;
            }
        }

        // Handle door at current position
        if (!path.isEmpty() && isDoored(client, EthanApiPlugin.playerPosition(), path.get(0))) {
            System.out.println("Door detected");
            handleDoor(client, path.get(0));
            return;
        }

        // Calculate actual step size
        step = Math.min(max, path.size() - 1);
        currentPathDestination = path.get(step);

        // Update path (remove tiles we've walked past)
        if (path.indexOf(currentPathDestination) == path.size() - 1) {
            path = null; // We're taking the last step
        } else {
            path = path.subList(step + 1, path.size());
        }

        // Don't walk if we're already at destination
        if (currentPathDestination.equals(EthanApiPlugin.playerPosition())) {
            return;
        }

        // Execute movement
        System.out.println("Walking to: " + currentPathDestination + " (step size: " + step + ")");
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(currentPathDestination);
    }

    private static void handleDoor(Client client, WorldPoint nextTile) {
        WallObject wallObject = getTile(client, EthanApiPlugin.playerPosition()).getWallObject();
        if (wallObject == null) {
            wallObject = getTile(client, nextTile).getWallObject();
        }
        if (wallObject != null) {
            //ObjectPackets.queueObjectAction(wallObject, false, "Open", "Close");
        }
    }

    /**
     * Check if there's a door between two adjacent tiles
     */
    private static boolean isDoored(Client client, WorldPoint a, WorldPoint b) {
        Tile tileA = getTile(client, a);
        Tile tileB = getTile(client, b);

        if (tileA == null || tileB == null) {
            return false;
        }

        WallObject wallA = tileA.getWallObject();
        WallObject wallB = tileB.getWallObject();

        return (wallA != null && isDoor(client, wallA)) || (wallB != null && isDoor(client, wallB));
    }

    /**
     * Check if WallObject is a door
     */
    private static boolean isDoor(Client client, WallObject wallObject) {
        String name = client.getObjectDefinition(wallObject.getId()).getName();
        return name != null && name.toLowerCase().contains("door");
    }

    private static Tile getTile(Client client, WorldPoint worldPoint) {
        if (client.getTopLevelWorldView() == null) {
            return null;
        }
        if (!worldPoint.isInScene(client)) {
            return null;
        }
        return client.getTopLevelWorldView().getScene().getTiles()
                [worldPoint.getPlane()]
                [worldPoint.getX() - client.getTopLevelWorldView().getBaseX()]
                [worldPoint.getY() - client.getTopLevelWorldView().getBaseY()];
    }

    /**
     * Stop walking and clear path
     */
    public static void reset() {
        goal = null;
        path = null;
        currentPathDestination = null;
    }

    /**
     * Check if currently walking
     */
    public static boolean isWalking() {
        return EthanApiPlugin.isMoving();
    }

    /**
     * Get current goal
     */
    public static WorldPoint getGoal() {
        return goal;
    }

    /**
     * Get current destination we're walking to
     */
    public static WorldPoint getCurrentDestination() {
        return currentPathDestination;
    }

    /**
     * Get remaining path tiles
     */
    public static List<WorldPoint> getRemainingPath() {
        return path;
    }

    /**
     * Set step size range (tiles to walk per step)
     * @param min Minimum tiles per step
     * @param max Maximum tiles per step
     */
    public static void setStepSize(int min, int max) {
        minStepSize = min;
        maxStepSize = max;
    }

    /**
     * Get just the path without starting to walk
     * @param destination Target WorldPoint
     * @return List of WorldPoints in path, or null if unreachable
     */
    public static List<WorldPoint> getPath(WorldPoint destination) {
        return EthanApiPlugin.pathToGoalFromPlayerNoCustomTiles(destination);
    }

    /**
     * Get path with custom obstacles
     * @param destination Target WorldPoint
     * @param dangerous Tiles to avoid
     * @param impassible Tiles that cannot be crossed
     * @return List of WorldPoints in path, or null if unreachable
     */
    public static List<WorldPoint> getPath(WorldPoint destination, HashSet<WorldPoint> dangerous, HashSet<WorldPoint> impassible) {
        return EthanApiPlugin.pathToGoalFromPlayerUsingReachableTiles(destination, dangerous, impassible);
    }
}