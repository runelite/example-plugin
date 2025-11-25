package com.barracudatrial;

import com.barracudatrial.game.route.TrialType;
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

	private final Map<Point, Integer> screenPositionLabelCounts = new HashMap<>();

	@Inject
	public BarracudaTrialOverlay(Client client, BarracudaTrialPlugin plugin, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.objectRenderer = new ObjectRenderer(client, plugin, modelOutlineRenderer);
		this.pathRenderer = new PathRenderer(client, plugin, objectRenderer);
		this.debugRenderer = new DebugRenderer(client, plugin);

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		screenPositionLabelCounts.clear();

		if (!plugin.getGameState().isInTrialArea())
		{
			return null;
		}

		// Share label tracking across all renderers to prevent overlap
		objectRenderer.setLabelCountsByCanvasPosition(screenPositionLabelCounts);
		debugRenderer.setLabelCountsByCanvasPosition(screenPositionLabelCounts);

		CachedConfig cachedConfig = plugin.getCachedConfig();

		if (cachedConfig.isShowOptimalPath())
		{
			pathRenderer.renderOptimalPath(graphics);
		}

		if (cachedConfig.isHighlightObjectives())
		{
			objectRenderer.renderLostSupplies(graphics);
		}

		var trial = plugin.getGameState().getCurrentTrial();
		if (cachedConfig.isHighlightClouds() && trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM)
		{
			objectRenderer.renderLightningClouds(graphics);
		}

		if (cachedConfig.isHighlightFetidPools() && trial != null && trial.getTrialType() == TrialType.JUBBLY_JIVE)
		{
			objectRenderer.renderFetidPools(graphics);
		}

		if (cachedConfig.isHighlightObjectives() && trial != null && trial.getTrialType() == TrialType.JUBBLY_JIVE)
		{
			objectRenderer.renderToadPillars(graphics);
			objectRenderer.renderToadPickup(graphics);
		}

		if (cachedConfig.isHighlightSpeedBoosts())
		{
			objectRenderer.renderSpeedBoosts(graphics);
		}

		if (cachedConfig.isHighlightObjectives() && trial != null && trial.getTrialType() == TrialType.TEMPOR_TANTRUM)
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
