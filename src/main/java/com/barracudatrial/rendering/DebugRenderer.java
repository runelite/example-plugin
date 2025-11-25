package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import com.barracudatrial.game.route.JubblyJiveConfig;
import com.barracudatrial.game.route.TemporTantrumConfig;
import com.barracudatrial.game.route.TrialType;
import lombok.Setter;
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

	@Setter
	private Map<Point, Integer> labelCountsByCanvasPosition;

	public DebugRenderer(Client client, BarracudaTrialPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
	}

	public void renderDebugInfo(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		renderExclusionZoneVisualization(graphics);
		renderBoatExclusionZones(graphics);

		renderFrontBoatTileDebug(graphics);
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
					LocalPoint tileLocalPoint = ObjectRenderer.localPointFromWorldIncludingExtended(topLevelWorldView, tileWorldPoint);

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
		LocalPoint centerLocalPoint = ObjectRenderer.localPointFromWorldIncludingExtended(topLevelWorldView, exclusionZoneCenterPoint);
		if (centerLocalPoint != null)
		{
			Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, centerLocalPoint, "EXCLUSION ZONE", 0);
			if (labelCanvasPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, labelCanvasPoint, "EXCLUSION ZONE", new Color(255, 0, 255, 255));
			}
		}
	}

	private void renderBoatExclusionZones(Graphics2D graphics)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		Color boatExclusionColor = new Color(255, 100, 0, 60); // Orange

		var trial = plugin.getGameState().getCurrentTrial();
		if (trial != null)
		{
			if (trial.getTrialType() == TrialType.TEMPOR_TANTRUM && trial instanceof TemporTantrumConfig)
			{
				var tempor = (TemporTantrumConfig)trial;
				renderBoatExclusionZone(graphics, topLevelWorldView,
					tempor.getRumPickupLocation(),
					"BOAT (PICKUP)", boatExclusionColor);

				renderBoatExclusionZone(graphics, topLevelWorldView,
					tempor.getRumDropoffLocation(),
					"BOAT (DROPOFF)", boatExclusionColor);
			}
			if (trial.getTrialType() == TrialType.JUBBLY_JIVE && trial instanceof JubblyJiveConfig)
			{
				var jubbly = (JubblyJiveConfig)trial;
				renderBoatExclusionZone(graphics, topLevelWorldView,
					jubbly.getToadPickupLocation(),
					"TOAD PICKUP", boatExclusionColor);
			}
		}
	}

	private void renderBoatExclusionZone(Graphics2D graphics, WorldView worldView, WorldPoint center, String label, Color color)
	{
		int width = TemporTantrumConfig.BOAT_EXCLUSION_WIDTH;
		int height = TemporTantrumConfig.BOAT_EXCLUSION_HEIGHT;

		int halfWidth = width / 2;
		int halfHeight = height / 2;

		int minX = center.getX() - halfWidth;
		int maxX = center.getX() + halfWidth;
		int minY = center.getY() - halfHeight;
		int maxY = center.getY() + halfHeight;

		for (int x = minX; x <= maxX; x++)
		{
			for (int y = minY; y <= maxY; y++)
			{
				WorldPoint tileWorldPoint = new WorldPoint(x, y, 0);
				LocalPoint tileLocalPoint = ObjectRenderer.localPointFromWorldIncludingExtended(worldView, tileWorldPoint);

				if (tileLocalPoint != null)
				{
					Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tileLocalPoint);
					if (tilePolygon != null)
					{
						OverlayUtil.renderPolygon(graphics, tilePolygon, color);
					}
				}
			}
		}

		LocalPoint centerLocalPoint = ObjectRenderer.localPointFromWorldIncludingExtended(worldView, center);
		if (centerLocalPoint != null)
		{
			Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, centerLocalPoint, label, 0);
			if (labelCanvasPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, labelCanvasPoint, label, new Color(255, 100, 0, 255));
			}
		}
	}

	private void renderFrontBoatTileDebug(Graphics2D graphics)
	{
		var state = plugin.getGameState();
		if (!state.isInTrialArea())
		{
			return;
		}

		// Draw the stored front boat tile (LocalPoint - boat-relative, rotates with boat)
		LocalPoint frontBoatTileLocal = state.getFrontBoatTileLocal();
		if (frontBoatTileLocal != null)
		{
			Polygon frontTilePolygon = Perspective.getCanvasTilePoly(client, frontBoatTileLocal);
			if (frontTilePolygon != null)
			{
				Color frontTileColor = new Color(255, 0, 255, 120); // Magenta
				OverlayUtil.renderPolygon(graphics, frontTilePolygon, frontTileColor);
			}
			Point frontLabelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, frontBoatTileLocal, "FRONT-LOCAL", 0);
			if (frontLabelCanvasPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, frontLabelCanvasPoint, "FRONT-LOCAL", Color.MAGENTA);
			}
		}

		// Draw the estimated actual world tile (WorldPoint - static world coordinates, for pathfinding)
		WorldPoint frontBoatTileActual = state.getFrontBoatTileEstimatedActual();
		if (frontBoatTileActual != null)
		{
			WorldView topLevelWorldView = client.getTopLevelWorldView();
			if (topLevelWorldView != null)
			{
				LocalPoint actualLocal = ObjectRenderer.localPointFromWorldIncludingExtended(topLevelWorldView, frontBoatTileActual);
				if (actualLocal != null)
				{
					Polygon actualTilePolygon = Perspective.getCanvasTilePoly(client, actualLocal);
					if (actualTilePolygon != null)
					{
						Color actualTileColor = new Color(0, 255, 255, 120); // Cyan
						OverlayUtil.renderPolygon(graphics, actualTilePolygon, actualTileColor);
					}
					Point actualLabelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, actualLocal, "FRONT-ACTUAL", 0);
					if (actualLabelCanvasPoint != null)
					{
						OverlayUtil.renderTextLocation(graphics, actualLabelCanvasPoint, "FRONT-ACTUAL", Color.CYAN);
					}
				}
			}
		}
	}

	private void renderDebugTextOverlay(Graphics2D graphics)
	{
		int textStartX = 10;
		int textStartY = 150;
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
		debugLines.add(String.format("Lap: %d / %d", plugin.getGameState().getCurrentLap(), plugin.getGameState().getRumsNeeded()));
		debugLines.add(String.format("Lost Supplies Visible: %d", plugin.getGameState().getLostSupplies().size()));
		debugLines.add(String.format("LostSupplies: %d / %d", plugin.getGameState().getLostSuppliesCollected(), plugin.getGameState().getLostSuppliesTotal()));
		debugLines.add(String.format("Rum: %d / %d", plugin.getGameState().getRumsCollected(), plugin.getGameState().getRumsNeeded()));
		debugLines.add(String.format("Current Path: %d points", plugin.getGameState().getCurrentSegmentPath().size()));
		debugLines.add(String.format("Waypoints in Route: %d", plugin.getGameState().getCurrentStaticRoute().size()));
		debugLines.add(String.format("Completed Waypoints: %d", plugin.getGameState().getCompletedWaypointIndices().size()));
		debugLines.add(String.format("Next Path: %d points", plugin.getGameState().getNextSegmentPath().size()));
		debugLines.add(String.format("Last Path Recalc: %s", plugin.getGameState().getLastPathRecalcCaller()));
		debugLines.add("");
		debugLines.add("--- Visible Objects ---");
		debugLines.add(String.format("Lightning Clouds: %d", plugin.getGameState().getLightningClouds().size()));
		debugLines.add(String.format("Rocks (visible): %d", plugin.getGameState().getKnownRockLocations().size()));
		debugLines.add(String.format("Speed Boosts (visible): %d", plugin.getGameState().getSpeedBoosts().size()));
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
			GameObject rumPickupObject = ObjectRenderer.findGameObjectAtWorldPoint(client, rumPickupLocation);
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
			GameObject rumReturnObject = ObjectRenderer.findGameObjectAtWorldPoint(client, rumReturnLocation);
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

}
