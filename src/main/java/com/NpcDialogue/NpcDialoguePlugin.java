/*
 * Copyright (c) 2018, Woox <https://github.com/wooxsolo>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package NpcDialogue;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
        name = "NPC dialogue",
        description = "Utility to make it easier to transcribe NPC dialogue for OSRS Wiki.",
        enabledByDefault = false
)

public class NpcDialoguePlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    private String lastNpcDialogueText = null;
    private String lastPlayerDialogueText = null;
    private Widget[] dialogueOptions;
    private NpcDialoguePanel panel;
    private NavigationButton navButton;

    @Override
    public void startUp()
    {
        // Shamelessly copied from NotesPlugin
        panel = injector.getInstance(NpcDialoguePanel.class);
        panel.init();

        // Hack to get around not having resources.
        final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "dialogue_icon.png");

        navButton = NavigationButton.builder()
                .tooltip("NPC dialogue")
                .icon(icon)
                .priority(100)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        if (menuOptionClicked.getMenuAction() == MenuAction.WIDGET_TYPE_6 && menuOptionClicked.getMenuOption().equals("Continue")) {
            int actionParam = menuOptionClicked.getActionParam();
            // if -1, "Click here to continue"
            if (actionParam > 0 && actionParam < dialogueOptions.length) {
                panel.appendText("<chose " + dialogueOptions[actionParam].getText() + ">");
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Widget npcDialogueTextWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);

        if (npcDialogueTextWidget != null && npcDialogueTextWidget.getText() != lastNpcDialogueText) {
            String npcText = npcDialogueTextWidget.getText();
            lastNpcDialogueText = npcText;

            String npcName = client.getWidget(WidgetInfo.DIALOG_NPC_NAME).getText();
            panel.appendText(npcName + ": " + npcText);
        }

        // This should be in WidgetInfo under DialogPlayer, but isn't currently.
        Widget playerDialogueTextWidget = client.getWidget(WidgetID.DIALOG_PLAYER_GROUP_ID, 4);

        if (playerDialogueTextWidget != null && playerDialogueTextWidget.getText() != lastPlayerDialogueText) {
            String playerText = playerDialogueTextWidget.getText();
            lastPlayerDialogueText = playerText;

            panel.appendText("Player: " + playerText);
        }

        Widget playerDialogueOptionsWidget = client.getWidget(WidgetID.DIALOG_OPTION_GROUP_ID, 1);
        if (playerDialogueOptionsWidget != null && playerDialogueOptionsWidget.getChildren() != dialogueOptions) {
            dialogueOptions = playerDialogueOptionsWidget.getChildren();
            for (int i = 1; i < dialogueOptions.length - 2; i++) {
                panel.appendText("Option: " + dialogueOptions[i].getText());
            }
        }
    }
}
