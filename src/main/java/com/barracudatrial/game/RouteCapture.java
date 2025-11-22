package com.barracudatrial.game;

import com.barracudatrial.game.route.Difficulty;
import com.barracudatrial.game.route.RouteWaypoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages route capture for static route data collection.
 * When debug mode is enabled, this class tracks pickups via chat messages
 * and logs WorldPoint coordinates for route generation.
 *
 * Workflow:
 * 1. Examine Rum Dropoff (north) - starts capture
 * 2. Pick up Lost Shipments in desired order - logs each shipment location
 * 3. Pick up Rum - logs RUM_PICKUP waypoint
 * 4. Continue picking up remaining shipments for subsequent laps
 * 5. Deliver Rum - completes capture automatically
 *
 * Output format is designed for easy copy/paste into TemporTantrumRoutes.java
 */
@Slf4j
public class RouteCapture
{
	private boolean isCapturing = false;
	private final List<RouteWaypoint> capturedWaypoints = new ArrayList<>();
	private final List<WorldPoint> shipmentLocationsForVisualFeedback = new ArrayList<>();
	private final State state;

	public RouteCapture(State state)
	{
		this.state = state;
	}

	/**
	 * Handle examine action on Rum Dropoff (north location).
	 * Starts or restarts route capture.
	 */
	public void onExamineRumDropoff(WorldPoint location, int sceneX, int sceneY, int sceneBaseX, int sceneBaseY, int objectId, String impostorInfo)
	{
		log.info("[ROUTE CAPTURE] Rum Dropoff examined:");
		log.info("[ROUTE CAPTURE]   ObjectID: {}, ImpostorID: {}", objectId, impostorInfo);
		log.info("[ROUTE CAPTURE]   SceneXY: ({}, {}), SceneBase: ({}, {})", sceneX, sceneY, sceneBaseX, sceneBaseY);
		log.info("[ROUTE CAPTURE]   Boat WorldPoint (REAL): {}", formatWorldPoint(location));
		log.info("[ROUTE CAPTURE]   Plane: {}", location.getPlane());
		log.info("[ROUTE CAPTURE]   State.rumReturnLocation: {}",
			state.getRumReturnLocation() != null ? formatWorldPoint(state.getRumReturnLocation()) : "null");

		if (isCapturing)
		{
			log.info("[ROUTE CAPTURE] RESET - Starting over");
		}
		startCapture(location);
	}

	/**
	 * Records shipments that were collected during route capture.
	 * Called with the list of collected shipments detected by ObjectTracker.
	 */
	public void onShipmentsCollected(List<WorldPoint> collectedShipments)
	{
		if (!isCapturing)
		{
			return;
		}

		for (WorldPoint collectedLocation : collectedShipments)
		{
			var alreadyCollected = false;
			for (var existingWaypoint : capturedWaypoints)
			{
				if (existingWaypoint.getType() != RouteWaypoint.WaypointType.SHIPMENT)
					continue;

				var location = existingWaypoint.getLocation();

				if (location.getX() == collectedLocation.getX()
						&& location.getY() == collectedLocation.getY()
						&& location.getPlane() == collectedLocation.getPlane())
				{
					alreadyCollected = true;
					break;
				}
			}

			if (alreadyCollected)
			{
				continue;
			}
			
			capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.SHIPMENT, collectedLocation));
			shipmentLocationsForVisualFeedback.add(collectedLocation);
			int waypointNumber = capturedWaypoints.size();
			log.info("[ROUTE CAPTURE] Shipment #{}: {}", waypointNumber, formatWorldPoint(collectedLocation));
		}
	}

	/**
	 * Handle rum pickup detected via chat message.
	 */
	public void onRumPickedUp()
	{
		if (!isCapturing)
		{
			return;
		}

		capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.RUM_PICKUP));
		log.info("[ROUTE CAPTURE] Rum pickup recorded (waypoint #{})", capturedWaypoints.size());
	}

	/**
	 * Handle rum delivery detected via chat message.
	 * Completes the route capture.
	 */
	public void onRumDelivered(boolean isCompletingFinalLap)
	{
		if (!isCapturing)
		{
			return;
		}

		capturedWaypoints.add(new RouteWaypoint(RouteWaypoint.WaypointType.RUM_DROPOFF));
		log.info("[ROUTE CAPTURE] Rum dropoff recorded (waypoint #{})", capturedWaypoints.size());

		if (isCompletingFinalLap)
		{
			completeCapture();
		}
	}

	/**
	 * Start a new route capture session.
	 */
	private void startCapture(WorldPoint dropoffLocation)
	{
		isCapturing = true;
		capturedWaypoints.clear();
		shipmentLocationsForVisualFeedback.clear();

		log.info("[ROUTE CAPTURE] ========================================");
		log.info("[ROUTE CAPTURE] STARTED - Pick up shipments in your desired order");
		log.info("[ROUTE CAPTURE] Starting location (Rum Dropoff): {}", formatWorldPoint(dropoffLocation));
		log.info("[ROUTE CAPTURE] ========================================");
	}

	/**
	 * Complete the route capture and output formatted results.
	 */
	private void completeCapture()
	{
		log.info("");
		log.info("[ROUTE CAPTURE] ========================================");
		log.info("[ROUTE CAPTURE] COMPLETE - Route captured successfully!");
		log.info("[ROUTE CAPTURE] ========================================");
		log.info("");
		log.info("Trial: Tempor Tantrum");

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
		log.info("=== COPY THIS JAVA CODE INTO TemporTantrumRoutes.java ===");
		log.info("");
		log.info("ROUTES.put(Difficulty.{}, List.of(", difficulty != null ? difficulty : "UNKNOWN");

		for (int i = 0; i < capturedWaypoints.size(); i++)
		{
			RouteWaypoint waypoint = capturedWaypoints.get(i);
			String comma = (i < capturedWaypoints.size() - 1) ? "," : "";

			if (waypoint.getType() == RouteWaypoint.WaypointType.SHIPMENT)
			{
				WorldPoint loc = waypoint.getLocation();
				log.info("\tnew RouteWaypoint(WaypointType.{}, new WorldPoint({}, {}, {})){}",
					waypoint.getType(), loc.getX(), loc.getY(), loc.getPlane(), comma);
			}
			else
			{
				log.info("\tnew RouteWaypoint(WaypointType.{}){}",
					waypoint.getType(), comma);
			}
		}

		log.info("));");
		log.info("");
		log.info("Total waypoints: {}", capturedWaypoints.size());
		log.info("=========================================================");

		isCapturing = false;
		capturedWaypoints.clear();
		shipmentLocationsForVisualFeedback.clear();
	}

	/**
	 * Format a WorldPoint for logging.
	 */
	public static String formatWorldPoint(WorldPoint point)
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
