package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
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
		WorldPoint boatLocation = plugin.getGameState().getBoatLocation();
		if (boatLocation == null)
		{
			return;
		}

		List<WorldPoint> currentSegmentPath = plugin.getGameState().getCurrentSegmentPath();
		if (currentSegmentPath.isEmpty())
		{
			return;
		}

		if (cachedConfig.isDebugMode())
		{
			drawDebugWaypointLines(graphics, currentSegmentPath, boatLocation);
		}

		int totalSegmentsInPath = calculateTotalSegmentsInPath(currentSegmentPath, boatLocation);

		drawInterpolatedPathWithTracer(graphics, currentSegmentPath, boatLocation, totalSegmentsInPath, frameCounterForTracerAnimation);
	}

	private void drawDebugWaypointLines(Graphics2D graphics, List<WorldPoint> waypoints, WorldPoint startLocation)
	{
		WorldPoint previousWaypoint = startLocation;
		for (WorldPoint waypoint : waypoints)
		{
			drawStraightLineBetweenPoints(graphics, previousWaypoint, waypoint, new Color(255, 255, 0, 150));
			previousWaypoint = waypoint;
		}
	}

	private void drawInterpolatedPathWithTracer(Graphics2D graphics, List<WorldPoint> waypoints, WorldPoint startLocation, int totalSegmentsInPath, int frameCounterForTracerAnimation)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		int globalSegmentOffset = 0;
		WorldPoint previousWaypoint = startLocation;
		for (WorldPoint waypoint : waypoints)
		{
			globalSegmentOffset = drawInterpolatedSegment(graphics, previousWaypoint, waypoint, cachedConfig.getPathColor(), totalSegmentsInPath, globalSegmentOffset, frameCounterForTracerAnimation);
			previousWaypoint = waypoint;
		}
	}

	private int calculateTotalSegmentsInPath(List<WorldPoint> waypoints, WorldPoint startLocation)
	{
		int totalSegmentCount = 0;
		WorldPoint previousWaypoint = startLocation;
		for (WorldPoint waypoint : waypoints)
		{
			List<WorldPoint> interpolatedPoints = interpolateLineBetweenPoints(previousWaypoint, waypoint);
			totalSegmentCount += Math.max(0, interpolatedPoints.size() - 1);
			previousWaypoint = waypoint;
		}
		return totalSegmentCount;
	}

	private void drawStraightLineBetweenPoints(Graphics2D graphics, WorldPoint fromPoint, WorldPoint toPoint, Color lineColor)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		if (fromPoint == null || toPoint == null || fromPoint.getPlane() != toPoint.getPlane())
		{
			return;
		}

		LocalPoint fromLocalPoint = LocalPoint.fromWorld(topLevelWorldView, fromPoint);
		LocalPoint toLocalPoint = LocalPoint.fromWorld(topLevelWorldView, toPoint);

		if (fromLocalPoint == null || toLocalPoint == null)
		{
			return;
		}

		Point fromCanvasPoint = Perspective.localToCanvas(client, fromLocalPoint, fromPoint.getPlane(), 0);
		Point toCanvasPoint = Perspective.localToCanvas(client, toLocalPoint, toPoint.getPlane(), 0);

		if (fromCanvasPoint == null || toCanvasPoint == null)
		{
			return;
		}

		graphics.setColor(lineColor);
		graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(fromCanvasPoint.getX(), fromCanvasPoint.getY(), toCanvasPoint.getX(), toCanvasPoint.getY());
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
}
