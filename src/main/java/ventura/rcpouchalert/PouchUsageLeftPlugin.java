/*
 * Copyright (c) 2020, David Ventura
 * Copyright (c) 2023, Truth Forger <http://github.com/Blackberry0Pie>
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
package ventura.rcpouchalert;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
		name = "Pouch Health",
		description = "Shows how much essence you can put into your pouches before it degrades.",
		tags = {"rc", "runecraft", "runecrafting", "pouch", "rune", "essence", "degrade"}
)
public class PouchUsageLeftPlugin extends Plugin
{
	private static final int SMALL_POUCH = ItemID.SMALL_POUCH;
	private static final int MEDIUM_POUCH = ItemID.MEDIUM_POUCH;
	private static final int LARGE_POUCH = ItemID.LARGE_POUCH;
	private static final int GIANT_POUCH = ItemID.GIANT_POUCH;
	private static final int COLOSSAL_POUCH = ItemID.COLOSSAL_POUCH;

	//these are approximations as there is some variation to these
	private static final int MEDIUM_POUCH_USES = 45*6; //270
	private static final int LARGE_POUCH_USES = 29*9; //261
	private static final int GIANT_POUCH_USES = 10*12; //120
	private static final int COLOSSAL_POUCH_USES = 8*40; //320

//	private static final List<Integer> healthyPouchList = List.of(MEDIUM_POUCH, LARGE_POUCH, GIANT_POUCH, COLOSSAL_POUCH);
//	private static final List<Integer> degradedPouchList = List.of(ItemID.MEDIUM_POUCH_5511, ItemID.LARGE_POUCH_5513, ItemID.GIANT_POUCH_5515, ItemID.COLOSSAL_POUCH_26786);
	private static final List<Integer> essenceList = List.of(ItemID.DAEYALT_ESSENCE, ItemID.PURE_ESSENCE, ItemID.RUNE_ESSENCE, ItemID.GUARDIAN_ESSENCE);

	@Getter
	private Map<Integer, Integer> itemUses = new HashMap<>() {{
		put(SMALL_POUCH, 0);
		put(MEDIUM_POUCH, 0);
		put(LARGE_POUCH, 0);
		put(GIANT_POUCH, 0);
		put(COLOSSAL_POUCH, 0);
	}};

	@Getter
	private final Map<Integer, Integer> maxItemUses = new HashMap<>() {{
		put(SMALL_POUCH, -1);
		put(MEDIUM_POUCH, MEDIUM_POUCH_USES);
		put(LARGE_POUCH, LARGE_POUCH_USES);
		put(GIANT_POUCH, GIANT_POUCH_USES);
		put(COLOSSAL_POUCH, COLOSSAL_POUCH_USES);
	}};

	private final Map<Integer, Integer> pouchSize = new HashMap<>() {{
		put(SMALL_POUCH, 3);
		put(MEDIUM_POUCH, 6);
		put(LARGE_POUCH, 9);
		put(GIANT_POUCH, 12);
		put(COLOSSAL_POUCH, 40);
	}};

	//Ordered list of which pouches were filled on a tick to deduct from max capacity
	private ArrayList<Integer> filledPouches = new ArrayList<>();

	private Multiset<Integer> previousInventorySnapshot;
	private int lastClickedItem = -1;
	private String lastDialogueText = null;

	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private RCPouchOverlay rcOverlay;
	@Inject
	private ItemManager itemManager;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(rcOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(rcOverlay);
	}

	private Multiset<Integer> getInventorySnapshot()
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		Multiset<Integer> inventorySnapshot = HashMultiset.create();

		if (inventory != null)
		{
			Arrays.stream(inventory.getItems())
					.forEach(item -> inventorySnapshot.add(item.getId(), item.getQuantity()));
		}

		return inventorySnapshot;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (previousInventorySnapshot == null) {
			previousInventorySnapshot = getInventorySnapshot();
			return;
		}
		if (lastClickedItem == -1) return;
		Multiset<Integer> currentInventorySnapshot = getInventorySnapshot();
		final Multiset<Integer> itemsRemoved = Multisets.difference(previousInventorySnapshot, currentInventorySnapshot);
		if (itemsRemoved.isEmpty())
		{
			log.info("Did not actually fill anything...");
			return;
		}

		int removedEssenceCount = (int)itemsRemoved.stream().filter(essenceList::contains).count();

		//Iterate over pouches filled in a single tick in order they were clicked, incrementing respective pouch usage by at most the maximum capacity of each pouch; Break loop when all essence stored on the tick has been accounted for
		//This is still a bit buggy and doesn't always like updating each pouch correctly if pouches are clicked on rapidly, but it's an improvement over taking the total stored essence amount out of only the last pouch clicked when filling multiple pouches simultaneously
		for (int pouchID : filledPouches) {
			if (!itemUses.containsKey(pouchID)) {
				log.warn("itemUses does not contain key: " + pouchID);
				continue;
			}
			int essAmount = Math.min(removedEssenceCount, pouchSize.get(pouchID));
			itemUses.put(pouchID, itemUses.get(pouchID) + essAmount);
			log.info("Stored {} items", essAmount);
			if (removedEssenceCount <= pouchSize.get(pouchID)) {
				break;
			} else {
				removedEssenceCount = removedEssenceCount - pouchSize.get(pouchID);
			}
		}
		filledPouches.clear();
		lastClickedItem = -1;
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked event)
	{
		lastClickedItem = -1;
		if (event.getMenuOption() == null || !event.getMenuOption().equals("Fill")) {
			return;
		}

		//get inventory index of Essence Pouch being filled
		int inventoryIndex = event.getParam0();
		final int itemId;
		final String itemName;

		ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
		if (inventoryContainer == null)
			return;

		Item item = inventoryContainer.getItem(inventoryIndex);
		if (item == null)
			return;
		itemId = item.getId();
		itemName = item.toString();
		if(!filledPouches.contains(itemId))
		{
			filledPouches.add(itemId);
		}
		log.info("Filled pouches: {}", filledPouches);

		if (!itemUses.containsKey(itemId)) {
			log.info("Filled an item that we don't know about: {} with ID: {}", itemName, itemId);
			return;
		}

		previousInventorySnapshot = getInventorySnapshot();
		lastClickedItem = itemId;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick) {
		Widget npcDialogueTextWidget = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
		if (npcDialogueTextWidget != null && !npcDialogueTextWidget.getText().equals(lastDialogueText))
		{
			lastDialogueText = npcDialogueTextWidget.getText();
			Widget npcNameWidget = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
			if (npcNameWidget != null && npcNameWidget.getText().equals("Dark Mage"))
			{
				//log.info("last dialogue: {}", lastDialogueText);
				if (lastDialogueText.equals("Fine. A simple transfiguration spell should resolve things<br>for you.") ||
						lastDialogueText.equals("There, I have repaired your pouches. Now leave me<br>alone. I'm concentrating!") ||
						lastDialogueText.equals("OK...It's done.")) //TODO check NPC contact
				{
					//log.info("essence pouches repaired");
					//Repair all pouches
					itemUses.replaceAll((k, v) -> 0);
				}
			}
		}
	}
}
