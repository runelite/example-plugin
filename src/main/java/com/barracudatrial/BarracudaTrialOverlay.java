package com.barracudatrial;

import com.barracudatrial.rendering.DebugRenderer;
import com.barracudatrial.rendering.ObjectRenderer;
import com.barracudatrial.rendering.PathRenderer;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

public class BarracudaTrialOverlay extends Overlay
{
	private final BarracudaTrialPlugin plugin;
	private final PathRenderer pathRenderer;
	private final ObjectRenderer objectRenderer;
	private final DebugRenderer debugRenderer;

	private int currentFrameNumber = 0;
	private Map<Point, Integer> screenPositionLabelCounts = new HashMap<>();

	@Inject
	public BarracudaTrialOverlay(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.pathRenderer = new PathRenderer(client, plugin);
		this.objectRenderer = new ObjectRenderer(client, plugin, modelOutlineRenderer);
		this.debugRenderer = new DebugRenderer(client, plugin, modelOutlineRenderer);

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		currentFrameNumber++;
		screenPositionLabelCounts.clear();

		if (!plugin.getGameState().isInTrialArea())
		{
			return null;
		}

		// Share label tracking across all renderers to prevent overlap
		objectRenderer.setLabelCounts(screenPositionLabelCounts);
		debugRenderer.setLabelCounts(screenPositionLabelCounts);

		CachedConfig cachedConfig = plugin.getCachedConfig();

		if (cachedConfig.isShowOptimalPath())
		{
			pathRenderer.renderOptimalPath(graphics, currentFrameNumber);
		}

		if (cachedConfig.isHighlightLostSupplies())
		{
			objectRenderer.renderLostSupplies(graphics);
		}

		if (cachedConfig.isDebugMode())
		{
			objectRenderer.renderVisibleSupplyLocations(graphics);
		}

		if (cachedConfig.isHighlightClouds())
		{
			objectRenderer.renderLightningClouds(graphics);
		}

		if (cachedConfig.isHighlightRocks())
		{
			objectRenderer.renderRocks(graphics);
		}

		if (cachedConfig.isHighlightSpeedBoosts())
		{
			objectRenderer.renderSpeedBoosts(graphics);
		}

		if (cachedConfig.isHighlightRumLocations())
		{
			objectRenderer.renderRumLocations(graphics);
		}

		if (cachedConfig.isDebugMode())
		{
			debugRenderer.renderDebugInfo(graphics);
		}

		return null;
	}
}
