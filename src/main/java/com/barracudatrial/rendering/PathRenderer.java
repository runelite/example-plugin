package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.pathfinding.BarracudaTileCostCalculator;
import lombok.RequiredArgsConstructor;
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
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PathRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final ObjectRenderer objectRenderer;

	public void renderOptimalPath(Graphics2D graphics)
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

		// Trim the path to start from the closest point to our visual position
		// This prevents visual lag when the pathfinding position is behind the rendering position
		List<WorldPoint> trimmedPath = getTrimmedPathForRendering(visualFrontPositionTransformed, currentSegmentPath);

		drawSmoothPathWithBezier(graphics, trimmedPath, visualFrontPositionTransformed);

		// Render debug visualizations
		if (cachedConfig.isDebugMode())
		{
			renderPathTiles(graphics, currentSegmentPath);
			renderWaypointLabels(graphics);
		}
	}

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

		return boatWorldEntity.transformToMainWorld(frontBoatTileLocal);
	}

	private List<WorldPoint> getTrimmedPathForRendering(LocalPoint visualPosition, List<WorldPoint> path)
	{
		if (path.isEmpty() || visualPosition == null)
		{
			return path;
		}

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return path;
		}

		int closestIndex = findClosestPointOnPath(visualPosition, path, topLevelWorldView);

		// Step forward along the path to bias toward showing "forward progress"
		// This prevents the path from appearing to start "alongside" when moving fast
		int forwardBiasOffset = 2;
		int startIndex = Math.min(path.size() - 1, closestIndex + forwardBiasOffset);

		if (startIndex >= path.size())
		{
			return new ArrayList<>();
		}

		return new ArrayList<>(path.subList(startIndex, path.size()));
	}

	private int findClosestPointOnPath(LocalPoint visualPosition, List<WorldPoint> path, WorldView worldView)
	{
		int closestIndex = 0;
		double minDistance = Double.POSITIVE_INFINITY;

		for (int i = 0; i < path.size(); i++)
		{
			LocalPoint pathPointLocal = ObjectRenderer.localPointFromWorldIncludingExtended(worldView, path.get(i));
			if (pathPointLocal == null)
			{
				continue;
			}

			int dx = visualPosition.getX() - pathPointLocal.getX();
			int dy = visualPosition.getY() - pathPointLocal.getY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			if (distance < minDistance)
			{
				minDistance = distance;
				closestIndex = i;
			}
		}

		return closestIndex;
	}

	private void drawSmoothPathWithBezier(Graphics2D graphics, List<WorldPoint> waypoints, LocalPoint visualStartPosition)
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

		// Convert visual start position to canvas coordinates
		Point startCanvas = Perspective.localToCanvas(client, visualStartPosition, client.getPlane(), 0);
		if (startCanvas == null)
		{
			return;
		}

		// Create Path2D for smooth Bézier curves
		Path2D.Double path = new Path2D.Double();
		path.moveTo(startCanvas.getX(), startCanvas.getY());

		// Convert all waypoints to canvas coordinates
		List<Point> canvasPoints = new ArrayList<>();
		for (WorldPoint wp : waypoints)
		{
			LocalPoint lp = ObjectRenderer.localPointFromWorldIncludingExtended(topLevelWorldView, wp);
			if (lp != null)
			{
				Point cp = Perspective.localToCanvas(client, lp, wp.getPlane(), 0);
				if (cp != null)
				{
					canvasPoints.add(cp);
				}
			}
		}

		if (canvasPoints.isEmpty())
		{
			return;
		}

		// Draw line from boat to first waypoint
		path.lineTo(canvasPoints.get(0).getX(), canvasPoints.get(0).getY());

		// Draw smooth Bézier curves through waypoints
		if (canvasPoints.size() == 1)
		{
			// Just one point, already connected above
		}
		else if (canvasPoints.size() == 2)
		{
			// Two points - just draw a line
			path.lineTo(canvasPoints.get(1).getX(), canvasPoints.get(1).getY());
		}
		else
		{
			// Three or more points - use Bézier curves
			for (int i = 0; i < canvasPoints.size() - 1; i++)
			{
				Point p0 = i > 0 ? canvasPoints.get(i - 1) : canvasPoints.get(i);
				Point p1 = canvasPoints.get(i);
				Point p2 = canvasPoints.get(i + 1);
				Point p3 = (i + 2 < canvasPoints.size()) ? canvasPoints.get(i + 2) : p2;

				// Calculate control points for cubic Bezier
				// Using a tension factor to control curve smoothness
				double tension = 0.1;

				double cp1x = p1.getX() + (p2.getX() - p0.getX()) * tension;
				double cp1y = p1.getY() + (p2.getY() - p0.getY()) * tension;

				double cp2x = p2.getX() - (p3.getX() - p1.getX()) * tension;
				double cp2y = p2.getY() - (p3.getY() - p1.getY()) * tension;

				path.curveTo(cp1x, cp1y, cp2x, cp2y, p2.getX(), p2.getY());
			}
		}

		// Draw the path
		graphics.setStroke(new BasicStroke(cachedConfig.getPathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(cachedConfig.getPathColor());
		graphics.draw(path);
	}

	private void renderPathTiles(Graphics2D graphics, List<WorldPoint> path)
	{
		if (path == null || path.isEmpty())
		{
			return;
		}

		var boostLocations = plugin.getGameState().getKnownSpeedBoostLocations();

		Set<WorldPoint> allBoostTiles = boostLocations.values()
				.stream()
				.flatMap(List::stream)
				.collect(Collectors.toSet());

		Color normalTileColor = new Color(255, 255, 0, 80);
		Color boostTileColor = new Color(135, 206, 250, 100);

		for (WorldPoint pathTile : path)
		{
			boolean isBoostTile = allBoostTiles.contains(pathTile);
			Color tileColor = isBoostTile ? boostTileColor : normalTileColor;
			String label = isBoostTile ? "Boost!" : null;

			objectRenderer.renderTileHighlightAtWorldPoint(graphics, pathTile, tileColor, label);
		}
	}

	private void renderWaypointLabels(Graphics2D graphics)
	{
		List<RouteWaypoint> staticRoute = plugin.getGameState().getCurrentStaticRoute();
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (staticRoute == null || topLevelWorldView == null) return;

		graphics.setColor(Color.WHITE);
		int index = 0;
		for (RouteWaypoint waypoint : staticRoute)
		{
			WorldPoint loc = waypoint.getLocation();
			if (loc != null)
			{
				LocalPoint lp = ObjectRenderer.localPointFromWorldIncludingExtended(topLevelWorldView, loc);
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
