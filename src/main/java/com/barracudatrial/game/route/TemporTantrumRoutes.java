package com.barracudatrial.game.route;
import com.barracudatrial.game.route.RouteWaypoint.WaypointType;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static route data for Tempor Tantrum trial.
 *
 * Routes are hardcoded sequences of RouteWaypoints representing the optimal order
 * to complete objectives for each difficulty level. These routes were captured
 * during actual trial runs using the route capture system in debug mode.
 *
 * Each route contains:
 * - Lost Shipment locations in optimal collection order
 * - Rum pickup and dropoff waypoints at the correct points in sequence
 * - Coordinates are specific to the Tempor Tantrum trial layout
 * - Difficulty determines the number of laps (1, 2, or 3)
 *
 * The PathPlanner uses these routes to direct the player to the next uncompleted
 * waypoint in the sequence, with A* pathfinding handling tactical navigation
 * around clouds and rocks.
 */
public class TemporTantrumRoutes
{
	/**
	 * Static routes mapped by difficulty level.
	 * Each list represents the sequence of waypoints to complete.
	 */
	private static final Map<Difficulty, List<RouteWaypoint>> ROUTES = new HashMap<>();

	static
	{
		// SWORDFISH difficulty - 1 lap, 14 shipments + rum pickup/dropoff
		// Captured 2025-11-21
		// Rum coordinates confirmed via LocationManager and NPC boat WorldEntity
		ROUTES.put(Difficulty.SWORDFISH, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2994, 2891, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2866, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2981, 2848, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2828, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2990, 2808, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3001, 2788, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3012, 2768, 0)),
			new RouteWaypoint(WaypointType.RUM_PICKUP, new WorldPoint(3037, 2767, 0)), // Pick up rum
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3057, 2792, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3065, 2811, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3077, 2825, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3078, 2863, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3082, 2875, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3084, 2896, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3053, 2920, 0)),
			new RouteWaypoint(WaypointType.RUM_DROPOFF, new WorldPoint(3035, 2926, 0)) // Drop off rum
		));

		// SHARK difficulty - 2 laps (not yet captured)
		ROUTES.put(Difficulty.SHARK, new ArrayList<>());

		// MARLIN difficulty - 3 laps (not yet captured)
		ROUTES.put(Difficulty.MARLIN, new ArrayList<>());
	}

	/**
	 * Get the static route for a given difficulty.
	 * @param difficulty The difficulty level
	 * @return List of RouteWaypoints representing the optimal waypoint sequence,
	 *         or empty list if no route is defined for this difficulty
	 */
	public static List<RouteWaypoint> getRoute(Difficulty difficulty)
	{
		return ROUTES.getOrDefault(difficulty, new ArrayList<>());
	}

	/**
	 * Check if a route has been defined for a given difficulty.
	 * @param difficulty The difficulty level
	 * @return true if a non-empty route exists for this difficulty
	 */
	public static boolean hasRoute(Difficulty difficulty)
	{
		List<RouteWaypoint> route = ROUTES.get(difficulty);
		return route != null && !route.isEmpty();
	}

	/**
	 * Get the total number of waypoints in a route for a given difficulty.
	 * @param difficulty The difficulty level
	 * @return The number of waypoints in the route, or 0 if no route exists
	 */
	public static int getRouteLength(Difficulty difficulty)
	{
		List<RouteWaypoint> route = ROUTES.get(difficulty);
		return route != null ? route.size() : 0;
	}
}
