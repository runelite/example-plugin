package com.example;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
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
		name = "Runecrafting counter"
)
public class PouchUsageLeft extends Plugin
{
	private static final int SPELL_CONTACT_ANIMATION_ID = 4413;

	private static final int SMALL_POUCH = ItemID.SMALL_POUCH;
	private static final int MED_POUCH = ItemID.MEDIUM_POUCH;
	private static final int LARGE_POUCH = ItemID.LARGE_POUCH;
	private static final int GIANT_POUCH = ItemID.GIANT_POUCH;
	private static final int COLOSSAL_POUCH = ItemID.COLOSSAL_POUCH;

	private static final int MED_POUCH_USES = 44*6;
	private static final int LARGE_POUCH_USES = 31*9;
	private static final int GIANT_POUCH_USES = 10*12;
	private static final int COLOSSAL_POUCH_USES = 8*40;

	@Getter
	private Map<Integer, Integer> itemUses = new HashMap<Integer, Integer>() {{
		put(SMALL_POUCH, 0);
		put(MED_POUCH, 0);
		put(LARGE_POUCH, 0);
		put(GIANT_POUCH, 0);
		put(COLOSSAL_POUCH, 0);
	}};

	public final Map<Integer, Integer> maxItemUses = new HashMap<Integer, Integer>() {{
		put(SMALL_POUCH, -1);
		put(MED_POUCH, MED_POUCH_USES);
		put(LARGE_POUCH, LARGE_POUCH_USES);
		put(GIANT_POUCH, GIANT_POUCH_USES);
		put(COLOSSAL_POUCH, COLOSSAL_POUCH_USES);
	}};

	public final Map<Integer, Integer> pouchSize = new HashMap<Integer, Integer>() {{
		put(SMALL_POUCH, 3);
		put(MED_POUCH, 6);
		put(LARGE_POUCH, 9);
		put(GIANT_POUCH, 12);
		put(COLOSSAL_POUCH, 40);
	}};

	//Ordered list of which pouches were filled on a tick to deduct from max capacity
	public ArrayList<Integer> filledPouches = new ArrayList<Integer>();

	private Multiset<Integer> previousInventorySnapshot;
	private int lastClickedItem = -1;

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

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}

		String playerName = client.getLocalPlayer().getName();
		String actorName = event.getActor().getName();

		if (!playerName.equals(actorName))
		{
			return;
		}

		int animId = event.getActor().getAnimation();
		if (animId == SPELL_CONTACT_ANIMATION_ID)
		{
			itemUses.replaceAll((k, v) -> 0);
		}

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

		if (previousInventorySnapshot == null) return;
		if (lastClickedItem == -1) return;
		Multiset<Integer> currentInventorySnapshot = getInventorySnapshot();
		final Multiset<Integer> itemsRemoved = Multisets.difference(previousInventorySnapshot, currentInventorySnapshot);
		if (itemsRemoved.isEmpty())
		{
			log.info("Did not actually fill anything...");
			return;
		}

		int removedItemCount = (int)itemsRemoved.stream().filter(k -> k == ItemID.DAEYALT_ESSENCE || k == ItemID.PURE_ESSENCE || k == ItemID.RUNE_ESSENCE || k == ItemID.GUARDIAN_ESSENCE).count();
		log.info("Stored {} items", removedItemCount);

		//Iterate over pouches filled in a single tick in order they were clicked, incrementing respective pouch usage by at most the maximum capacity of each pouch; Break loop when all essence stored on the tick has been accounted for
		//This is still a bit buggy and doesn't always like updating each pouch correctly if pouches are clicked on rapidly, but it's an improvement over taking the total stored essence amount out of only the last pouch clicked when filling multiple pouches simultaneously
		for (int i = 0; i < filledPouches.size(); i++)
		{
			int pouchID = filledPouches.get(i);
			itemUses.put(pouchID, itemUses.get(pouchID) + Math.min(removedItemCount, pouchSize.get(pouchID)));
			if (removedItemCount <= pouchSize.get(pouchID))
			{
				break;
			}
			else
			{
				removedItemCount = removedItemCount - pouchSize.get(pouchID);
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
		Item item = inventoryContainer.getItem(inventoryIndex);
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
}
