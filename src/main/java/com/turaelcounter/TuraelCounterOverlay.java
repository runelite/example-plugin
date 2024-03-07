package com.turaelcounter;

import java.awt.image.BufferedImage;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.plugins.Plugin;

public class TuraelCounterOverlay extends Counter {

    @Getter
    private final int streakReset;

    TuraelCounterOverlay(Plugin plugin, int streakReset, BufferedImage image) {
        super(image, plugin, streakReset);
        this.streakReset = getStreakReset();
    }

    @Override
    public String getText() {
        int resetCount = TuraelCounterPlugin.streakReset;
        return String.valueOf(resetCount);
    }
}

//    @Override
//    public Dimension render(Graphics2D graphics)
//    {
//        panelComponent.getChildren().clear();
//
//        int resetCount = TuraelCounterPlugin.streakReset;
//
//        final String turaelMessage = String.valueOf(resetCount);
//
//        panelComponent.getChildren().add((TitleComponent.builder())
//                .text(turaelMessage)
//                .build());
//
////        panelComponent.setPreferredSize(new Dimension(100, 20));
//
////        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
//
//        return super.render(graphics);
//    }

