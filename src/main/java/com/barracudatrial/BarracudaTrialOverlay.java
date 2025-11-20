package com.barracudatrial;

import net.runelite.api.Client;
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
	private final BarracudaTrialConfig config;
	private final BarracudaTrialPathRenderer pathRenderer;
	private final BarracudaTrialObjectRenderer objectRenderer;
	private final BarracudaTrialDebugRenderer debugRenderer;

	private int frameCounter = 0;
	private Map<net.runelite.api.Point, Integer> labelCounts = new HashMap<>();

	@Inject
	public BarracudaTrialOverlay(Client client, BarracudaTrialPlugin plugin, BarracudaTrialConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.config = config;
		this.pathRenderer = new BarracudaTrialPathRenderer(client, plugin, config);
		this.objectRenderer = new BarracudaTrialObjectRenderer(client, plugin, config, modelOutlineRenderer);
		this.debugRenderer = new BarracudaTrialDebugRenderer(client, plugin, config, modelOutlineRenderer);

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		frameCounter++;
		labelCounts.clear();

		if (!plugin.isInTrialArea())
		{
			return null;
		}

		// Share label tracking across all renderers to prevent overlap
		objectRenderer.setLabelCounts(labelCounts);
		debugRenderer.setLabelCounts(labelCounts);

		if (config.showOptimalPath())
		{
			pathRenderer.renderOptimalPath(graphics, frameCounter);
		}

		if (config.highlightLostSupplies())
		{
			objectRenderer.renderLostSupplies(graphics);
		}

		if (config.highlightClouds())
		{
			objectRenderer.renderLightningClouds(graphics);
		}

		if (config.highlightRocks())
		{
			objectRenderer.renderRocks(graphics);
		}

		if (config.highlightSpeedBoosts())
		{
			objectRenderer.renderSpeedBoosts(graphics);
		}

		if (config.highlightRumLocations())
		{
			objectRenderer.renderRumLocations(graphics);
		}

		if (config.debugMode())
		{
			debugRenderer.renderDebugInfo(graphics);
		}

		return null;
	}
}
