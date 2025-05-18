package com.rr.bosses.yama;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;

public class HighlightPlayerOverlay extends Overlay {
    private final Client client;
    private final YamaUtilitiesPlugin plugin;
    private final YamaUtilitiesConfig config;
    private final ModelOutlineRenderer outlineRenderer;

    @Inject
    private HighlightPlayerOverlay(Client client, YamaUtilitiesPlugin plugin, YamaUtilitiesConfig config, ModelOutlineRenderer outlineRenderer){
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.outlineRenderer = outlineRenderer;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public Dimension render(Graphics2D graphics){

        if(config.showGlyphProtectionIndicator())
        {
            Color highlightColor = getCurrentColor();
            if (highlightColor != null)
            {
                outlineRenderer.drawOutline(client.getLocalPlayer(), config.highlightBorderWidth(), highlightColor, 5);
            }
        }
        return null;
    }

    private Color getCurrentColor()
    {
        if (plugin.getRemainingTicksOfFireGlyphProtection() > 0 && plugin.getRemainingTicksOfShadowGlyphProtection() > 0)
        {
            return config.bothGlyphsColor();
        }
        else if (plugin.getRemainingTicksOfFireGlyphProtection() > 0)
        {
            return config.fireGlyphColor();
        }
        else if (plugin.getRemainingTicksOfShadowGlyphProtection() > 0)
        {
            return config.shadowGlyphColor();
        }
        return null;
    }

}
