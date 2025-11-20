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

	private Map<net.runelite.api.Point, Integer> labelCounts;

	public ObjectRenderer(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	public void setLabelCounts(Map<net.runelite.api.Point, Integer> labelCounts)
	{
		this.labelCounts = labelCounts;
	}

	public void renderLostSupplies(Graphics2D graphics)
	{
		for (GameObject supply : plugin.getLostSupplies())
		{
			String label = null;
			if (config.showIDs())
			{
				label = getObjectLabelWithImpostor(supply, "Lost Supplies");
			}
			renderGameObject(graphics, supply, config.lostSuppliesColor(), config.showLostSuppliesTile(), label);
		}
	}

	public void renderLightningClouds(Graphics2D graphics)
	{
		for (NPC cloud : plugin.getLightningClouds())
		{
			int animation = cloud.getAnimation();

			// Only render dangerous clouds
			if (plugin.isCloudSafe(animation))
			{
				continue;
			}

			Color cloudColor = config.cloudColor();

			// Draw danger area for dangerous clouds
			renderCloudDangerArea(graphics, cloud, cloudColor);

			// Draw the cloud itself
			String label = null;
			if (config.showIDs())
			{
				label = String.format("Cloud (ID: %d, Anim: %d)", cloud.getId(), cloud.getAnimation());
			}
			renderNPC(graphics, cloud, cloudColor, true, label);
		}
	}

	public void renderRocks(Graphics2D graphics)
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

	public void renderSpeedBoosts(Graphics2D graphics)
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

	public void renderRumLocations(Graphics2D graphics)
	{
		Color rumColor = config.rumLocationColor();

		// Highlight the active rum location based on whether we're carrying rum
		if (plugin.isHasRumOnUs())
		{
			// Carrying rum - highlight dropoff location
			WorldPoint returnLocation = plugin.getRumReturnLocation();
			if (returnLocation != null)
			{
				renderRumLocation(graphics, returnLocation, rumColor);
			}
		}
		else
		{
			// Not carrying rum - highlight pickup location
			WorldPoint pickupLocation = plugin.getRumPickupLocation();
			if (pickupLocation != null)
			{
				renderRumLocation(graphics, pickupLocation, rumColor);
			}
		}
	}

	private void renderRumLocation(Graphics2D graphics, WorldPoint rumLocation, Color rumColor)
	{
		GameObject rumObject = findGameObjectAt(rumLocation);
		if (rumObject != null)
		{
			renderGameObject(graphics, rumObject, rumColor, true, null);
		}
		else
		{
			// Fallback: highlight just the tile if object not found
			renderTileAtWorldPoint(graphics, rumLocation, rumColor);
		}
	}

	private void renderGameObject(Graphics2D graphics, GameObject gameObject, Color color, boolean showTile, String label)
	{
		LocalPoint localPoint = gameObject.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		// Highlight tile
		if (showTile)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}

		// Highlight model
		modelOutlineRenderer.drawOutline(gameObject, 2, color, 4);

		// Draw label
		if (label != null)
		{
			renderLabelAtLocalPoint(graphics, localPoint, label, color, 0);
		}
	}

	private void renderNPC(Graphics2D graphics, NPC npc, Color color, boolean showTile, String label)
	{
		LocalPoint localPoint = npc.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		// Highlight tile
		if (showTile)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}

		// Highlight model
		modelOutlineRenderer.drawOutline(npc, 2, color, 4);

		// Draw label
		if (label != null)
		{
			renderLabelAtLocalPoint(graphics, localPoint, label, color, npc.getLogicalHeight() + 40);
		}
	}

	private void renderCloudDangerArea(Graphics2D graphics, NPC npc, Color color)
	{
		LocalPoint center = npc.getLocalLocation();
		if (center == null)
		{
			return;
		}

		int radius = config.cloudDangerRadius();

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

	private void renderTileAtWorldPoint(Graphics2D graphics, WorldPoint worldPoint, Color color)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(topLevelWorldView, worldPoint);
		if (localPoint == null)
		{
			return;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, color);
		}
	}

	private void renderLabelAtLocalPoint(Graphics2D graphics, LocalPoint localPoint, String label, Color color, int heightOffset)
	{
		net.runelite.api.Point textPoint = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, heightOffset);
		if (textPoint != null)
		{
			int yOffset = getAndIncrementLabelOffset(textPoint);
			net.runelite.api.Point offsetPoint = new net.runelite.api.Point(textPoint.getX(), textPoint.getY() + yOffset);
			OverlayUtil.renderTextLocation(graphics, offsetPoint, label, color);
		}
	}

	private int getAndIncrementLabelOffset(net.runelite.api.Point canvasPoint)
	{
		// Round position to nearest 10 pixels to group nearby labels
		net.runelite.api.Point roundedPoint = new net.runelite.api.Point(
			(canvasPoint.getX() / 10) * 10,
			(canvasPoint.getY() / 10) * 10
		);

		int count = labelCounts.getOrDefault(roundedPoint, 0);
		labelCounts.put(roundedPoint, count + 1);

		// 15 pixels per label to prevent overlap
		return count * 15;
	}

	private String getObjectLabelWithImpostor(GameObject obj, String typeName)
	{
		ObjectComposition comp = client.getObjectDefinition(obj.getId());

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

		// Return first non-null GameObject on tile
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
