package com.barracudatrial.game.route;

import java.util.ArrayList;
import java.util.List;

/**
 * PLACEHOLDER: Jubbly Jive static routes
 * Routes will be defined once actual Jubbly Jive locations and waypoints are known
 */
public class JubblyJiveRoutes
{
	public static List<RouteWaypoint> getRoute(Difficulty difficulty)
	{
		if (difficulty == null)
		{
			return new ArrayList<>();
		}

		// TODO: Define actual routes for each difficulty level
		// Swordfish: 1 Jubbly bird, 20 boxes
		// Shark: 2 Jubbly birds, 38 boxes
		// Marlin: 3 Jubbly birds, 56 boxes

		return new ArrayList<>();
	}
}
