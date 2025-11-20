package com.barracudatrial;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class BarracudaTrialPathRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final BarracudaTrialConfig config;

	public BarracudaTrialPathRenderer(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
	}

	public void renderOptimalPath(Graphics2D graphics, int frameCounter)
	{
		WorldPoint boatLocation = plugin.getBoatLocation();
		if (boatLocation == null)
		{
			return;
		}

		List<WorldPoint> currentSegment = plugin.getCurrentSegmentPath();
		if (currentSegment.isEmpty())
		{
			return;
		}

		// Debug mode: draw simple yellow lines showing waypoint-to-waypoint connections
		if (config.debugMode())
		{
			WorldPoint previous = boatLocation;
			for (WorldPoint point : currentSegment)
			{
				drawSimpleLine(graphics, previous, point, new Color(255, 255, 0, 150));
				previous = point;
			}
		}

		// Calculate total segments for tracer animation
		int totalSegments = calculateTotalSegments(currentSegment, boatLocation);

		// Draw interpolated path with optional tracer
		int segmentOffset = 0;
		WorldPoint previousPoint = boatLocation;
		for (WorldPoint point : currentSegment)
		{
			segmentOffset = drawPathSegment(graphics, previousPoint, point, config.pathColor(), totalSegments, segmentOffset, frameCounter);
			previousPoint = point;
		}
	}

	private int calculateTotalSegments(List<WorldPoint> path, WorldPoint startPoint)
	{
		int totalSegments = 0;
		WorldPoint previousPoint = startPoint;
		for (WorldPoint point : path)
		{
			List<WorldPoint> interpolated = interpolateLine(previousPoint, point);
			totalSegments += Math.max(0, interpolated.size() - 1);
			previousPoint = point;
		}
		return totalSegments;
	}

	private void drawSimpleLine(Graphics2D graphics, WorldPoint from, WorldPoint to, Color color)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		if (from == null || to == null || from.getPlane() != to.getPlane())
		{
			return;
		}

		LocalPoint fromLocal = LocalPoint.fromWorld(topLevelWorldView, from);
		LocalPoint toLocal = LocalPoint.fromWorld(topLevelWorldView, to);

		if (fromLocal == null || toLocal == null)
		{
			return;
		}

		net.runelite.api.Point fromCanvas = Perspective.localToCanvas(client, fromLocal, from.getPlane(), 0);
		net.runelite.api.Point toCanvas = Perspective.localToCanvas(client, toLocal, to.getPlane(), 0);

		if (fromCanvas == null || toCanvas == null)
		{
			return;
		}

		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(fromCanvas.getX(), fromCanvas.getY(), toCanvas.getX(), toCanvas.getY());
	}

	private int drawPathSegment(Graphics2D graphics, WorldPoint from, WorldPoint to, Color pathColor, int totalSegments, int segmentOffset, int frameCounter)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return segmentOffset;
		}

		if (from == null || to == null)
		{
			return segmentOffset;
		}

		if (from.equals(new WorldPoint(0, 0, 0)) || to.equals(new WorldPoint(0, 0, 0)))
		{
			return segmentOffset;
		}

		if (from.getPlane() != to.getPlane())
		{
			return segmentOffset;
		}

		List<WorldPoint> interpolated = interpolateLine(from, to);
		if (interpolated.isEmpty())
		{
			return segmentOffset;
		}

		int segmentCount = interpolated.size() - 1;
		if (segmentCount <= 0)
		{
			return segmentOffset;
		}

		boolean tracerEnabled = config.showPathTracer() && pathColor.equals(config.pathColor());
		int globalPulsePosition = tracerEnabled && totalSegments > 0 ? (frameCounter % totalSegments) : -1;

		graphics.setStroke(new BasicStroke(config.pathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		for (int j = 0; j < segmentCount; j++)
		{
			WorldPoint wp1 = interpolated.get(j);
			WorldPoint wp2 = interpolated.get(j + 1);

			LocalPoint lp1 = LocalPoint.fromWorld(topLevelWorldView, wp1);
			LocalPoint lp2 = LocalPoint.fromWorld(topLevelWorldView, wp2);

			if (lp1 == null || lp2 == null)
			{
				continue;
			}

			net.runelite.api.Point p1 = Perspective.localToCanvas(client, lp1, wp1.getPlane(), 0);
			net.runelite.api.Point p2 = Perspective.localToCanvas(client, lp2, wp2.getPlane(), 0);

			if (p1 == null || p2 == null)
			{
				continue;
			}

			// Apply tracer effect
			int globalSegmentIndex = segmentOffset + j;
			Color segmentColor = (globalSegmentIndex == globalPulsePosition) ? config.tracerColor() : pathColor;

			graphics.setColor(segmentColor);
			graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
		}

		return segmentOffset + segmentCount;
	}

	private List<WorldPoint> interpolateLine(WorldPoint start, WorldPoint end)
	{
		List<WorldPoint> result = new ArrayList<>();

		// Prevent excessive memory usage from bad data
		int distance = start.distanceTo(end);
		if (distance > 500)
		{
			return result;
		}

		int steps = Math.max(distance, 1);

		for (int i = 0; i <= steps; i++)
		{
			double t = i / (double) steps;
			int x = (int) Math.round(lerp(start.getX(), end.getX(), t));
			int y = (int) Math.round(lerp(start.getY(), end.getY(), t));
			int plane = start.getPlane();
			result.add(new WorldPoint(x, y, plane));
		}

		return result;
	}

	private double lerp(int a, int b, double t)
	{
		return a + (b - a) * t;
	}
}
