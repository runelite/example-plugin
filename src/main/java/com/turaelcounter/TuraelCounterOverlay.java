package com.turaelcounter;

import net.runelite.client.ui.overlay.OverlayPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.infobox.Counter;

public class TuraelCounterOverlay extends OverlayPanel {

    private final TuraelCounterConfig config;
    private final Client client;

    @Inject
    private TuraelCounterOverlay(TuraelCounterConfig config, Client client)
    {
        this.config = config;
        this.client = client;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        int resetCount = TuraelCounterPlugin.streakReset;

        final String turaelMessage = String.valueOf(resetCount);

        panelComponent.getChildren().add((TitleComponent.builder())
                .text(turaelMessage)
                .build());

        panelComponent.setPreferredSize(new Dimension(100, 20));

        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);

        return super.render(graphics);
    }
}
