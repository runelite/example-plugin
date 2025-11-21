package com.barracudatrial.rendering;

import com.barracudatrial.BarracudaTrialConfig;
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
	private final BarracudaTrialConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Map<net.runelite.api.Point, Integer> labelCountsByCanvasPosition;

	public ObjectRenderer(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
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

	public void renderLostSupplies(Graphics2D graphics)
	{
		for (GameObject lostSupplyObject : plugin.getGameState().getLostSupplies())
		{
			String debugLabel = null;
			if (config.showIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(lostSupplyObject, "Lost Supplies");
			}
			renderGameObjectWithHighlight(graphics, lostSupplyObject, config.lostSuppliesColor(), config.showLostSuppliesTile(), debugLabel);
		}
	}

	public void renderLightningClouds(Graphics2D graphics)
	{
		for (NPC cloudNpc : plugin.getGameState().getLightningClouds())
		{
			int currentAnimation = cloudNpc.getAnimation();

			boolean isCloudSafe = plugin.isCloudSafe(currentAnimation);
			if (isCloudSafe)
			{
				continue;
			}

			Color dangerousCloudColor = config.cloudColor();

			renderCloudDangerAreaOnGround(graphics, cloudNpc, dangerousCloudColor);

			String debugLabel = null;
			if (config.showIDs())
			{
				debugLabel = String.format("Cloud (ID: %d, Anim: %d)", cloudNpc.getId(), cloudNpc.getAnimation());
			}
			renderNpcWithHighlight(graphics, cloudNpc, dangerousCloudColor, true, debugLabel);
		}
	}

	public void renderRocks(Graphics2D graphics)
	{
		for (GameObject rockObject : plugin.getGameState().getRocks())
		{
			String debugLabel = null;
			if (config.showIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(rockObject, "Rock");
			}
			renderGameObjectWithHighlight(graphics, rockObject, config.rockColor(), true, debugLabel);
		}
	}

	public void renderSpeedBoosts(Graphics2D graphics)
	{
		for (GameObject speedBoostObject : plugin.getGameState().getSpeedBoosts())
		{
			String debugLabel = null;
			if (config.showIDs())
			{
				debugLabel = buildObjectLabelWithImpostorInfo(speedBoostObject, "Speed Boost");
			}
			renderGameObjectWithHighlight(graphics, speedBoostObject, config.speedBoostColor(), true, debugLabel);
		}
	}

	public void renderRumLocations(Graphics2D graphics)
	{
		Color rumHighlightColor = config.rumLocationColor();

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
		if (objectLocalPoint == null)
		{
			return;
		}

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
		LocalPoint cloudCenterPoint = cloudNpc.getLocalLocation();
		if (cloudCenterPoint == null)
		{
			return;
		}

		int dangerRadiusInTiles = config.cloudDangerRadius();

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
		net.runelite.api.Point labelCanvasPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, labelText, heightOffsetInPixels);
		if (labelCanvasPoint != null)
		{
			int yOffsetToAvoidLabelOverlap = calculateAndIncrementLabelOffset(labelCanvasPoint);
			net.runelite.api.Point adjustedCanvasPoint = new net.runelite.api.Point(labelCanvasPoint.getX(), labelCanvasPoint.getY() + yOffsetToAvoidLabelOverlap);
			OverlayUtil.renderTextLocation(graphics, adjustedCanvasPoint, labelText, labelColor);
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
