package com.barracudatrial.rendering;

import com.barracudatrial.BarracudaTrialConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DebugRenderer
{
	private final Client client;
	private final BarracudaTrialPlugin plugin;
	private final BarracudaTrialConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Map<net.runelite.api.Point, Integer> labelCountsByCanvasPosition;

	public DebugRenderer(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	public void setLabelCounts(Map<net.runelite.api.Point, Integer> labelCountsByCanvasPosition)
	{
		this.labelCountsByCanvasPosition = labelCountsByCanvasPosition;
	}

	public void renderDebugInfo(Graphics2D graphics)
	{
		renderExclusionZoneVisualization(graphics);

		if (config.showIDs())
		{
			renderAllRocksInSceneWithLabels(graphics);
		}

		renderDebugTextOverlay(graphics);
	}

	private void renderExclusionZoneVisualization(Graphics2D graphics)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		int exclusionZoneMinX = plugin.getGameState().getExclusionZoneMinX();
		int exclusionZoneMaxX = plugin.getGameState().getExclusionZoneMaxX();
		int exclusionZoneMinY = plugin.getGameState().getExclusionZoneMinY();
		int exclusionZoneMaxY = plugin.getGameState().getExclusionZoneMaxY();

		Color exclusionZoneColor = new Color(255, 0, 255, 60);

		for (int x = exclusionZoneMinX; x <= exclusionZoneMaxX; x++)
		{
			for (int y = exclusionZoneMinY; y <= exclusionZoneMaxY; y++)
			{
				WorldPoint tileWorldPoint = new WorldPoint(x, y, 0);

				boolean isTileInExclusionZone = plugin.isPointInExclusionZone(tileWorldPoint);
				if (isTileInExclusionZone)
				{
					LocalPoint tileLocalPoint = LocalPoint.fromWorld(topLevelWorldView, tileWorldPoint);

					if (tileLocalPoint != null)
					{
						Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tileLocalPoint);
						if (tilePolygon != null)
						{
							OverlayUtil.renderPolygon(graphics, tilePolygon, exclusionZoneColor);
						}
					}
				}
			}
		}

		WorldPoint exclusionZoneCenterPoint = new WorldPoint((exclusionZoneMinX + exclusionZoneMaxX) / 2, (exclusionZoneMinY + exclusionZoneMaxY) / 2, 0);
		LocalPoint centerLocalPoint = LocalPoint.fromWorld(topLevelWorldView, exclusionZoneCenterPoint);
		if (centerLocalPoint != null)
		{
			Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, centerLocalPoint, "EXCLUSION ZONE", 0);
			if (labelCanvasPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, labelCanvasPoint, "EXCLUSION ZONE", new Color(255, 0, 255, 255));
			}
		}
	}

	private void renderAllRocksInSceneWithLabels(Graphics2D graphics)
	{
		Set<String> alreadyRenderedLabels = new HashSet<>();

		for (GameObject rockObject : plugin.getAllRocksInScene())
		{
			LocalPoint rockLocalPoint = rockObject.getLocalLocation();
			if (rockLocalPoint == null)
			{
				continue;
			}

			Color debugRockColor = new Color(255, 165, 0, 180);

			Polygon tilePolygon = Perspective.getCanvasTilePoly(client, rockLocalPoint);
			if (tilePolygon != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePolygon, debugRockColor);
			}

			modelOutlineRenderer.drawOutline(rockObject, 2, debugRockColor, 4);

			String rockLabel = buildObjectLabelWithImpostorInfo(rockObject);

			boolean isLabelAlreadyRendered = alreadyRenderedLabels.contains(rockLabel);
			if (!isLabelAlreadyRendered)
			{
				Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, rockLocalPoint, rockLabel, 0);
				if (labelCanvasPoint != null)
				{
					int yOffsetToAvoidLabelOverlap = calculateAndIncrementLabelOffset(labelCanvasPoint);
					Point adjustedLabelPoint = new Point(labelCanvasPoint.getX(), labelCanvasPoint.getY() + yOffsetToAvoidLabelOverlap);
					OverlayUtil.renderTextLocation(graphics, adjustedLabelPoint, rockLabel, debugRockColor);
					alreadyRenderedLabels.add(rockLabel);
				}
			}
		}
	}

	private void renderDebugTextOverlay(Graphics2D graphics)
	{
		int textStartX = 10;
		int textStartY = 80;
		int lineHeightInPixels = 15;

		graphics.setFont(new Font("Arial", Font.BOLD, 12));

		List<String> debugTextLines = buildDebugTextLines();

		for (String textLine : debugTextLines)
		{
			Color textBackgroundColor = new Color(0, 0, 0, 180);
			Color textForegroundColor = Color.WHITE;

			Rectangle2D textBounds = graphics.getFontMetrics().getStringBounds(textLine, graphics);
			graphics.setColor(textBackgroundColor);
			graphics.fillRect(textStartX - 2, textStartY - 12, (int) textBounds.getWidth() + 4, (int) textBounds.getHeight());

			graphics.setColor(textForegroundColor);
			graphics.drawString(textLine, textStartX, textStartY);

			textStartY += lineHeightInPixels;
		}
	}

	private List<String> buildDebugTextLines()
	{
		List<String> debugLines = new ArrayList<>();

		debugLines.add("=== BARRACUDA TRIAL DEBUG ===");
		debugLines.add(String.format("Lap: %d / %d", plugin.getGameState().getCurrentLap() + 1, plugin.getGameState().getRumsNeeded()));
		debugLines.add(String.format("Planned Laps: %d", plugin.getGameState().getPlannedLaps().size()));
		debugLines.add(String.format("Lost Supplies Visible: %d", plugin.getGameState().getLostSupplies().size()));
		debugLines.add(String.format("Crates: %d / %d", plugin.getGameState().getCratesCollected(), plugin.getGameState().getCratesTotal()));
		debugLines.add(String.format("Rum: %d / %d", plugin.getGameState().getRumsCollected(), plugin.getGameState().getRumsNeeded()));
		debugLines.add(String.format("Current Path: %d points", plugin.getGameState().getCurrentSegmentPath().size()));
		debugLines.add(String.format("Next Path: %d points", plugin.getGameState().getNextSegmentPath().size()));
		debugLines.add("");
		debugLines.add("--- Performance (ms) ---");
		debugLines.add(String.format("Total Game Tick: %d ms", plugin.getGameState().getLastTotalGameTickTimeMs()));
		debugLines.add(String.format("  Lost Supplies Update: %d ms", plugin.getGameState().getLastLostSuppliesUpdateTimeMs()));
		debugLines.add(String.format("  Cloud Update: %d ms", plugin.getGameState().getLastCloudUpdateTimeMs()));
		debugLines.add(String.format("  Rock Update: %d ms", plugin.getGameState().getLastRockUpdateTimeMs()));
		debugLines.add(String.format("  Path Planning: %d ms", plugin.getGameState().getLastPathPlanningTimeMs()));
		debugLines.add(String.format("  A* Pathfinding: %d ms", plugin.getGameState().getLastAStarTimeMs()));
		debugLines.add(String.format("Last Path Recalc: %s", plugin.getGameState().getLastPathRecalcCaller()));
		debugLines.add("");
		debugLines.add("--- Visible Objects ---");
		debugLines.add(String.format("Lightning Clouds: %d", plugin.getGameState().getLightningClouds().size()));
		debugLines.add(String.format("Rocks (visible): %d", plugin.getGameState().getRocks().size()));
		debugLines.add(String.format("Speed Boosts (visible): %d", plugin.getGameState().getSpeedBoosts().size()));
		debugLines.add(String.format("All Rocks (debug scan): %d", plugin.getAllRocksInScene().size()));
		debugLines.add("");
		debugLines.add("--- Persistent Storage ---");
		debugLines.add(String.format("Known Rock Locations: %d", plugin.getGameState().getKnownRockLocations().size()));
		debugLines.add(String.format("Known Speed Boosts: %d", plugin.getGameState().getKnownSpeedBoostLocations().size()));
		debugLines.add(String.format("Known Supply Spawns: %d", plugin.getGameState().getKnownLostSuppliesSpawnLocations().size()));

		WorldPoint boatCurrentLocation = plugin.getGameState().getBoatLocation();
		if (boatCurrentLocation != null)
		{
			debugLines.add("");
			debugLines.add("--- Boat Position ---");
			debugLines.add(String.format("Tile: (%d, %d, %d)", boatCurrentLocation.getX(), boatCurrentLocation.getY(), boatCurrentLocation.getPlane()));
		}

		debugLines.add("");
		debugLines.add("--- Rum Locations ---");
		appendRumLocationDebugInfo(debugLines);

		return debugLines;
	}

	private void appendRumLocationDebugInfo(List<String> debugLines)
	{
		WorldPoint rumPickupLocation = plugin.getGameState().getRumPickupLocation();
		WorldPoint rumReturnLocation = plugin.getGameState().getRumReturnLocation();

		if (rumPickupLocation != null)
		{
			GameObject rumPickupObject = findGameObjectAtWorldPoint(rumPickupLocation);
			String pickupInfoLine = String.format("Pickup (S): (%d, %d, %d)",
				rumPickupLocation.getX(), rumPickupLocation.getY(), rumPickupLocation.getPlane());
			if (rumPickupObject != null)
			{
				pickupInfoLine += String.format(" [ID: %d]", rumPickupObject.getId());
			}
			debugLines.add(pickupInfoLine);
		}
		else
		{
			debugLines.add("Pickup (S): null");
		}

		if (rumReturnLocation != null)
		{
			GameObject rumReturnObject = findGameObjectAtWorldPoint(rumReturnLocation);
			String returnInfoLine = String.format("Return (N): (%d, %d, %d)",
				rumReturnLocation.getX(), rumReturnLocation.getY(), rumReturnLocation.getPlane());
			if (rumReturnObject != null)
			{
				returnInfoLine += String.format(" [ID: %d]", rumReturnObject.getId());
			}
			debugLines.add(returnInfoLine);
		}
		else
		{
			debugLines.add("Return (N): null");
		}
	}

	private int calculateAndIncrementLabelOffset(net.runelite.api.Point canvasPoint)
	{
		net.runelite.api.Point roundedCanvasPoint = new net.runelite.api.Point(
			(canvasPoint.getX() / 10) * 10,
			(canvasPoint.getY() / 10) * 10
		);

		int existingLabelCount = labelCountsByCanvasPosition.getOrDefault(roundedCanvasPoint, 0);
		labelCountsByCanvasPosition.put(roundedCanvasPoint, existingLabelCount + 1);

		int pixelsPerLabel = 15;
		return existingLabelCount * pixelsPerLabel;
	}

	private String buildObjectLabelWithImpostorInfo(GameObject gameObject)
	{
		ObjectComposition objectComposition = client.getObjectDefinition(gameObject.getId());

		String displayName;
		if (objectComposition != null && objectComposition.getName() != null)
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

		LocalPoint localPoint = LocalPoint.fromWorld(topLevelWorldView, worldPoint);
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
}
