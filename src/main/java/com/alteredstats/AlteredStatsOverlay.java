package com.alteredstats;

import javax.inject.Inject;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.util.ColorUtil;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class AlteredStatsOverlay extends OverlayPanel
{
    private final Client client;
    private final com.alteredstats.AlteredStatsConfig config;
    private final Skill[] skills = Arrays.stream(Skill.values()).sorted(Comparator.comparing(Skill::getName)).toArray(Skill[]::new);

    @Inject
    private AlteredStatsOverlay(Client client, AlteredStatsConfig config, AlteredStatsPlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPriority(OverlayPriority.MED);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Altered stats overlay."));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Skill[] skills = this.skills;
        if (!config.includeHP()) {
            skills = Arrays.stream(this.skills).filter(s -> s != Skill.HITPOINTS).toArray(Skill[]::new);
        }
        for (Skill skill: skills) {
            int boosted = client.getBoostedSkillLevel(skill);
            int level = client.getRealSkillLevel(skill);
            if(boosted != level || config.showAllStats()) {
                setOverlayText(skill, boosted, level);
            }
        }

        return super.render(graphics);
    }

    private void setOverlayText(Skill skill, int boosted, int base)
    {
        int delta = boosted - base;
        final Color strColor = getTextColor(delta);

        String s;
        switch (config.displayType()) {
            case LEVELS:
                s = Integer.toString(boosted);
                break;
            case LEVELS_WITH_BASES:
                s = String.format("%s/%s", boosted, base);
                break;
            default: // case DELTAS:
                s = (delta < 0 ? "" : "+") + delta;
        }

        String str = ColorUtil.prependColorTag(s, strColor);

        panelComponent.getChildren().add(LineComponent.builder()
                .left(skill.getName())
                .right(str)
                .rightColor(strColor)
                .build());
    }

    private Color getTextColor(int boost)
    {
        if (boost == 0) return Color.WHITE;
        return boost < 0 ? Color.RED : Color.GREEN;
    }
}
