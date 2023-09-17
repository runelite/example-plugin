/*
 * Copyright (c) 2018, Seth <http://github.com/sethtroll>
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
package com.example;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.*;

class RCPouchOverlay extends WidgetItemOverlay
{
    private final PouchUsageLeft plugin;

    @Inject
    private RCPouchOverlay(PouchUsageLeft plugin)
    {
        showOnInventory();
        this.plugin = plugin;
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
    {

        if (!this.plugin.getItemUses().containsKey(itemId)) return;

        final Rectangle bounds = itemWidget.getCanvasBounds();
        final TextComponent textComponent = new TextComponent();
        textComponent.setPosition(new Point(bounds.x - 1, bounds.y + bounds.height));

        int usesLeft = this.plugin.maxItemUses.get(itemId) - this.plugin.getItemUses().get(itemId);
        if (usesLeft > 12) {
            textComponent.setColor(Color.WHITE);
        } else {
            textComponent.setColor(Color.RED);
        }

        //Prevent rendering of usage counter on Small Pouch
        if (usesLeft > 0) {
            textComponent.setText(Integer.toString(usesLeft));

            textComponent.render(graphics);
        }
    }
}
