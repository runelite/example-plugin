package com.turaelcounter;

import java.awt.*;
import java.awt.image.BufferedImage;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import java.awt.Color;

public class TuraelStreakInfobox extends InfoBox {

    private final TuraelCounterPlugin plugin;

    public TuraelStreakInfobox(BufferedImage image, TuraelCounterPlugin plugin)
    {
        super(image, plugin);
        this.plugin = plugin;
    }

    @Override
    public String getText()
    {
        if (plugin != null)
        {
            return String.valueOf(plugin.getStreakReset());
        }
        else
        {
            return "n/a";
        }
    }

    @Override
    public Color getTextColor() {
        return Color.RED;
    }
}

