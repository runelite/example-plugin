package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class PathRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;

	public PathRenderer(Client client, BarracudaTrialPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
	}

	public void renderOptimalPath(Graphics2D graphics, int frameCounterForTracerAnimation)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		List<WorldPoint> currentSegmentPath = plugin.getGameState().getCurrentSegmentPath();
		if (currentSegmentPath.isEmpty())
		{
			return;
		}

		// Get the visual front position transformed to main world coordinates
		// This preserves sub-tile accuracy while being in the correct coordinate system for interpolation
		LocalPoint visualFrontPositionTransformed = getTransformedFrontPosition();
		if (visualFrontPositionTransformed == null)
		{
			return;
		}

		int totalSegmentsInPath = calculateTotalSegmentsInPath(currentSegmentPath, visualFrontPositionTransformed);

		drawInterpolatedPathWithTracer(graphics, currentSegmentPath, visualFrontPositionTransformed, totalSegmentsInPath, frameCounterForTracerAnimation);

		// Render waypoint labels in debug mode
		if (cachedConfig.isDebugMode())
		{
			renderWaypointLabels(graphics);
		}
	}

	/**
	 * Gets the boat-relative front position and transforms it to main world coordinates.
	 * This preserves sub-tile accuracy while ensuring compatibility with world-space waypoints.
	 */
	private LocalPoint getTransformedFrontPosition()
	{
		LocalPoint frontBoatTileLocal = plugin.getGameState().getFrontBoatTileLocal();
		if (frontBoatTileLocal == null)
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		WorldView playerWorldView = localPlayer.getWorldView();
		if (playerWorldView == null)
		{
			return null;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return null;
		}

		int playerWorldViewId = playerWorldView.getId();
		WorldEntity boatWorldEntity = topLevelWorldView.worldEntities().byIndex(playerWorldViewId);
		if (boatWorldEntity == null)
		{
			return null;
		}

		// Transform from boat-relative to main world coordinates (preserves sub-tile accuracy)
		return boatWorldEntity.transformToMainWorld(frontBoatTileLocal);
	}

	private void drawInterpolatedPathWithTracer(Graphics2D graphics, List<WorldPoint> waypoints, LocalPoint visualStartPosition, int totalSegmentsInPath, int frameCounterForTracerAnimation)
	{
		if (waypoints.isEmpty() || visualStartPosition == null)
		{
			return;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		CachedConfig cachedConfig = plugin.getCachedConfig();
		int globalSegmentOffset = 0;

		// First segment: visual front to first waypoint
		LocalPoint firstWaypointLocal = LocalPoint.fromWorld(topLevelWorldView, waypoints.get(0));
		if (firstWaypointLocal != null)
		{
			globalSegmentOffset = drawInterpolatedSegmentFromLocal(graphics, visualStartPosition, firstWaypointLocal, cachedConfig.getPathColor(), totalSegmentsInPath, globalSegmentOffset, frameCounterForTracerAnimation);
		}

		// Remaining segments: between waypoints
		for (int i = 1; i < waypoints.size(); i++)
		{
			WorldPoint previousWaypoint = waypoints.get(i - 1);
			WorldPoint waypoint = waypoints.get(i);
			globalSegmentOffset = drawInterpolatedSegment(graphics, previousWaypoint, waypoint, cachedConfig.getPathColor(), totalSegmentsInPath, globalSegmentOffset, frameCounterForTracerAnimation);
		}
	}

	private int calculateTotalSegmentsInPath(List<WorldPoint> waypoints, LocalPoint visualStartPosition)
	{
		if (waypoints.isEmpty() || visualStartPosition == null)
		{
			return 0;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return 0;
		}

		int totalSegmentCount = 0;

		// Count segments from visual front to first waypoint
		LocalPoint firstWaypointLocal = LocalPoint.fromWorld(topLevelWorldView, waypoints.get(0));
		if (firstWaypointLocal != null)
		{
			List<LocalPoint> interpolatedPoints = interpolateLineBetweenLocalPoints(visualStartPosition, firstWaypointLocal);
			totalSegmentCount += Math.max(0, interpolatedPoints.size() - 1);
		}

		// Count segments between waypoints
		for (int i = 1; i < waypoints.size(); i++)
		{
			WorldPoint previousWaypoint = waypoints.get(i - 1);
			WorldPoint waypoint = waypoints.get(i);
			List<WorldPoint> interpolatedPoints = interpolateLineBetweenPoints(previousWaypoint, waypoint);
			totalSegmentCount += Math.max(0, interpolatedPoints.size() - 1);
		}

		return totalSegmentCount;
	}

	private int drawInterpolatedSegment(Graphics2D graphics, WorldPoint fromPoint, WorldPoint toPoint, Color pathColor, int totalSegmentsInPath, int globalSegmentOffset, int frameCounterForTracerAnimation)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return globalSegmentOffset;
		}

		if (fromPoint == null || toPoint == null)
		{
			return globalSegmentOffset;
		}

		if (fromPoint.equals(new WorldPoint(0, 0, 0)) || toPoint.equals(new WorldPoint(0, 0, 0)))
		{
			return globalSegmentOffset;
		}

		if (fromPoint.getPlane() != toPoint.getPlane())
		{
			return globalSegmentOffset;
		}

		List<WorldPoint> interpolatedPoints = interpolateLineBetweenPoints(fromPoint, toPoint);
		if (interpolatedPoints.isEmpty())
		{
			return globalSegmentOffset;
		}

		int segmentCountInThisLine = interpolatedPoints.size() - 1;
		if (segmentCountInThisLine <= 0)
		{
			return globalSegmentOffset;
		}

		boolean isTracerEnabled = cachedConfig.isShowPathTracer() && pathColor.equals(cachedConfig.getPathColor());
		int globalTracerPosition = isTracerEnabled && totalSegmentsInPath > 0 ? (frameCounterForTracerAnimation % totalSegmentsInPath) : -1;

		graphics.setStroke(new BasicStroke(cachedConfig.getPathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		for (int i = 0; i < segmentCountInThisLine; i++)
		{
			WorldPoint segmentStartPoint = interpolatedPoints.get(i);
			WorldPoint segmentEndPoint = interpolatedPoints.get(i + 1);

			LocalPoint segmentStartLocal = LocalPoint.fromWorld(topLevelWorldView, segmentStartPoint);
			LocalPoint segmentEndLocal = LocalPoint.fromWorld(topLevelWorldView, segmentEndPoint);

			if (segmentStartLocal == null || segmentEndLocal == null)
			{
				continue;
			}

			Point segmentStartCanvas = Perspective.localToCanvas(client, segmentStartLocal, segmentStartPoint.getPlane(), 0);
			Point segmentEndCanvas = Perspective.localToCanvas(client, segmentEndLocal, segmentEndPoint.getPlane(), 0);

			if (segmentStartCanvas == null || segmentEndCanvas == null)
			{
				continue;
			}

			int globalSegmentIndexForAnimation = globalSegmentOffset + i;
			Color segmentColor = (globalSegmentIndexForAnimation == globalTracerPosition) ? cachedConfig.getTracerColor() : pathColor;

			graphics.setColor(segmentColor);
			graphics.drawLine(segmentStartCanvas.getX(), segmentStartCanvas.getY(), segmentEndCanvas.getX(), segmentEndCanvas.getY());
		}

		return globalSegmentOffset + segmentCountInThisLine;
	}

	private List<WorldPoint> interpolateLineBetweenPoints(WorldPoint startPoint, WorldPoint endPoint)
	{
		List<WorldPoint> interpolatedPoints = new ArrayList<>();

		int distanceBetweenPoints = startPoint.distanceTo(endPoint);
		if (distanceBetweenPoints > 500)
		{
			return interpolatedPoints;
		}

		int interpolationSteps = Math.max(distanceBetweenPoints, 1);

		for (int i = 0; i <= interpolationSteps; i++)
		{
			double interpolationRatio = i / (double) interpolationSteps;
			int interpolatedX = (int) Math.round(linearInterpolate(startPoint.getX(), endPoint.getX(), interpolationRatio));
			int interpolatedY = (int) Math.round(linearInterpolate(startPoint.getY(), endPoint.getY(), interpolationRatio));
			int plane = startPoint.getPlane();
			interpolatedPoints.add(new WorldPoint(interpolatedX, interpolatedY, plane));
		}

		return interpolatedPoints;
	}

	private double linearInterpolate(int startValue, int endValue, double ratio)
	{
		return startValue + (endValue - startValue) * ratio;
	}

	private void drawStraightLineBetweenLocalPoints(Graphics2D graphics, LocalPoint fromLocalPoint, LocalPoint toLocalPoint, Color lineColor)
	{
		if (fromLocalPoint == null || toLocalPoint == null)
		{
			return;
		}

		Point fromCanvasPoint = Perspective.localToCanvas(client, fromLocalPoint, client.getPlane(), 0);
		Point toCanvasPoint = Perspective.localToCanvas(client, toLocalPoint, client.getPlane(), 0);

		if (fromCanvasPoint == null || toCanvasPoint == null)
		{
			return;
		}

		graphics.setColor(lineColor);
		graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(fromCanvasPoint.getX(), fromCanvasPoint.getY(), toCanvasPoint.getX(), toCanvasPoint.getY());
	}

	private List<LocalPoint> interpolateLineBetweenLocalPoints(LocalPoint startPoint, LocalPoint endPoint)
	{
		List<LocalPoint> interpolatedPoints = new ArrayList<>();

		if (startPoint == null || endPoint == null)
		{
			return interpolatedPoints;
		}

		int dx = endPoint.getX() - startPoint.getX();
		int dy = endPoint.getY() - startPoint.getY();
		int distanceBetweenPoints = (int) Math.sqrt(dx * dx + dy * dy) / 128; // Convert to tiles

		if (distanceBetweenPoints > 500)
		{
			return interpolatedPoints;
		}

		int interpolationSteps = Math.max(distanceBetweenPoints, 1);

		for (int i = 0; i <= interpolationSteps; i++)
		{
			double interpolationRatio = i / (double) interpolationSteps;
			int interpolatedX = (int) Math.round(linearInterpolate(startPoint.getX(), endPoint.getX(), interpolationRatio));
			int interpolatedY = (int) Math.round(linearInterpolate(startPoint.getY(), endPoint.getY(), interpolationRatio));
			interpolatedPoints.add(new LocalPoint(interpolatedX, interpolatedY));
		}

		return interpolatedPoints;
	}

	private int drawInterpolatedSegmentFromLocal(Graphics2D graphics, LocalPoint fromLocalPoint, LocalPoint toLocalPoint, Color pathColor, int totalSegmentsInPath, int globalSegmentOffset, int frameCounterForTracerAnimation)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();

		if (fromLocalPoint == null || toLocalPoint == null)
		{
			return globalSegmentOffset;
		}

		List<LocalPoint> interpolatedPoints = interpolateLineBetweenLocalPoints(fromLocalPoint, toLocalPoint);
		if (interpolatedPoints.isEmpty())
		{
			return globalSegmentOffset;
		}

		int segmentCountInThisLine = interpolatedPoints.size() - 1;
		if (segmentCountInThisLine <= 0)
		{
			return globalSegmentOffset;
		}

		boolean isTracerEnabled = cachedConfig.isShowPathTracer() && pathColor.equals(cachedConfig.getPathColor());
		int globalTracerPosition = isTracerEnabled && totalSegmentsInPath > 0 ? (frameCounterForTracerAnimation % totalSegmentsInPath) : -1;

		graphics.setStroke(new BasicStroke(cachedConfig.getPathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		for (int i = 0; i < segmentCountInThisLine; i++)
		{
			LocalPoint segmentStartLocal = interpolatedPoints.get(i);
			LocalPoint segmentEndLocal = interpolatedPoints.get(i + 1);

			Point segmentStartCanvas = Perspective.localToCanvas(client, segmentStartLocal, client.getPlane(), 0);
			Point segmentEndCanvas = Perspective.localToCanvas(client, segmentEndLocal, client.getPlane(), 0);

			if (segmentStartCanvas == null || segmentEndCanvas == null)
			{
				continue;
			}

			int globalSegmentIndexForAnimation = globalSegmentOffset + i;
			Color segmentColor = (globalSegmentIndexForAnimation == globalTracerPosition) ? cachedConfig.getTracerColor() : pathColor;

			graphics.setColor(segmentColor);
			graphics.drawLine(segmentStartCanvas.getX(), segmentStartCanvas.getY(), segmentEndCanvas.getX(), segmentEndCanvas.getY());
		}

		return globalSegmentOffset + segmentCountInThisLine;
	}

	private void renderWaypointLabels(Graphics2D graphics)
	{
		List<com.barracudatrial.game.route.RouteWaypoint> staticRoute = plugin.getGameState().getCurrentStaticRoute();
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (staticRoute == null || topLevelWorldView == null) return;

		graphics.setColor(Color.WHITE);
		int index = 0;
		for (com.barracudatrial.game.route.RouteWaypoint waypoint : staticRoute)
		{
			WorldPoint loc = waypoint.getLocation();
			if (loc != null)
			{
				LocalPoint lp = LocalPoint.fromWorld(topLevelWorldView, loc);
				Point cp = lp != null ? Perspective.getCanvasTextLocation(client, graphics, lp, "", 20) : null;
				if (cp != null)
				{
					graphics.drawString(String.format("#%d %s @ (%d, %d)", index, waypoint.getType(), loc.getX(), loc.getY()), cp.getX(), cp.getY());
				}
			}
			index++;
		}
	}
}
