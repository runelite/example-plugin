package com.barracudatrial.game;

import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages route capture for static route data collection.
 * When debug mode is enabled, this class tracks examine actions on trial objects
 * and logs WorldPoint coordinates for route generation.
 *
 * Workflow:
 * 1. Examine Rum Dropoff (north) - starts/resets capture
 * 2. Examine Lost Shipments in order - logs each shipment location
 * 3. Examine Rum Pickup (south) - logs pickup location
 * 4. Continue examining remaining shipments for subsequent laps
 * 5. Examine Rum Dropoff again after pickup - completes capture
 *
 * Output format is designed for easy copy/paste to AI for code generation.
 */
@Slf4j
public class RouteCapture
{
	private boolean isCapturing = false;
	private boolean hasExaminedPickup = false;
	private final List<RouteWaypoint> capturedWaypoints = new ArrayList<>();
	private final List<WorldPoint> shipmentLocationsForVisualFeedback = new ArrayList<>();
	private final State state;

	public RouteCapture(State state)
	{
		this.state = state;
	}

	/**
	 * Handle examine action on Rum Dropoff (north location).
	 * Starts new capture or completes capture if already collecting.
	 */
	public void onExamineRumDropoff(WorldPoint location, int sceneX, int sceneY, int sceneBaseX, int sceneBaseY, int objectId, String impostorInfo)
	{
		// Detailed logging for debugging location issues
		log.info("[ROUTE CAPTURE] Rum Dropoff examined:");
		log.info("[ROUTE CAPTURE]   ObjectID: {}, ImpostorID: {}", objectId, impostorInfo);
		log.info("[ROUTE CAPTURE]   SceneXY: ({}, {}), SceneBase: ({}, {})", sceneX, sceneY, sceneBaseX, sceneBaseY);
		log.info("[ROUTE CAPTURE]   Boat WorldPoint (REAL): {}", formatWorldPoint(location));
		log.info("[ROUTE CAPTURE]   Plane: {}", location.getPlane());
		log.info("[ROUTE CAPTURE]   State.rumReturnLocation: {}",
			state.getRumReturnLocation() != null ? formatWorldPoint(state.getRumReturnLocation()) : "null");
		log.info("[ROUTE CAPTURE]   NOTE: Using boat's world location, not scene-local coords");

		if (!isCapturing)
		{
			startCapture(location);
		}
		else if (hasExaminedPickup)
		{
			completeCapture(location);
		}
		else
		{
			log.info("[ROUTE CAPTURE] RESET - Starting over");
			startCapture(location);
		}
	}

	/**
	 * Handle examine action on Rum Pickup (south location).
	 */
	public void onExamineRumPickup(WorldPoint location, int sceneX, int sceneY, int sceneBaseX, int sceneBaseY, int objectId, String impostorInfo)
	{
		if (!isCapturing)
		{
			return;
		}

		capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.RUM_PICKUP, location));
		hasExaminedPickup = true;

		// Detailed logging for debugging location issues
		log.info("[ROUTE CAPTURE] Rum Pickup examined:");
		log.info("[ROUTE CAPTURE]   ObjectID: {}, ImpostorID: {}", objectId, impostorInfo);
		log.info("[ROUTE CAPTURE]   SceneXY: ({}, {}), SceneBase: ({}, {})", sceneX, sceneY, sceneBaseX, sceneBaseY);
		log.info("[ROUTE CAPTURE]   Boat WorldPoint (REAL): {}", formatWorldPoint(location));
		log.info("[ROUTE CAPTURE]   Plane: {}", location.getPlane());
		log.info("[ROUTE CAPTURE]   State.rumPickupLocation: {}",
			state.getRumPickupLocation() != null ? formatWorldPoint(state.getRumPickupLocation()) : "null");
		log.info("[ROUTE CAPTURE]   NOTE: Using boat's world location, not scene-local coords");
	}

	/**
	 * Handle examine action on Lost Shipment.
	 */
	public void onExamineLostShipment(WorldPoint location)
	{
		if (!isCapturing)
		{
			return;
		}

		capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.SHIPMENT, location));
		shipmentLocationsForVisualFeedback.add(location);
		int waypointNumber = capturedWaypoints.size();
		log.info("[ROUTE CAPTURE] Shipment #{}: {}", waypointNumber, formatWorldPoint(location));
	}

	/**
	 * Start a new route capture session.
	 */
	private void startCapture(WorldPoint dropoffLocation)
	{
		isCapturing = true;
		hasExaminedPickup = false;
		capturedWaypoints.clear();
		shipmentLocationsForVisualFeedback.clear();

		log.info("[ROUTE CAPTURE] Started new route capture");
		log.info("[ROUTE CAPTURE] Starting location (Rum Dropoff): {}", formatWorldPoint(dropoffLocation));
	}

	/**
	 * Complete the route capture and output formatted results.
	 */
	private void completeCapture(WorldPoint dropoffLocation)
	{
		// Add final dropoff waypoint
		capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.RUM_DROPOFF, dropoffLocation));

		log.info("[ROUTE CAPTURE] COMPLETE - Route captured successfully");
		log.info("[ROUTE CAPTURE] === COMPLETE ROUTE ===");
		log.info("Trial: Tempor Tantrum");

		// Determine difficulty from state
		Difficulty difficulty = Difficulty.fromRumsRequired(state.getRumsNeeded());
		if (difficulty != null)
		{
			log.info("Difficulty: {} ({} lap{})", difficulty, difficulty.getRumsRequired(),
				difficulty.getRumsRequired() > 1 ? "s" : "");
		}
		else
		{
			log.info("Difficulty: Unknown (rums needed: {})", state.getRumsNeeded());
		}

		log.info("");
		log.info("Waypoints (copy this Java code):");
		log.info("ROUTES.put(Difficulty.{}, List.of(", difficulty != null ? difficulty : "UNKNOWN");

		// Output each waypoint in RouteWaypoint format
		for (int i = 0; i < capturedWaypoints.size(); i++)
		{
			RouteWaypoint waypoint = capturedWaypoints.get(i);
			WorldPoint loc = waypoint.getLocation();
			String comma = (i < capturedWaypoints.size() - 1) ? "," : "";

			String comment = "";
			if (waypoint.getType() == RouteWaypoint.WaypointType.RUM_PICKUP)
			{
				comment = " // Pick up rum";
			}
			else if (waypoint.getType() == RouteWaypoint.WaypointType.RUM_DROPOFF)
			{
				comment = " // Drop off rum";
			}

			log.info("\tnew RouteWaypoint(WaypointType.{}, new WorldPoint({}, {}, {})){}{}",
				waypoint.getType(), loc.getX(), loc.getY(), loc.getPlane(), comma, comment);
		}

		log.info("));");
		log.info("");
		log.info("[ROUTE CAPTURE] Total waypoints: {}", capturedWaypoints.size());
		log.info("[ROUTE CAPTURE] Copy the Java code above into TemporTantrumRoutes.java");

		// Reset state
		isCapturing = false;
		hasExaminedPickup = false;
		capturedWaypoints.clear();
		shipmentLocationsForVisualFeedback.clear();
	}

	/**
	 * Format a WorldPoint for logging.
	 */
	private String formatWorldPoint(WorldPoint point)
	{
		return String.format("WorldPoint(%d, %d, %d)", point.getX(), point.getY(), point.getPlane());
	}

	/**
	 * Check if route capture is currently active.
	 */
	public boolean isCapturing()
	{
		return isCapturing;
	}

	/**
	 * Reset capture state (useful when leaving trial area).
	 */
	public void reset()
	{
		isCapturing = false;
		hasExaminedPickup = false;
		capturedWaypoints.clear();
		shipmentLocationsForVisualFeedback.clear();
	}

	/**
	 * Get the list of examined shipment locations.
	 * Used for visual feedback during route capture.
	 */
	public List<WorldPoint> getExaminedShipmentLocations()
	{
		return shipmentLocationsForVisualFeedback;
	}
}
