package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import com.barracudatrial.game.ObjectTracker;
import com.barracudatrial.game.route.JubblyJiveConfig;
import com.barracudatrial.game.route.RouteWaypoint;
import com.barracudatrial.game.route.TemporTantrumConfig;
import com.barracudatrial.game.route.TrialType;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ObjectRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Setter
	private Map<Point, Integer> labelCountsByCanvasPosition;

	public ObjectRenderer(Client client, BarracudaTrialPlugin plugin, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	public void renderLostSupplies(Graphics2D graphics)
	{
		var cachedConfig = plugin.getCachedConfig();
		var gameState = plugin.getGameState();
		var lostSupplies = gameState.getLostSupplies();
		var route = gameState.getCurrentStaticRoute();
		var currentLap = gameState.getCurrentLap();
		var completedWaypointIndices = gameState.getCompletedWaypointIndices();

		Set<WorldPoint> allRouteLocations = Collections.emptySet();
		Set<WorldPoint> laterLapLocations = Collections.emptySet();
		WorldPoint currentWaypointLocation = null;

		if (route != null && !route.isEmpty())
		{
			allRouteLocations = new HashSet<>(route.size());
			laterLapLocations = new HashSet<>(route.size());

			for (int i = 0; i < route.size(); i++)
			{
				var waypoint = route.get(i);
				var location = waypoint.getLocation();

				allRouteLocations.add(location);

				if (currentLap != waypoint.getLap())
				{
					laterLapLocations.add(location);
				}
				else if (currentWaypointLocation == null && !completedWaypointIndices.contains(i))
				{
					currentWaypointLocation = location;
				}
			}
		}

		for (var lostSupplyObject : lostSupplies)
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(lostSupplyObject, "Lost Supplies");
			}

			var worldLocation = lostSupplyObject.getWorldLocation();

			Color renderColor;
			if (currentWaypointLocation != null && currentWaypointLocation.equals(worldLocation))
			{
				renderColor = cachedConfig.getObjectivesColorCurrentWaypoint();
			}
			else if (laterLapLocations.contains(worldLocation))
			{
				renderColor = cachedConfig.getObjectivesColorLaterLaps();
			}
			else
			{
				renderColor = cachedConfig.getObjectivesColorCurrentLap();
			}

			if (cachedConfig.isDebugMode() && (allRouteLocations.isEmpty() || !allRouteLocations.contains(worldLocation)))
			{
				renderColor = Color.RED;
				debugLabel = (debugLabel == null ? "" : debugLabel + " ") + "(not in route)";
			}

			renderGameObjectWithHighlight(graphics, lostSupplyObject, renderColor, false, debugLabel);
		}
	}

	public void renderSpeedBoosts(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		var color = cachedConfig.getSpeedBoostColor();

		for (GameObject speedBoostObject : plugin.getGameState().getSpeedBoosts())
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(speedBoostObject, "Speed Boost");
			}
			renderGameObjectWithHighlight(graphics, speedBoostObject, color, true, debugLabel);
		}

		if (cachedConfig.isDebugMode())
		{
			var map = plugin.getGameState().getKnownSpeedBoostLocations();
			for (var speedBoostObject : map.keySet())
			{
				renderTileHighlightAtWorldPoint(graphics, speedBoostObject, Color.GREEN, "Boost Location");
			}
		}
	}

	public void renderLightningClouds(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		Color color = cachedConfig.getCloudColor();
		for (NPC cloudNpc : plugin.getGameState().getLightningClouds())
		{
			int currentAnimation = cloudNpc.getAnimation();

			boolean isCloudSafe = ObjectTracker.IsCloudSafe(currentAnimation);
			if (isCloudSafe)
			{
				continue;
			}

			renderCloudDangerAreaOnGround(graphics, cloudNpc, color);

			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = String.format("Cloud (ID: %d, Anim: %d)", cloudNpc.getId(), cloudNpc.getAnimation());
			}
			renderNpcWithHighlight(graphics, cloudNpc, color, debugLabel);
		}
	}

	public void renderFetidPools(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		var color = cachedConfig.getFetidPoolColor();
		for (GameObject fetidPool : plugin.getGameState().getFetidPools())
		{
			String debugLabel = null;
			if (cachedConfig.isShowIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(fetidPool, "Fetid Pool");
			}
			// TODO: slowdowns & exceptions
			// renderGameObjectWithHighlight(graphics, fetidPool, color, false, debugLabel);
		}
	}

	public void renderToadPickup(Graphics2D graphics)
	{
		var cached = plugin.getCachedConfig();
		var state = plugin.getGameState();
		var route = state.getCurrentStaticRoute();
		if (route == null || route.isEmpty())
			return;

		int currentLap = state.getCurrentLap();
		var completed = state.getCompletedWaypointIndices();
		int nextWaypointIndex = state.getNextWaypointIndex();

		for (int i = 0; i < route.size(); i++)
		{
			var waypoint = route.get(i);
			if (waypoint.getType() != RouteWaypoint.WaypointType.TOAD_PICKUP)
				continue;

			if (completed.contains(i))
				continue; // already completed, don't show

			var loc = waypoint.getLocation();

			Color color;
			if (i == nextWaypointIndex)
			{
				color = cached.getObjectivesColorCurrentWaypoint();
			}
			else if (waypoint.getLap() != currentLap)
			{
				color = cached.getObjectivesColorLaterLaps();
			}
			else
			{
				color = cached.getObjectivesColorCurrentLap();
			}

			var toadObject = ObjectRenderer.findGameObjectAtWorldPoint(client, loc);
			if (toadObject == null)
				continue;

			String label = cached.isShowIDs()
					? buildObjectLabelWithImpostorInfo(toadObject, "Toad Pickup")
					: null;

			renderBoatZoneRectangle(graphics, loc, color);
			renderGameObjectWithHighlight(graphics, toadObject, color, true, label);
		}
	}

	public void renderToadPillars(Graphics2D graphics)
	{
		var cached = plugin.getCachedConfig();
		var state = plugin.getGameState();
		var route = state.getCurrentStaticRoute();
		if (route == null)
			return;

		if (!state.isHasThrowableObjective())
			return;

		int currentLap = state.getCurrentLap();
		var completed = state.getCompletedWaypointIndices();

		int currentWaypointIndex = -1;
		var currentLapLocations = new HashSet<WorldPoint>();
		var laterLapLocations = new HashSet<WorldPoint>();

		for (int i = 0; i < route.size(); i++)
		{
			if (completed.contains(i))
				continue;

			if (currentWaypointIndex == -1)
			{
				currentWaypointIndex = i;
			}

			var wp = route.get(i);
			if (wp.getType() != RouteWaypoint.WaypointType.TOAD_PILLAR)
				continue;

			var loc = wp.getLocation();
			if (wp.getLap() == currentLap)
			{
				currentLapLocations.add(loc);
			}
			else
			{
				laterLapLocations.add(loc);
			}
		}

		List<WorldPoint> currentWaypointLocations;
		if (currentWaypointIndex >= 0)
		{
			// Special case - display pillars as "current" if they are the current OR NEXT waypoint
			var currentWp = route.get(currentWaypointIndex);
			currentWaypointLocations = new ArrayList<>();
			currentWaypointLocations.add(currentWp.getLocation());
			if (currentWaypointIndex + 1 < route.size())
			{
				var nextWp = route.get(currentWaypointIndex + 1);
				currentWaypointLocations.add(nextWp.getLocation());
			}
		} else {
            currentWaypointLocations = List.of();
        }

        state.getKnownToadPillars().entrySet().stream()
				.filter(e -> !e.getValue())
				.map(Map.Entry::getKey)
				.map(p -> ObjectRenderer.findGameObjectAtWorldPoint(client, p))
				.filter(Objects::nonNull)
				.forEach(pillar -> {
					var loc = pillar.getWorldLocation();

					Color color;
					if (currentWaypointLocations.contains(loc))
					{
						color = cached.getObjectivesColorCurrentWaypoint();
					}
					else if (currentLapLocations.contains(loc))
					{
						color = cached.getObjectivesColorCurrentLap();
					}
					else if (laterLapLocations.contains(loc))
					{
						// Clearer to leave them unhighlighted
						// color = cached.getObjectivesColorLaterLaps();
						return;
					}
					else
					{
						// Already completed or not in route
						return;
					}

					String label = cached.isShowIDs()
							? buildObjectLabelWithImpostorInfo(pillar, "Toad Pillar")
							: null;

					renderGameObjectWithHighlight(graphics, pillar, color, false, label);
				});
	}

	public void renderRumLocations(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		Color rumHighlightColor = cachedConfig.getObjectivesColorCurrentLap();

		var isCarryingRum = plugin.getGameState().isHasThrowableObjective();
		WorldPoint targetRumLocation;

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
				renderBoatZoneRectangle(graphics, targetRumLocation, rumHighlightColor);
				renderRumLocationHighlight(graphics, targetRumLocation, rumHighlightColor);
			}
		}
	}

	private void renderRumLocationHighlight(Graphics2D graphics, WorldPoint rumLocationPoint, Color highlightColor)
	{
		GameObject rumObjectAtLocation = findGameObjectAtWorldPoint(client, rumLocationPoint);
		if (rumObjectAtLocation != null)
		{
			renderGameObjectWithHighlight(graphics, rumObjectAtLocation, highlightColor, true, null);
		}
		else
		{
			renderTileHighlightAtWorldPoint(graphics, rumLocationPoint, highlightColor);
		}
	}

	private void renderGameObjectWithHighlight(Graphics2D graphics, TileObject tileObject, Color highlightColor, boolean shouldHighlightTile, String debugLabel)
	{
		LocalPoint objectLocalPoint = tileObject.getLocalLocation();

		if (shouldHighlightTile)
		{
			Polygon tilePolygon = Perspective.getCanvasTilePoly(client, objectLocalPoint);
			if (tilePolygon != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
			}
		}

		drawTileObjectHull(graphics, tileObject, highlightColor);

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

	private void drawTileObjectHull(Graphics2D g, TileObject object, Color borderColor)
	{
		Stroke stroke = new BasicStroke(2f);
		Shape poly = null;
		Shape poly2 = null;

		if (object instanceof GameObject)
		{
			poly = ((GameObject) object).getConvexHull();
		}
		else if (object instanceof WallObject)
		{
			poly = ((WallObject) object).getConvexHull();
			poly2 = ((WallObject) object).getConvexHull2();
		}
		else if (object instanceof DecorativeObject)
		{
			poly = ((DecorativeObject) object).getConvexHull();
			poly2 = ((DecorativeObject) object).getConvexHull2();
		}
		else if (object instanceof GroundObject)
		{
			poly = ((GroundObject) object).getConvexHull();
		}

		if (poly == null)
		{
			poly = object.getCanvasTilePoly();
		}

		Color fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 50);

		if (poly != null)
		{
			OverlayUtil.renderPolygon(g, poly, borderColor, fillColor, stroke);
		}
		if (poly2 != null)
		{
			OverlayUtil.renderPolygon(g, poly2, borderColor, fillColor, stroke);
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
		ObjectComposition comp = client.getObjectDefinition(gameObject.getId());

		String name =
				typeName != null
				? typeName
				: comp != null && comp.getName() != null
					? comp.getName()
					: "Unknown";

		WorldPoint wp = gameObject.getWorldLocation();
		int sceneX = gameObject.getSceneMinLocation().getX();
		int sceneY = gameObject.getSceneMinLocation().getY();

		StringBuilder sb = new StringBuilder();
		sb.append(name)
				.append(" (ID: ").append(gameObject.getId());

		if (comp != null)
		{
			int[] ids = comp.getImpostorIds();
			if (ids != null && ids.length > 0)
			{
				ObjectComposition imp = comp.getImpostor();
				if (imp != null)
				{
					sb.append(", Imp: ").append(imp.getId());
				}
			}
		}

		sb.append(", W: ").append(wp.getX()).append("/").append(wp.getY()).append("/").append(wp.getPlane());
		sb.append(", S: ").append(sceneX).append("/").append(sceneY);
		sb.append(")");

		return sb.toString();
	}

	public static GameObject findGameObjectAtWorldPoint(Client client, WorldPoint worldPoint)
	{
		return findGameObjectAtWorldPoint(client, worldPoint, null);
	}

	public static GameObject findGameObjectAtWorldPoint(Client client, WorldPoint worldPoint, Integer objectId)
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

		Tile tile = GetTileFromSceneOrExtended(worldPoint, localPoint, scene);

		if (tile == null)
		{
			return null;
		}

		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null)
			{
				if (objectId != null && gameObject.getId() != objectId)
				{
					continue;
				}
				return gameObject;
			}
		}

		return null;
	}

	private static Tile GetTileFromSceneOrExtended(WorldPoint worldPoint, LocalPoint localPoint, Scene scene) {
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		var plane = worldPoint.getPlane();
		var tiles = scene.getTiles();

		if (tiles != null &&
				plane >= 0 && plane < tiles.length &&
				sceneX >= 0 && sceneX < tiles[plane].length &&
				sceneY >= 0 && sceneY < tiles[plane][sceneX].length)
		{
			return tiles[plane][sceneX][sceneY];
		}
		else
		{
			var ext = scene.getExtendedTiles();
			if (ext != null &&
					plane >= 0 && plane < ext.length &&
					sceneX >= 0 && sceneX < ext[plane].length &&
					sceneY >= 0 && sceneY < ext[plane][sceneX].length)
			{
				return ext[plane][sceneX][sceneY];
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

		RouteWaypoint nextWaypoint = staticRoute.get(nextWaypointIndex);
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

		var trial = plugin.getGameState().getCurrentTrial();
		if (trial == null)
		{
			return;
		}

		int width;
		int height;
		if (trial.getTrialType() == TrialType.TEMPOR_TANTRUM)
		{
			width = TemporTantrumConfig.BOAT_EXCLUSION_WIDTH;
			height = TemporTantrumConfig.BOAT_EXCLUSION_HEIGHT;
		}
		else if (trial.getTrialType() == TrialType.JUBBLY_JIVE)
		{
			width = JubblyJiveConfig.BOAT_EXCLUSION_WIDTH;
			height = JubblyJiveConfig.BOAT_EXCLUSION_HEIGHT;
		}
		else
		{
			return;
		}

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
