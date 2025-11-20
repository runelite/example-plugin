package com.barracudatrial;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class BarracudaTrialOverlay extends Overlay
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final BarracudaTrialConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private int frame = 0;
	private java.util.Map<Point, Integer> labelCounts = new java.util.HashMap<>();

	@Inject
	public BarracudaTrialOverlay(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		frame++;
		labelCounts.clear();

		if (!plugin.isInTrialArea())
		{
			return null;
		}

		if (config.showOptimalPath())
		{
			renderOptimalPath(graphics);
		}

		if (config.highlightLostSupplies())
		{
			renderLostSupplies(graphics);
		}

		if (config.highlightClouds())
		{
			renderLightningClouds(graphics);
		}

		if (config.highlightRocks())
		{
			renderRocks(graphics);
		}

		if (config.highlightSpeedBoosts())
		{
			renderSpeedBoosts(graphics);
		}

		if (config.highlightRumLocations())
		{
			renderRumLocations(graphics);
		}

		if (config.debugMode())
		{
			renderDebugInfo(graphics);
		}

		return null;
	}

	private void renderOptimalPath(Graphics2D graphics)
	{
		WorldPoint boatLocation = plugin.getBoatLocation();
		if (boatLocation == null)
		{
			return;
		}

		List<WorldPoint> currentSegment = plugin.getCurrentSegmentPath();
		if (!currentSegment.isEmpty())
		{
			if (config.debugMode())
			{
				WorldPoint previous = boatLocation;
				for (WorldPoint point : currentSegment)
				{
					drawSimpleLine(graphics, previous, point, new Color(255, 255, 0, 150));
					previous = point;
				}
			}

			int totalSegments = 0;
			WorldPoint previousPoint = boatLocation;
			for (WorldPoint point : currentSegment)
			{
				List<WorldPoint> interpolated = interpolateLine(previousPoint, point);
				totalSegments += Math.max(0, interpolated.size() - 1);
				previousPoint = point;
			}

			int segmentOffset = 0;
			previousPoint = boatLocation;
			for (WorldPoint point : currentSegment)
			{
				segmentOffset = drawPathSegment(graphics, previousPoint, point, config.pathColor(), totalSegments, segmentOffset);
				previousPoint = point;
			}
		}
	}

	private void drawSimpleLine(Graphics2D graphics, WorldPoint from, WorldPoint to, Color color)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		if (from == null || to == null) return;
		if (from.getPlane() != to.getPlane()) return;

		LocalPoint fromLocal = LocalPoint.fromWorld(topLevelWorldView, from);
		LocalPoint toLocal = LocalPoint.fromWorld(topLevelWorldView, to);

		if (fromLocal == null || toLocal == null) return;

		Point fromCanvas = Perspective.localToCanvas(client, fromLocal, from.getPlane(), 0);
		Point toCanvas = Perspective.localToCanvas(client, toLocal, to.getPlane(), 0);

		if (fromCanvas == null || toCanvas == null) return;

		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(fromCanvas.getX(), fromCanvas.getY(), toCanvas.getX(), toCanvas.getY());
	}

	private int drawPathSegment(Graphics2D graphics, WorldPoint from, WorldPoint to, Color pathColor, int totalSegments, int segmentOffset)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return segmentOffset;
		}

		if (from == null || to == null) return segmentOffset;
		if (from.equals(new WorldPoint(0, 0, 0))) return segmentOffset;
		if (to.equals(new WorldPoint(0, 0, 0))) return segmentOffset;
		if (from.getPlane() != to.getPlane()) return segmentOffset;

		List<WorldPoint> interpolated = interpolateLine(from, to);
		if (interpolated.isEmpty()) return segmentOffset;

		int segmentCount = interpolated.size() - 1;
		if (segmentCount <= 0) return segmentOffset;

		boolean tracerEnabled = config.showPathTracer() && pathColor.equals(config.pathColor());
		int globalPulsePosition = tracerEnabled && totalSegments > 0 ? (frame % totalSegments) : -1;

		graphics.setStroke(new BasicStroke(config.pathWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		for (int j = 0; j < segmentCount; j++)
		{
			WorldPoint wp1 = interpolated.get(j);
			WorldPoint wp2 = interpolated.get(j + 1);

			// Convert to canvas points
			LocalPoint lp1 = LocalPoint.fromWorld(topLevelWorldView, wp1);
			LocalPoint lp2 = LocalPoint.fromWorld(topLevelWorldView, wp2);

			if (lp1 == null || lp2 == null) continue;

			Point p1 = Perspective.localToCanvas(client, lp1, wp1.getPlane(), 0);
			Point p2 = Perspective.localToCanvas(client, lp2, wp2.getPlane(), 0);

			if (p1 == null || p2 == null) continue;

			// Apply tracer effect using global position
			int globalSegmentIndex = segmentOffset + j;
			Color segmentColor = (globalSegmentIndex == globalPulsePosition) ? config.tracerColor() : pathColor;

			graphics.setColor(segmentColor);
			graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
		}

		// Return updated offset for next segment
		return segmentOffset + segmentCount;
	}

	private void renderLostSupplies(Graphics2D graphics)
	{
		for (GameObject supply : plugin.getLostSupplies())
		{
			String label = null;
			// Only show label with IDs if showIDs enabled
			if (config.showIDs())
			{
				label = getObjectLabelWithImpostor(supply, "Lost Supplies");
			}
			renderGameObject(graphics, supply, config.lostSuppliesColor(), config.showLostSuppliesTile(), label);
		}
	}

	private void renderLightningClouds(Graphics2D graphics)
	{
		for (NPC cloud : plugin.getLightningClouds())
		{
			int animation = cloud.getAnimation();

			// Only render clouds that are dangerous
			// Skip safe animations (harmless/inactive clouds)
			if (plugin.isCloudSafe(animation))
			{
				continue; // Skip rendering this cloud entirely
			}

			Color cloudColor = config.cloudColor();

			// Always draw danger area for dangerous clouds (animation != -1)
			renderNPCDangerArea(graphics, cloud, cloudColor);

			// Draw the cloud itself with ID if showIDs enabled
			String label = null;
			if (config.showIDs())
			{
				label = String.format("Cloud (ID: %d, Anim: %d)", cloud.getId(), cloud.getAnimation());
			}
			renderNPC(graphics, cloud, cloudColor, true, label);
		}
	}

	private void renderRocks(Graphics2D graphics)
	{
		for (GameObject rock : plugin.getRocks())
		{
			String label = null;
			if (config.showIDs())
			{
				label = getObjectLabelWithImpostor(rock, "Rock");
			}
			renderGameObject(graphics, rock, config.rockColor(), true, label);
		}
	}

	private void renderSpeedBoosts(Graphics2D graphics)
	{
		for (GameObject speedBoost : plugin.getSpeedBoosts())
		{
			String label = null;
			if (config.showIDs())
			{
				label = getObjectLabelWithImpostor(speedBoost, "Speed Boost");
			}
			renderGameObject(graphics, speedBoost, config.speedBoostColor(), true, label);
		}
	}

	private void renderGameObject(Graphics2D graphics, GameObject gameObject, Color color, boolean showTile, String label)
	{
		LocalPoint localPoint = gameObject.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		// Highlight the tile
		if (showTile)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}

		// Highlight the model
		modelOutlineRenderer.drawOutline(gameObject, 2, color, 4);

		// Draw label
		if (label != null)
		{
			Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
			if (textPoint != null)
			{
				// Apply Y offset to avoid label overlap
				int yOffset = getAndIncrementLabelOffset(textPoint);
				Point offsetPoint = new Point(textPoint.getX(), textPoint.getY() + yOffset);
				OverlayUtil.renderTextLocation(graphics, offsetPoint, label, color);
			}
		}
	}

	private void renderRumLocations(Graphics2D graphics)
	{
		Color rumColor = config.rumLocationColor();

		// Only highlight the rum location that should be clicked
		if (plugin.isHasRumOnUs())
		{
			// Carrying rum - highlight the dropoff/return location
			WorldPoint returnLocation = plugin.getRumReturnLocation();
			if (returnLocation != null)
			{
				renderRumLocation(graphics, returnLocation, rumColor);
			}
		}
		else
		{
			// Not carrying rum - highlight the pickup location
			WorldPoint pickupLocation = plugin.getRumPickupLocation();
			if (pickupLocation != null)
			{
				renderRumLocation(graphics, pickupLocation, rumColor);
			}
		}
	}

	private void renderRumLocation(Graphics2D graphics, WorldPoint rumLocation, Color rumColor)
	{
		// Find the GameObject at this location
		GameObject rumObject = findGameObjectAt(rumLocation);
		if (rumObject != null)
		{
			// Use same rendering as lost supplies - highlight tile and model
			renderGameObject(graphics, rumObject, rumColor, true, null);
		}
		else
		{
			// Fallback: just highlight the tile if we can't find the object
			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView == null)
			{
				return;
			}

			LocalPoint localPoint = LocalPoint.fromWorld(topLevelWorldView, rumLocation);
			if (localPoint == null)
			{
				return;
			}

			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, rumColor);
			}
		}
	}

	private void renderNPC(Graphics2D graphics, NPC npc, Color color, boolean showTile, String label)
	{
		LocalPoint localPoint = npc.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		// Highlight the tile
		if (showTile)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}

		// Highlight the model
		modelOutlineRenderer.drawOutline(npc, 2, color, 4);

		// Draw label
		if (label != null)
		{
			Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, npc.getLogicalHeight() + 40);
			if (textPoint != null)
			{
				// Apply Y offset to avoid label overlap
				int yOffset = getAndIncrementLabelOffset(textPoint);
				Point offsetPoint = new Point(textPoint.getX(), textPoint.getY() + yOffset);
				OverlayUtil.renderTextLocation(graphics, offsetPoint, label, color);
			}
		}
	}

	private void renderNPCDangerArea(Graphics2D graphics, NPC npc, Color color)
	{
		LocalPoint center = npc.getLocalLocation();
		if (center == null)
		{
			return;
		}

		// Draw a larger area around the cloud to show danger zone
		int radius = config.cloudDangerRadius(); // tiles

		for (int dx = -radius; dx <= radius; dx++)
		{
			for (int dy = -radius; dy <= radius; dy++)
			{
				if (dx * dx + dy * dy <= radius * radius)
				{
					LocalPoint point = new LocalPoint(
						center.getX() + dx * Perspective.LOCAL_TILE_SIZE,
						center.getY() + dy * Perspective.LOCAL_TILE_SIZE
					);

					Polygon poly = Perspective.getCanvasTilePoly(client, point);
					if (poly != null)
					{
						Color areaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 30);
						graphics.setColor(areaColor);
						graphics.fill(poly);
						graphics.setColor(color);
						graphics.draw(poly);
					}
				}
			}
		}
	}

	private java.awt.Point getCenterPoint(Polygon polygon)
	{
		if (polygon.npoints == 0)
		{
			return null;
		}

		int sumX = 0;
		int sumY = 0;

		for (int i = 0; i < polygon.npoints; i++)
		{
			sumX += polygon.xpoints[i];
			sumY += polygon.ypoints[i];
		}

		return new java.awt.Point(sumX / polygon.npoints, sumY / polygon.npoints);
	}

	private void renderDebugInfo(Graphics2D graphics)
	{
		// Render exclusion zone
		renderExclusionZone(graphics);

		// Render all rocks with IDs (only if showIDs is enabled)
		if (config.showIDs())
		{
			renderAllRocks(graphics);
		}

		// Render debug text
		renderDebugText(graphics);
	}

	private void renderExclusionZone(Graphics2D graphics)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		int minX = plugin.getExclusionZoneMinX();
		int maxX = plugin.getExclusionZoneMaxX();
		int minY = plugin.getExclusionZoneMinY();
		int maxY = plugin.getExclusionZoneMaxY();

		Color exclusionColor = new Color(255, 0, 255, 60);

		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				WorldPoint wp = new WorldPoint(x, y, 0);

				if (plugin.isPointInExclusionZone(wp))
				{
					LocalPoint lp = LocalPoint.fromWorld(topLevelWorldView, wp);

					if (lp != null)
					{
						Polygon poly = Perspective.getCanvasTilePoly(client, lp);
						if (poly != null)
						{
							OverlayUtil.renderPolygon(graphics, poly, exclusionColor);
						}
					}
				}
			}
		}

		WorldPoint center = new WorldPoint((minX + maxX) / 2, (minY + maxY) / 2, 0);
		LocalPoint centerLocal = LocalPoint.fromWorld(topLevelWorldView, center);
		if (centerLocal != null)
		{
			Point textPoint = Perspective.getCanvasTextLocation(client, graphics, centerLocal, "EXCLUSION ZONE", 0);
			if (textPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, textPoint, "EXCLUSION ZONE", new Color(255, 0, 255, 255));
			}
		}
	}

	private void renderAllRocks(Graphics2D graphics)
	{
		// Track which labels we've already rendered to avoid duplicates
		java.util.Set<String> renderedLabels = new java.util.HashSet<>();

		for (GameObject rock : plugin.getAllRocksInScene())
		{
			LocalPoint localPoint = rock.getLocalLocation();
			if (localPoint == null)
			{
				continue;
			}

			Color rockColor = new Color(255, 165, 0, 180); // Orange for debug rocks

			// Highlight the tile
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, rockColor);
			}

			// Highlight the model
			modelOutlineRenderer.drawOutline(rock, 2, rockColor, 4);

			// Draw ID label with impostor info (skip duplicates)
			String label = getObjectLabelWithImpostor(rock, null);

			// Only render label if we haven't rendered this exact label yet
			if (!renderedLabels.contains(label))
			{
				Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
				if (textPoint != null)
				{
					// Apply Y offset to avoid label overlap
					int yOffset = getAndIncrementLabelOffset(textPoint);
					Point offsetPoint = new Point(textPoint.getX(), textPoint.getY() + yOffset);
					OverlayUtil.renderTextLocation(graphics, offsetPoint, label, rockColor);

					// Mark this label as rendered
					renderedLabels.add(label);
				}
			}
		}
	}

	private void renderDebugText(Graphics2D graphics)
	{
		// Render debug text overlay in top-right corner
		int x = 10;
		int y = 80;
		int lineHeight = 15;

		graphics.setFont(new Font("Arial", Font.BOLD, 12));

		List<String> debugLines = new ArrayList<>();
		debugLines.add("=== BARRACUDA TRIAL DEBUG ===");
		debugLines.add(String.format("Lap: %d / %d", plugin.getCurrentLap() + 1, plugin.getRumsNeeded()));
		debugLines.add(String.format("Planned Laps: %d", plugin.getPlannedLaps().size()));
		debugLines.add(String.format("Lost Supplies Visible: %d", plugin.getLostSupplies().size()));
		debugLines.add(String.format("Crates: %d / %d", plugin.getCratesCollected(), plugin.getCratesTotal()));
		debugLines.add(String.format("Rum: %d / %d", plugin.getRumsCollected(), plugin.getRumsNeeded()));
		debugLines.add(String.format("Current Path: %d points", plugin.getCurrentSegmentPath().size()));
		debugLines.add(String.format("Next Path: %d points", plugin.getNextSegmentPath().size()));
		debugLines.add("");
		debugLines.add("--- Performance (ms) ---");
		debugLines.add(String.format("Total Game Tick: %d ms", plugin.getLastTotalGameTickTimeMs()));
		debugLines.add(String.format("  Lost Supplies Update: %d ms", plugin.getLastLostSuppliesUpdateTimeMs()));
		debugLines.add(String.format("  Cloud Update: %d ms", plugin.getLastCloudUpdateTimeMs()));
		debugLines.add(String.format("  Rock Update: %d ms", plugin.getLastRockUpdateTimeMs()));
		debugLines.add(String.format("  Path Planning: %d ms", plugin.getLastPathPlanningTimeMs()));
		debugLines.add(String.format("  A* Pathfinding: %d ms", plugin.getLastAStarTimeMs()));
		debugLines.add(String.format("Last Path Recalc: %s", plugin.getLastPathRecalcCaller()));
		debugLines.add("");
		debugLines.add("--- Visible Objects ---");
		debugLines.add(String.format("Lightning Clouds: %d", plugin.getLightningClouds().size()));
		debugLines.add(String.format("Rocks (visible): %d", plugin.getRocks().size()));
		debugLines.add(String.format("Speed Boosts (visible): %d", plugin.getSpeedBoosts().size()));
		debugLines.add(String.format("All Rocks (debug scan): %d", plugin.getAllRocksInScene().size()));
		debugLines.add("");
		debugLines.add("--- Persistent Storage ---");
		debugLines.add(String.format("Known Rock Locations: %d", plugin.getKnownRockLocations().size()));
		debugLines.add(String.format("Known Speed Boosts: %d", plugin.getKnownSpeedBoostLocations().size()));
		debugLines.add(String.format("Known Supply Spawns: %d", plugin.getKnownLostSuppliesSpawnLocations().size()));

		WorldPoint boat = plugin.getBoatLocation();
		if (boat != null)
		{
			debugLines.add("");
			debugLines.add("--- Boat Position ---");
			debugLines.add(String.format("Tile: (%d, %d, %d)", boat.getX(), boat.getY(), boat.getPlane()));
		}

		WorldPoint rumPickup = plugin.getRumPickupLocation();
		WorldPoint rumReturn = plugin.getRumReturnLocation();
		debugLines.add("");
		debugLines.add("--- Rum Locations ---");
		if (rumPickup != null)
		{
			GameObject pickupObject = findGameObjectAt(rumPickup);
			String pickupInfo = String.format("Pickup (S): (%d, %d, %d)",
				rumPickup.getX(), rumPickup.getY(), rumPickup.getPlane());
			if (pickupObject != null)
			{
				pickupInfo += String.format(" [ID: %d]", pickupObject.getId());
			}
			debugLines.add(pickupInfo);
		}
		else
		{
			debugLines.add("Pickup (S): null");
		}
		if (rumReturn != null)
		{
			GameObject returnObject = findGameObjectAt(rumReturn);
			String returnInfo = String.format("Return (N): (%d, %d, %d)",
				rumReturn.getX(), rumReturn.getY(), rumReturn.getPlane());
			if (returnObject != null)
			{
				returnInfo += String.format(" [ID: %d]", returnObject.getId());
			}
			debugLines.add(returnInfo);
		}
		else
		{
			debugLines.add("Return (N): null");
		}

		for (String line : debugLines)
		{
			Color backgroundColor = new Color(0, 0, 0, 180);
			Color textColor = Color.WHITE;

			// Draw background
			Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(line, graphics);
			graphics.setColor(backgroundColor);
			graphics.fillRect(x - 2, y - 12, (int) bounds.getWidth() + 4, (int) bounds.getHeight());

			// Draw text
			graphics.setColor(textColor);
			graphics.drawString(line, x, y);

			y += lineHeight;
		}
	}

	// Helper methods for tracer animation (from port-tasks)
	private Color dimColor(Color color, float factor)
	{
		factor = Math.min(Math.max(factor, 0f), 1f);
		int r = (int)(color.getRed() * factor);
		int g = (int)(color.getGreen() * factor);
		int b = (int)(color.getBlue() * factor);
		return new Color(r, g, b, color.getAlpha());
	}

	private List<WorldPoint> interpolateLine(WorldPoint start, WorldPoint end)
	{
		List<WorldPoint> result = new ArrayList<>();

		// Safety check: limit max steps to prevent OOM errors
		int distance = start.distanceTo(end);
		if (distance > 500)
		{
			// Distance is unreasonably large - likely bad data, skip interpolation
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

	/**
	 * Gets the Y offset for a label at this position to avoid overlap
	 * Returns the offset and increments the counter for this position
	 */
	private int getAndIncrementLabelOffset(Point canvasPoint)
	{
		// Round position to nearest 10 pixels to group nearby labels
		Point roundedPoint = new Point(
			(canvasPoint.getX() / 10) * 10,
			(canvasPoint.getY() / 10) * 10
		);

		int count = labelCounts.getOrDefault(roundedPoint, 0);
		labelCounts.put(roundedPoint, count + 1);

		// Return Y offset: 15 pixels per label
		return count * 15;
	}

	/**
	 * Creates a label string for a GameObject with its ID and impostor ID (if any)
	 * @param obj The GameObject to label
	 * @param typeName Optional type name (e.g. "Lost Supplies", "Rock"), or null to use object name
	 * @return Label string like "Lost Supplies (ID: 59245, Impostor: 59244)"
	 */
	private String getObjectLabelWithImpostor(GameObject obj, String typeName)
	{
		ObjectComposition comp = client.getObjectDefinition(obj.getId());

		// Get the type name
		String name;
		if (typeName != null)
		{
			name = typeName;
		}
		else if (comp != null && comp.getName() != null)
		{
			name = comp.getName();
		}
		else
		{
			name = "Unknown";
		}

		// Check for impostor
		StringBuilder label = new StringBuilder();
		label.append(name).append(" (ID: ").append(obj.getId());

		if (comp != null)
		{
			int[] impostorIds = comp.getImpostorIds();
			if (impostorIds != null && impostorIds.length > 0)
			{
				ObjectComposition impostor = comp.getImpostor();
				if (impostor != null)
				{
					label.append(", Imp: ").append(impostor.getId());
				}
			}
		}

		label.append(")");
		return label.toString();
	}

	// Helper method to find a GameObject at a specific WorldPoint
	private GameObject findGameObjectAt(WorldPoint worldPoint)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}

		var scene = worldView.getScene();
		if (scene == null)
		{
			return null;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		var tile = scene.getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
		if (tile == null)
		{
			return null;
		}

		// Return the first non-null GameObject on this tile
		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null)
			{
				return gameObject;
			}
		}

		return null;
	}
}
