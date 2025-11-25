package com.barracudatrial.game.route;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.barracudatrial.game.route.JubblyJiveConfig.TOAD_PICKUP_LOCATION;
import static com.barracudatrial.game.route.JubblyJiveConfig.TOAD_PILLARS;
import static com.barracudatrial.game.route.RouteWaypoint.WaypointType;

// S 931.10 SailingBtHud.BT_PARTIAL_TEXT text = holding toad count
public class JubblyJiveRoutes
{
	private static final Map<Difficulty, List<RouteWaypoint>> ROUTES = new HashMap<>();

	static
	{
		// SWORDFISH difficulty - 1 lap, 20 shipments + 4 toads
		// Captured 2025-11-23
		ROUTES.put(Difficulty.SWORDFISH, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2413, 3016, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2396, 3010, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2378, 3008, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2362, 2998, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2353, 2977, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2345, 2974, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2320, 2976, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2280, 2978, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2250, 2992, 0)),
			new RouteWaypoint(WaypointType.TOAD_PICKUP, TOAD_PICKUP_LOCATION), //baseid 59169 imposter 59170
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2248, 3023, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2311, 3021, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2330, 3016, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2353, 3005, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2358, 2964, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[4]), // ID : 59148 IMPOSTER: 59150. with toad imposter = 59149
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2367, 2948, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2386, 2940, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[5]), // ID : 59154 IMPOSTER: 59156. with toad imposter = 59155
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2421, 2938, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2434, 2949, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2432, 2977, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[6]), // ID : 59160 IMPOSTER: 59162. with toad imposter = 59161
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2439, 2990, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[7]) // ID : 59166 IMPOSTER: 59168. with toad imposter = 59167
		));

		
		// SHARK difficulty - 2 lap, 38 shipments + 12 toads
		// Captured 2025-11-23
		ROUTES.put(Difficulty.SHARK, List.of(
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2413, 3016, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2396, 3010, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2378, 3008, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2362, 2998, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2353, 2977, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2345, 2974, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2332, 2972, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2320, 2976, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2297, 2978, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2280, 2978, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2250, 2992, 0)),
			new RouteWaypoint(WaypointType.TOAD_PICKUP, TOAD_PICKUP_LOCATION), //baseid 59169 imposter 59170
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2239, 3008, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2248, 3023, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2273, 3006, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[0]), // ID : 59124
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2290, 2998, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[1]), // ID : 59130
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2301, 3018, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[2]), // ID : 59136
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2311, 3021, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2330, 3016, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2345, 2991, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2358, 2964, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[4]), // ID : 59148 IMPOSTER: 59150. with toad imposter = 59149
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2367, 2948, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2386, 2940, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2421, 2938, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[5]), // ID : 59154 IMPOSTER: 59156. with toad imposter = 59155
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2434, 2949, 0)),
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2432, 2977, 0)),
			new JubblyJiveToadPillarWaypoint(TOAD_PILLARS[6]), // ID : 59160 IMPOSTER: 59162. with toad imposter = 59161
			new RouteWaypoint(WaypointType.SHIPMENT, new WorldPoint(2439, 2990, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[7]), // 59166 IMPOSTER: 59168. with toad imposter = 59167
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2412, 3026, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2393, 3020, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2371, 3022, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2341, 3031, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2353, 3005, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[3]), // ID : 59142
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2379, 2993, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2382, 2970, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[4]), // ID : 59148 IMPOSTER: 59150. with toad imposter = 59149
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2390, 2956, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2420, 2959, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[5]), // ID : 59154 IMPOSTER: 59156. with toad imposter = 59155
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2422, 2975, 0)),
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2414, 2992, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[6]), // ID : 59160 IMPOSTER: 59162. with toad imposter = 59161
			new RouteWaypoint(2, WaypointType.SHIPMENT, new WorldPoint(2416, 3001, 0)),
			new JubblyJiveToadPillarWaypoint(2, TOAD_PILLARS[7]) // ID : 59166 IMPOSTER: 59168. with toad imposter = 59167
		));

		// TODO: Marlin: 3 Jubbly birds, 56 boxes
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
