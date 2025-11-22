package com.barracudatrial.rendering;

import com.barracudatrial.CachedConfig;
import com.barracudatrial.BarracudaTrialPlugin;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Map;

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

	public void renderVisibleSupplyLocations(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		Color highlightColor = cachedConfig.getLostSuppliesColor();
		for (WorldPoint supplyLocation : plugin.getGameState().getVisibleSupplyLocations())
		{
			renderTileHighlightAtWorldPoint(graphics, supplyLocation, highlightColor);
		}
	}

	public void renderLightningClouds(Graphics2D graphics)
	{
		CachedConfig cachedConfig = plugin.getCachedConfig();
		for (NPC cloudNpc : plugin.getGameState().getLightningClouds())
		{
			int currentAnimation = cloudNpc.getAnimation();

			boolean isCloudSafe = plugin.isCloudSafe(currentAnimation);
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
			renderNpcWithHighlight(graphics, cloudNpc, dangerousCloudColor, true, debugLabel);
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
		if (isCarryingRum)
		{
			WorldPoint rumDropoffLocation = plugin.getGameState().getRumReturnLocation();
			if (rumDropoffLocation != null)
			{
				renderRumLocationHighlight(graphics, rumDropoffLocation, rumHighlightColor);
			}
		}
		else
		{
			WorldPoint rumPickupLocation = plugin.getGameState().getRumPickupLocation();
			if (rumPickupLocation != null)
			{
				renderRumLocationHighlight(graphics, rumPickupLocation, rumHighlightColor);
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

	private void renderNpcWithHighlight(Graphics2D graphics, NPC npc, Color highlightColor, boolean shouldHighlightTile, String debugLabel)
	{
		LocalPoint npcLocalPoint = npc.getLocalLocation();
		if (npcLocalPoint == null)
		{
			return;
		}

		if (shouldHighlightTile)
		{
			Polygon tilePolygon = Perspective.getCanvasTilePoly(client, npcLocalPoint);
			if (tilePolygon != null)
			{
				OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
			}
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

	private void renderTileHighlightAtWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color highlightColor)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		LocalPoint tileLocalPoint = LocalPoint.fromWorld(topLevelWorldView, worldPoint);
		if (tileLocalPoint == null)
		{
			return;
		}

		Polygon tilePolygon = Perspective.getCanvasTilePoly(client, tileLocalPoint);
		if (tilePolygon != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePolygon, highlightColor);
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
