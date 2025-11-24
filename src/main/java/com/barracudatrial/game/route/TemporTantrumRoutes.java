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
	private static final WorldPoint RUM_PICKUP_LOCATION = new WorldPoint(3037, 2767, 0);
	private static final WorldPoint RUM_DROPOFF_LOCATION = new WorldPoint(3035, 2926, 0);

	private static final Map<Difficulty, List<RouteWaypoint>> ROUTES = new HashMap<>();

	static
	{
		// SWORDFISH difficulty - 1 lap, 14 shipments + rum pickup/dropoff
		// Captured 2025-11-21
		ROUTES.put(Difficulty.SWORDFISH, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2994, 2891, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2866, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2981, 2848, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2828, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2990, 2808, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3001, 2788, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3012, 2768, 0)),
			new RouteWaypoint(WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3057, 2792, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3065, 2811, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3077, 2825, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3078, 2863, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3082, 2875, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3084, 2896, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3053, 2920, 0)),
			new RouteWaypoint(WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION)
		));

		// SHARK difficulty - 2 laps
		// Captured 2025-11-22
		ROUTES.put(Difficulty.SHARK, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2994, 2891, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2866, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2981, 2848, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2978, 2828, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2990, 2808, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3001, 2788, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3012, 2768, 0)),
			new RouteWaypoint(WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3057, 2792, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3065, 2811, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3077, 2825, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3078, 2863, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3082, 2875, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3059, 2904, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3053, 2920, 0)),
			new RouteWaypoint(WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3018, 2889, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3001, 2866, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3004, 2834, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3006, 2819, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3028, 2789, 0)),
			new RouteWaypoint(2, WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3077, 2776, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3082, 2801, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3093, 2835, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3093, 2882, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3084, 2896, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3079, 2906, 0)),
			new RouteWaypoint(2, WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION)
		));

		// MARLIN difficulty - 3 laps
		// Captured 2025-11-22
		ROUTES.put(Difficulty.MARLIN, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3018, 2889, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3001, 2866, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3004, 2834, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3006, 2819, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3030, 2815, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3028, 2789, 0)),
			new RouteWaypoint(WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3077, 2776, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3082, 2801, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3093, 2835, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3078, 2863, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3082, 2875, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(3059, 2904, 0)),
			new RouteWaypoint(WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2994, 2891, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2978, 2866, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2981, 2848, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2978, 2828, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2990, 2808, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3001, 2788, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3012, 2768, 0)),
			new RouteWaypoint(2, WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3037, 2762, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3057, 2792, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3065, 2811, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3077, 2825, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(3060, 2883, 0)),
			new RouteWaypoint(2, WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2963, 2882, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2965, 2865, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2957, 2829, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2953, 2809, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2966, 2795, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(2987, 2777, 0)),
			new RouteWaypoint(3, WaypointType.RUM_PICKUP, RUM_PICKUP_LOCATION),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3095, 2775, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3119, 2855, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3093, 2882, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3084, 2896, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3079, 2906, 0)),
			new RouteWaypoint(3, WaypointType.SHIPMENT, new WorldPoint(3053, 2920, 0)),
			new RouteWaypoint(3, WaypointType.RUM_DROPOFF, RUM_DROPOFF_LOCATION)
		));
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
}
