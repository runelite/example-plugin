// java
package com.example.GemCrabFighter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

@Singleton
public class GemCrabFighterOverlay extends OverlayPanel {

    private final Client client;
    private final GemCrabFighterPlugin plugin;
    private final GemCrabFighterConfig config;

    @Inject
    private GemCrabFighterOverlay(Client client, GemCrabFighterPlugin gemCrabFighterPlugin, GemCrabFighterConfig gemCrabFighterConfig) {
        super(gemCrabFighterPlugin);
        this.client = client;
        this.plugin = gemCrabFighterPlugin;
        this.config = gemCrabFighterConfig;
        setPosition(OverlayPosition.TOP_LEFT);
        setPreferredSize(new Dimension(200, 200));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isPluginStarted()) {
            return null; // Don't render if plugin isn't started
        }

        panelComponent.setPreferredSize(new Dimension(200, 320));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Knudas Gem Crab AIO")
                .color(new Color(100, 200, 255))
                .build());

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.isPluginStarted() ? "Running" : "Paused")
                .color(plugin.isPluginStarted() ? Color.GREEN : Color.RED)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Elapsed Time: ")
                .leftColor(Color.YELLOW)
                .right(plugin.isPluginStarted() ? plugin.getElapsedTime() : "00:00:00")
                .rightColor(Color.WHITE)
                .build());

        // Main state
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Main State: ")
                .leftColor(new Color(100, 200, 255))
                .right(plugin.getState() == null ? "STOPPED" : plugin.getState().name())
                .rightColor(Color.WHITE)
                .build());

        // Sub state
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Sub State: ")
                .leftColor(new Color(100, 200, 255))
                .right(plugin.getSubState() == null ? "STOPPED" : plugin.getSubState().name())
                .rightColor(Color.WHITE)
                .build());

        return super.render(graphics);
    }
}