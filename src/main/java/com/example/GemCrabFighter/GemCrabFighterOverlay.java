package com.example.GemCrabFighter;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class GemCrabFighterOverlay extends OverlayPanel {

    private final Client client;
    private final GemCrabFighterPlugin plugin;
    private final GemCrabFighterConfig config;

    @Inject
    private GemCrabFighterOverlay(Client client, GemCrabFighterPlugin plugin, GemCrabFighterConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.isPluginStarted()) {
            return null;
        }

        // Title
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Gem Crab Fighter")
                .color(Color.GREEN)
                .build());

        // Status
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status:")
                .right(plugin.isPluginStarted() ? "Running" : "Stopped")
                .rightColor(plugin.isPluginStarted() ? Color.GREEN : Color.RED)
                .build());

        // Current State
        panelComponent.getChildren().add(LineComponent.builder()
                .left("State:")
                .right(plugin.getCurrentState().toString())
                .rightColor(getStateColor(plugin.getCurrentState()))
                .build());

        // Combat Style
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Combat:")
                .right(config.combatStyle())
                .build());

        // Health
        int currentHp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        panelComponent.getChildren().add(LineComponent.builder()
                .left("HP:")
                .right(currentHp + "/" + maxHp)
                .rightColor(getHealthColor(currentHp, maxHp))
                .build());

        // Prayer
        if (config.usePrayer()) {
            int currentPrayer = client.getBoostedSkillLevel(Skill.PRAYER);
            int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Prayer:")
                    .right(currentPrayer + "/" + maxPrayer)
                    .rightColor(getPrayerColor(currentPrayer, maxPrayer))
                    .build());
        }

        // Special Attack
        if (config.useSpecWeapon()) {
            int specEnergy = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Spec:")
                    .right(specEnergy + "%")
                    .rightColor(specEnergy >= config.specEnergyThreshold() ? Color.GREEN : Color.ORANGE)
                    .build());
        }

        return super.render(graphics);
    }

    private Color getStateColor(State state) {
        switch (state) {
            case FIGHTING:
                return Color.RED;
            case FIND_CRAB:
                return Color.YELLOW;
            case SWAP_CAVE:
                return Color.ORANGE;
            case EQUIP_GEAR:
                return Color.CYAN;
            case USE_SPEC:
                return Color.MAGENTA;
            case IDLE:
            default:
                return Color.WHITE;
        }
    }

    private Color getHealthColor(int current, int max) {
        double percentage = (double) current / max * 100;
        if (percentage <= 30) {
            return Color.RED;
        } else if (percentage <= 60) {
            return Color.ORANGE;
        } else {
            return Color.GREEN;
        }
    }

    private Color getPrayerColor(int current, int max) {
        double percentage = (double) current / max * 100;
        if (percentage <= 20) {
            return Color.RED;
        } else if (percentage <= 50) {
            return Color.ORANGE;
        } else {
            return Color.CYAN;
        }
    }
}