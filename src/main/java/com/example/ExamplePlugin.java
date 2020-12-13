package com.example;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
	name = "Runecrafting counter"
)
public class ExamplePlugin extends Plugin
{
	private static final int SPELL_CONTACT_ANIMATION_ID = 4413;

	private static final int[] AREAS_CLOSE_TO_ZMI = {9778, 12119};
	private static final int MED_POUCH = 5510;
	private static final int LARGE_POUCH = 5512;
	private static final int GIANT_POUCH = 5514;

	private static final int MED_POUCH_USES = 44*6;
	private static final int LARGE_POUCH_USES = 31*9;
	private static final int GIANT_POUCH_USES = 120;

	@Getter
	private Map<Integer, Integer> itemUses = new HashMap<Integer, Integer>() {{
			put(MED_POUCH, 0);
			put(LARGE_POUCH, 0);
			put(GIANT_POUCH, 0);
	}};

	public final Map<Integer, Integer> maxItemUses = new HashMap<Integer, Integer>() {{
		put(MED_POUCH, MED_POUCH_USES);
		put(LARGE_POUCH, LARGE_POUCH_USES);
		put(GIANT_POUCH, GIANT_POUCH_USES);
	}};

	private Multiset<Integer> previousInventorySnapshot;
	private int lastClickedItem = -1;

	@Getter
	private boolean isClose = false;

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
	protected void shutDown() throws Exception {
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

		if (!playerName.equals(actorName)) {
			return;
		}

		int animId = event.getActor().getAnimation();
		if (animId == SPELL_CONTACT_ANIMATION_ID) {
			itemUses.replaceAll((k, v) -> 0);
		}

	}

	@Subscribe
	public void onGameTick(GameTick tick) {

		isClose = isCloseToZMI();
	}

	private boolean isCloseToZMI()
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return false;
		}

		WorldPoint location = local.getWorldLocation();
		//log.info("RegionID: {}", location.getRegionID());
		for (int area : AREAS_CLOSE_TO_ZMI) {
			if (location.getRegionID() == area)
				return true;
		}
		return false;
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
		if (itemsRemoved.isEmpty()){
			log.info("Did not actually fill anything...");
			return;
		}


		int removedItemCount = (int)itemsRemoved.stream().filter(k -> k == ItemID.DAEYALT_ESSENCE || k == ItemID.PURE_ESSENCE).count();
		log.info("Stored {} items", removedItemCount);
		itemUses.put(lastClickedItem, itemUses.get(lastClickedItem)+removedItemCount);
		lastClickedItem = -1;
	}

	@Subscribe
	public void onMenuOptionClicked(final MenuOptionClicked event)
	{
		lastClickedItem = -1;
		if (event.getMenuOption() == null || !event.getMenuOption().equals("Fill")) {
			return;
		}
		int inventoryIndex = event.getActionParam();
		final int itemId;
		final String itemName;

		if (event.getWidgetId() == WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getId()) {
			ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
			Item item = inventoryContainer.getItem(inventoryIndex);
			itemId = item.getId();
			itemName = item.toString();
		} else {
			final ItemComposition itemComposition = itemManager.getItemComposition(event.getId());
			itemId = itemComposition.getId();
			itemName = itemComposition.getName();
		}

		if (!itemUses.containsKey(itemId)) {
			log.info("Filled an item that we don't know about: {} with ID: {}", itemName, itemId);
			return;
		}
		previousInventorySnapshot = getInventorySnapshot();
		lastClickedItem = itemId;
	}
}
