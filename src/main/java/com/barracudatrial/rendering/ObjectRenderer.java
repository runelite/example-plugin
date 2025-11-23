package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import com.barracudatrial.game.ObjectTracker;
import com.barracudatrial.game.route.RouteWaypoint;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Map<Point, Integer> labelCountsByCanvasPosition;

	public ObjectRenderer(Client client, BarracudaTrialPlugin plugin, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	public void setLabelCounts(Map<Point, Integer> labelCountsByCanvasPosition)
	{
		this.labelCountsByCanvasPosition = labelCountsByCanvasPosition;
	}

	public void renderLostSupplies(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		for (GameObject lostSupplyObject : plugin.getGameState().getLostSupplies())
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(lostSupplyObject, "Lost Supplies");
			}

			// Check if this shipment has been examined during route capture
			Color renderColor = cachedConfig.getLostSuppliesColor();
			if (plugin.getRouteCapture() != null && plugin.getRouteCapture().isCapturing())
			{
				WorldPoint shipmentLocation = lostSupplyObject.getWorldLocation();
				if (plugin.getRouteCapture().getExaminedShipmentLocations().contains(shipmentLocation))
				{
					// Invert RGB to show it's been examined
					renderColor = new Color(
						255 - renderColor.getRed(),
						255 - renderColor.getGreen(),
						255 - renderColor.getBlue(),
						renderColor.getAlpha()
					);
				}
			}

			renderGameObjectWithHighlight(graphics, lostSupplyObject, renderColor, false, debugLabel);
		}
	}

	public void renderRouteCaptureSupplyLocations(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		Color highlightColor = cachedConfig.getLostSuppliesColor();

		// Build a set of lost supply locations for quick lookup
		Set<WorldPoint> lostSupplyLocations = new HashSet<>();
		for (GameObject lostSupply : plugin.getGameState().getLostSupplies())
		{
			lostSupplyLocations.add(lostSupply.getWorldLocation());
		}

		for (WorldPoint supplyLocation : plugin.getGameState().getRouteCaptureSupplyLocations())
		{
			renderTileHighlightAtWorldPoint(graphics, supplyLocation, highlightColor);

			// If showIDs is on and this location doesn't have a lost supply, show the object ID
			if (cachedConfig.isShowIDs() && !lostSupplyLocations.contains(supplyLocation))
			{
				GameObject gameObject = findGameObjectAtWorldPoint(supplyLocation);
				if (gameObject != null)
				{
					String debugLabel = buildObjectLabelWithImpostorInfo(gameObject, "Visible Supply") +
						String.format(" @ (%d, %d)", supplyLocation.getX(), supplyLocation.getY());
					WorldView topLevelWorldView = client.getTopLevelWorldView();
					if (topLevelWorldView != null)
					{
						LocalPoint localPoint = localPointFromWorldIncludingExtended(topLevelWorldView, supplyLocation);
						if (localPoint != null)
						{
							renderLabelAtLocalPoint(graphics, localPoint, debugLabel, highlightColor, 0);
						}
					}
				}
			}
		}
	}

	public void renderLightningClouds(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		for (NPC cloudNpc : plugin.getGameState().getLightningClouds())
		{
			int currentAnimation = cloudNpc.getAnimation();

			boolean isCloudSafe = ObjectTracker.IsCloudSafe(currentAnimation);
			if (isCloudSafe)
			{
				continue;
			}

			Color dangerousCloudColor = cachedConfig.getCloudColor();

			renderCloudDangerAreaOnGround(graphics, cloudNpc, dangerousCloudColor);

			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = String.format("Cloud (ID: %d, Anim: %d)", cloudNpc.getId(), cloudNpc.getAnimation());
			}
			renderNpcWithHighlight(graphics, cloudNpc, dangerousCloudColor, debugLabel);
		}
	}

	public void renderRocks(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		for (GameObject rockObject : plugin.getGameState().getRocks())
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(rockObject, "Rock");
			}
			renderGameObjectWithHighlight(graphics, rockObject, cachedConfig.getRockColor(), true, debugLabel);
		}
	}

	public void renderSpeedBoosts(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		for (GameObject speedBoostObject : plugin.getGameState().getSpeedBoosts())
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(speedBoostObject, "Speed Boost");
			}
			renderGameObjectWithHighlight(graphics, speedBoostObject, cachedConfig.getSpeedBoostColor(), true, debugLabel);
		}
	}

	public void renderRumLocations(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		Color rumHighlightColor = cachedConfig.getRumLocationColor();

		boolean isCarryingRum = plugin.getGameState().isHasRumOnUs();
		WorldPoint targetRumLocation = null;

		if (isCarryingRum)
		{
			targetRumLocation = plugin.getGameState().getRumReturnLocation();
		}
		else
		{
			targetRumLocation = plugin.getGameState().getRumPickupLocation();
		}

		if (targetRumLocation != null)
		{
			// Check if this rum location is the next waypoint
			boolean isNextWaypoint = isRumLocationNextWaypoint(targetRumLocation);

			if (isNextWaypoint)
			{
				// Draw filled rectangle around entire boat exclusion zone
				renderBoatZoneRectangle(graphics, targetRumLocation, rumHighlightColor);
			}
			else
			{
				// Fallback: try to highlight the game object (doesn't work well currently)
				renderRumLocationHighlight(graphics, targetRumLocation, rumHighlightColor);
			}
		}
	}

	private void renderRumLocationHighlight(Graphics2D graphics, WorldPoint rumLocationPoint, Color highlightColor)
	{
		GameObject rumObjectAtLocation = findGameObjectAtWorldPoint(rumLocationPoint);
		if (rumObjectAtLocation != null)
		{
			renderGameObjectWithHighlight(graphics, rumObjectAtLocation, highlightColor, true, null);
		}
		else
		{
			renderTileHighlightAtWorldPoint(graphics, rumLocationPoint, highlightColor);
		}
	}

	private void renderGameObjectWithHighlight(Graphics2D graphics, GameObject gameObject, Color highlightColor, boolean shouldHighlightTile, String debugLabel)
	{
		LocalPoint objectLocalPoint = gameObject.getLocalLocation();

		if (shouldHighlightTile)
		{
			Polygon tilePolygon = Perspective.getCanvasTilePoly(client, objectLocalPoint);
			if (tilePolygon != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
			}
		}

		modelOutlineRenderer.drawOutline(gameObject, 2, highlightColor, 4);

		if (debugLabel != null)
		{
			renderLabelAtLocalPoint(graphics, objectLocalPoint, debugLabel, highlightColor, 0);
		}
	}

	private void renderNpcWithHighlight(Graphics2D graphics, NPC npc, Color highlightColor, String debugLabel)
	{
		LocalPoint npcLocalPoint = npc.getLocalLocation();
		if (npcLocalPoint == null)
		{
			return;
		}

		Polygon tilePolygon = Perspective.getCanvasTilePoly(client, npcLocalPoint);
		if (tilePolygon != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
		}

		modelOutlineRenderer.drawOutline(npc, 2, highlightColor, 4);

		if (debugLabel != null)
		{
			int heightOffsetAboveNpc = npc.getLogicalHeight() + 40;
			renderLabelAtLocalPoint(graphics, npcLocalPoint, debugLabel, highlightColor, heightOffsetAboveNpc);
		}
	}

	private void renderCloudDangerAreaOnGround(Graphics2D graphics, NPC cloudNpc, Color dangerAreaColor)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		LocalPoint cloudCenterPoint = cloudNpc.getLocalLocation();
		if (cloudCenterPoint == null)
		{
			return;
		}

		int dangerRadiusInTiles = cachedConfig.getCloudDangerRadius();

		for (int dx = -dangerRadiusInTiles; dx <= dangerRadiusInTiles; dx++)
		{
			for (int dy = -dangerRadiusInTiles; dy <= dangerRadiusInTiles; dy++)
			{
				boolean isTileWithinCircle = (dx * dx + dy * dy <= dangerRadiusInTiles * dangerRadiusInTiles);
				if (isTileWithinCircle)
				{
					LocalPoint tilePoint = new LocalPoint(
						cloudCenterPoint.getX() + dx * Perspective.LOCAL_TILE_SIZE,
						cloudCenterPoint.getY() + dy * Perspective.LOCAL_TILE_SIZE
					);

					Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tilePoint);
					if (tilePolygon != null)
					{
						Color transparentFillColor = new Color(dangerAreaColor.getRed(), dangerAreaColor.getGreen(), dangerAreaColor.getBlue(), 30);
						graphics.setColor(transparentFillColor);
						graphics.fill(tilePolygon);
						graphics.setColor(dangerAreaColor);
						graphics.draw(tilePolygon);
					}
				}
			}
		}
	}

	void renderTileHighlightAtWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color highlightColor)
	{
		renderTileHighlightAtWorldPoint(graphics, worldPoint, highlightColor, null);
	}

	void renderTileHighlightAtWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color highlightColor, String label)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		LocalPoint tileLocalPoint = localPointFromWorldIncludingExtended(topLevelWorldView, worldPoint);
		if (tileLocalPoint == null)
		{
			return;
		}

		Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tileLocalPoint);
		if (tilePolygon != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
		}

		if (label != null)
		{
			Point labelPoint = Perspective.getCanvasTextLocation(client, graphics, tileLocalPoint, "", 30);
			if (labelPoint != null)
			{
				graphics.setColor(highlightColor);
				graphics.drawString(label, labelPoint.getX(), labelPoint.getY());
			}
		}
	}

	private void renderLabelAtLocalPoint(Graphics2D graphics, LocalPoint localPoint, String labelText, Color labelColor, int heightOffsetInPixels)
	{
		Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, labelText, heightOffsetInPixels);
		if (labelCanvasPoint != null)
		{
			int yOffsetToAvoidLabelOverlap = calculateAndIncrementLabelOffset(labelCanvasPoint);
			Point adjustedCanvasPoint = new Point(labelCanvasPoint.getX(), labelCanvasPoint.getY() + yOffsetToAvoidLabelOverlap);
			OverlayUtil.renderTextLocation(graphics, adjustedCanvasPoint, labelText, labelColor);
		}
	}

	private int calculateAndIncrementLabelOffset(Point canvasPoint)
	{
		Point roundedCanvasPoint = new Point(
			(canvasPoint.getX() / 10) * 10,
			(canvasPoint.getY() / 10) * 10
		);

		int existingLabelCount = labelCountsByCanvasPosition.getOrDefault(roundedCanvasPoint, 0);
		labelCountsByCanvasPosition.put(roundedCanvasPoint, existingLabelCount + 1);

		int pixelsPerLabel = 15;
		return existingLabelCount * pixelsPerLabel;
	}

	private String buildObjectLabelWithImpostorInfo(GameObject gameObject, String typeName)
	{
		ObjectComposition objectComposition = client.getObjectDefinition(gameObject.getId());

		String displayName;
		if (typeName != null)
		{
			displayName = typeName;
		}
		else if (objectComposition != null && objectComposition.getName() != null)
		{
			displayName = objectComposition.getName();
		}
		else
		{
			displayName = "Unknown";
		}

		StringBuilder labelBuilder = new StringBuilder();
		labelBuilder.append(displayName).append(" (ID: ").append(gameObject.getId());

		if (objectComposition != null)
		{
			int[] impostorIds = objectComposition.getImpostorIds();
			boolean hasImpostorIds = (impostorIds != null && impostorIds.length > 0);
			if (hasImpostorIds)
			{
				ObjectComposition impostorComposition = objectComposition.getImpostor();
				if (impostorComposition != null)
				{
					labelBuilder.append(", Imp: ").append(impostorComposition.getId());
				}
			}
		}

		labelBuilder.append(")");
		return labelBuilder.toString();
	}

	private GameObject findGameObjectAtWorldPoint(WorldPoint worldPoint)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return null;
		}

		Scene scene = topLevelWorldView.getScene();
		if (scene == null)
		{
			return null;
		}

		LocalPoint localPoint = localPointFromWorldIncludingExtended(topLevelWorldView, worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile tile = scene.getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
		if (tile == null)
		{
			return null;
		}

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null)
			{
				return gameObject;
			}
		}

		return null;
	}

	private boolean isRumLocationNextWaypoint(WorldPoint rumLocation)
	{
		List<RouteWaypoint> staticRoute = plugin.getGameState().getCurrentStaticRoute();
		if (staticRoute == null || staticRoute.isEmpty())
		{
			return false;
		}

		int nextWaypointIndex = plugin.getGameState().getNextWaypointIndex();
		if (nextWaypointIndex >= staticRoute.size())
		{
			return false;
		}

		com.barracudatrial.game.route.RouteWaypoint nextWaypoint = staticRoute.get(nextWaypointIndex);
		WorldPoint nextWaypointLocation = nextWaypoint.getLocation();

		return nextWaypointLocation != null && nextWaypointLocation.equals(rumLocation);
	}

	private void renderBoatZoneRectangle(Graphics2D graphics, WorldPoint center, Color baseColor)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		int width = com.barracudatrial.game.route.RumLocations.BOAT_EXCLUSION_WIDTH;
		int height = com.barracudatrial.game.route.RumLocations.BOAT_EXCLUSION_HEIGHT;

		int halfWidth = width / 2;
		int halfHeight = height / 2;

		int minX = center.getX() - halfWidth;
		int maxX = center.getX() + halfWidth;
		int minY = center.getY() - halfHeight;
		int maxY = center.getY() + halfHeight;

		Color fillColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 150);

		// Collect all corner points of the rectangle boundary
		Polygon rectangleBoundary = new Polygon();

		// Walk around the perimeter clockwise: bottom-left -> bottom-right -> top-right -> top-left
		// Bottom edge (south)
		for (int x = minX; x <= maxX; x++)
		{
			WorldPoint tile = new WorldPoint(x, minY, 0);
			LocalPoint local = localPointFromWorldIncludingExtended(topLevelWorldView, tile);
			if (local != null)
			{
				Polygon tilePoly = Perspective.getCanvasTilePoly(client, local);
				if (tilePoly != null && tilePoly.npoints >= 4)
				{
					// Add bottom-left and bottom-right corners
					if (x == minX)
					{
						rectangleBoundary.addPoint(tilePoly.xpoints[0], tilePoly.ypoints[0]); // SW corner
					}
					if (x == maxX)
					{
						rectangleBoundary.addPoint(tilePoly.xpoints[1], tilePoly.ypoints[1]); // SE corner
					}
				}
			}
		}

		// Right edge (east)
		for (int y = minY; y <= maxY; y++)
		{
			WorldPoint tile = new WorldPoint(maxX, y, 0);
			LocalPoint local = localPointFromWorldIncludingExtended(topLevelWorldView, tile);
			if (local != null)
			{
				Polygon tilePoly = Perspective.getCanvasTilePoly(client, local);
				if (tilePoly != null && tilePoly.npoints >= 4)
				{
					if (y == maxY)
					{
						rectangleBoundary.addPoint(tilePoly.xpoints[2], tilePoly.ypoints[2]); // NE corner
					}
				}
			}
		}

		// Top edge (north)
		for (int x = maxX; x >= minX; x--)
		{
			WorldPoint tile = new WorldPoint(x, maxY, 0);
			LocalPoint local = localPointFromWorldIncludingExtended(topLevelWorldView, tile);
			if (local != null)
			{
				Polygon tilePoly = Perspective.getCanvasTilePoly(client, local);
				if (tilePoly != null && tilePoly.npoints >= 4)
				{
					if (x == minX)
					{
						rectangleBoundary.addPoint(tilePoly.xpoints[3], tilePoly.ypoints[3]); // NW corner
					}
				}
			}
		}

		if (rectangleBoundary.npoints > 0)
		{
			OverlayUtil.renderPolygon(graphics, rectangleBoundary, fillColor);
		}
	}

	/**
	 * Creates a LocalPoint from a WorldPoint, including support for extended tiles.
	 * LocalPoint.fromWorld() only works for the normal scene, not extended tiles.
	 * This manually creates LocalPoints for extended tiles by calculating scene coordinates.
	 */
	public static LocalPoint localPointFromWorldIncludingExtended(WorldView view, WorldPoint point)
	{
		if (view == null || point == null)
		{
			return null;
		}

		if (view.getPlane() != point.getPlane())
		{
			return null;
		}

		// Try normal method first (works for regular scene)
		LocalPoint normalPoint = LocalPoint.fromWorld(view, point);
		if (normalPoint != null)
		{
			return normalPoint;
		}

		// For extended tiles, manually create LocalPoint from scene coordinates
		int baseX = view.getBaseX();
		int baseY = view.getBaseY();
		int sceneX = point.getX() - baseX;
		int sceneY = point.getY() - baseY;

		// Extended tiles go up to around 192x192, check if within reasonable bounds
		if (sceneX >= -50 && sceneX < 200 && sceneY >= -50 && sceneY < 200)
		{
			return LocalPoint.fromScene(sceneX, sceneY, view);
		}

		return null;
	}
}
