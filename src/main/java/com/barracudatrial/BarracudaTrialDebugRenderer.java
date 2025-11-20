package com.barracudatrial;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BarracudaTrialDebugRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final BarracudaTrialConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Map<Point, Integer> labelCounts;

	public BarracudaTrialDebugRenderer(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	public void setLabelCounts(Map<Point, Integer> labelCounts)
	{
		this.labelCounts = labelCounts;
	}

	public void renderDebugInfo(Graphics2D graphics)
	{
		renderExclusionZone(graphics);

		if (config.showIDs())
		{
			renderAllRocksInScene(graphics);
		}

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

		// Render label at center of exclusion zone
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

	private void renderAllRocksInScene(Graphics2D graphics)
	{
		Set<String> renderedLabels = new HashSet<>();

		for (GameObject rock : plugin.getAllRocksInScene())
		{
			LocalPoint localPoint = rock.getLocalLocation();
			if (localPoint == null)
			{
				continue;
			}

			Color rockColor = new Color(255, 165, 0, 180);

			// Highlight tile
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, rockColor);
			}

			// Highlight model
			modelOutlineRenderer.drawOutline(rock, 2, rockColor, 4);

			// Draw ID label with impostor info
			String label = getObjectLabelWithImpostor(rock);

			// Only render each unique label once to avoid clutter
			if (!renderedLabels.contains(label))
			{
				Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
				if (textPoint != null)
				{
					int yOffset = getAndIncrementLabelOffset(textPoint);
					Point offsetPoint = new Point(textPoint.getX(), textPoint.getY() + yOffset);
					OverlayUtil.renderTextLocation(graphics, offsetPoint, label, rockColor);
					renderedLabels.add(label);
				}
			}
		}
	}

	private void renderDebugText(Graphics2D graphics)
	{
		int x = 10;
		int y = 80;
		int lineHeight = 15;

		graphics.setFont(new Font("Arial", Font.BOLD, 12));

		List<String> debugLines = buildDebugLines();

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

	private List<String> buildDebugLines()
	{
		List<String> lines = new ArrayList<>();

		lines.add("=== BARRACUDA TRIAL DEBUG ===");
		lines.add(String.format("Lap: %d / %d", plugin.getCurrentLap() + 1, plugin.getRumsNeeded()));
		lines.add(String.format("Planned Laps: %d", plugin.getPlannedLaps().size()));
		lines.add(String.format("Lost Supplies Visible: %d", plugin.getLostSupplies().size()));
		lines.add(String.format("Crates: %d / %d", plugin.getCratesCollected(), plugin.getCratesTotal()));
		lines.add(String.format("Rum: %d / %d", plugin.getRumsCollected(), plugin.getRumsNeeded()));
		lines.add(String.format("Current Path: %d points", plugin.getCurrentSegmentPath().size()));
		lines.add(String.format("Next Path: %d points", plugin.getNextSegmentPath().size()));
		lines.add("");
		lines.add("--- Performance (ms) ---");
		lines.add(String.format("Total Game Tick: %d ms", plugin.getLastTotalGameTickTimeMs()));
		lines.add(String.format("  Lost Supplies Update: %d ms", plugin.getLastLostSuppliesUpdateTimeMs()));
		lines.add(String.format("  Cloud Update: %d ms", plugin.getLastCloudUpdateTimeMs()));
		lines.add(String.format("  Rock Update: %d ms", plugin.getLastRockUpdateTimeMs()));
		lines.add(String.format("  Path Planning: %d ms", plugin.getLastPathPlanningTimeMs()));
		lines.add(String.format("  A* Pathfinding: %d ms", plugin.getLastAStarTimeMs()));
		lines.add(String.format("Last Path Recalc: %s", plugin.getLastPathRecalcCaller()));
		lines.add("");
		lines.add("--- Visible Objects ---");
		lines.add(String.format("Lightning Clouds: %d", plugin.getLightningClouds().size()));
		lines.add(String.format("Rocks (visible): %d", plugin.getRocks().size()));
		lines.add(String.format("Speed Boosts (visible): %d", plugin.getSpeedBoosts().size()));
		lines.add(String.format("All Rocks (debug scan): %d", plugin.getAllRocksInScene().size()));
		lines.add("");
		lines.add("--- Persistent Storage ---");
		lines.add(String.format("Known Rock Locations: %d", plugin.getKnownRockLocations().size()));
		lines.add(String.format("Known Speed Boosts: %d", plugin.getKnownSpeedBoostLocations().size()));
		lines.add(String.format("Known Supply Spawns: %d", plugin.getKnownLostSuppliesSpawnLocations().size()));

		WorldPoint boat = plugin.getBoatLocation();
		if (boat != null)
		{
			lines.add("");
			lines.add("--- Boat Position ---");
			lines.add(String.format("Tile: (%d, %d, %d)", boat.getX(), boat.getY(), boat.getPlane()));
		}

		lines.add("");
		lines.add("--- Rum Locations ---");
		addRumLocationDebugInfo(lines);

		return lines;
	}

	private void addRumLocationDebugInfo(List<String> lines)
	{
		WorldPoint rumPickup = plugin.getRumPickupLocation();
		WorldPoint rumReturn = plugin.getRumReturnLocation();

		if (rumPickup != null)
		{
			GameObject pickupObject = findGameObjectAt(rumPickup);
			String pickupInfo = String.format("Pickup (S): (%d, %d, %d)",
				rumPickup.getX(), rumPickup.getY(), rumPickup.getPlane());
			if (pickupObject != null)
			{
				pickupInfo += String.format(" [ID: %d]", pickupObject.getId());
			}
			lines.add(pickupInfo);
		}
		else
		{
			lines.add("Pickup (S): null");
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
			lines.add(returnInfo);
		}
		else
		{
			lines.add("Return (N): null");
		}
	}

	private int getAndIncrementLabelOffset(Point canvasPoint)
	{
		Point roundedPoint = new Point(
			(canvasPoint.getX() / 10) * 10,
			(canvasPoint.getY() / 10) * 10
		);

		int count = labelCounts.getOrDefault(roundedPoint, 0);
		labelCounts.put(roundedPoint, count + 1);

		return count * 15;
	}

	private String getObjectLabelWithImpostor(GameObject obj)
	{
		ObjectComposition comp = client.getObjectDefinition(obj.getId());

		String name;
		if (comp != null && comp.getName() != null)
		{
			name = comp.getName();
		}
		else
		{
			name = "Unknown";
		}

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
