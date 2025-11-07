package com.example.PowerSkiller;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import ChinBreakHandler.ChinBreakHandler;

import lombok.extern.slf4j.Slf4j;
import com.google.inject.Inject;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;

import java.time.Instant;
import java.util.*;

@Slf4j
@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font>PowerSkillerPlugin</html>",
        description = "Will interact with an object and drop or bank all items when inventory is full",
        tags = {"ethan", "piggy", "skilling"}
)
public class PowerSkillerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    PowerSkillerConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PowerSkillerOverlay overlay;
    @Inject
    private ChinBreakHandler chinBreakHandler;
    State state;
    boolean started;

    boolean bankPin = false;

    private int timeout;

    @Override
    protected void startUp() throws Exception {
        chinBreakHandler.registerPlugin(this, true);
        bankPin = false;
        keyManager.registerKeyListener(toggle);
        this.overlayManager.add(overlay);
    }


    @Override
    protected void shutDown() throws Exception {
        bankPin = false;
        keyManager.unregisterKeyListener(toggle);
        chinBreakHandler.unregisterPlugin(this);
        this.overlayManager.remove(overlay);
    }

    @Provides
    private PowerSkillerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(PowerSkillerConfig.class);
    }

    private int debugEveryXTicks = 5;
    private int tickCounter = 0;

    @Subscribe
    private void onGameTick(GameTick event) {

        tickCounter++;
        state = getNextState();

        if (!EthanApiPlugin.loggedIn() || !started) {
            // We do an early return if the user isn't logged in
            return;
        }


        // DETAILED DEBUGGING
        boolean breakActive = chinBreakHandler.isBreakActive(this);
        Map<Plugin, Instant> activeBreaks = chinBreakHandler.getActiveBreaks();
        Map<Plugin, Instant> plannedBreaks = chinBreakHandler.getPlannedBreaks();

        if (tickCounter % 50 == 0) { // Log every 50 ticks
            log.info("=== Break Handler Status ===");
            log.info("isBreakActive: {}", breakActive);
            log.info("Active breaks size: {}", activeBreaks.size());
            log.info("Planned breaks size: {}", plannedBreaks.size());
            log.info("This plugin in active: {}", activeBreaks.containsKey(this));
            log.info("This plugin in planned: {}", plannedBreaks.containsKey(this));

            if (!plannedBreaks.isEmpty()) {
                for (Map.Entry<Plugin, Instant> entry : plannedBreaks.entrySet()) {
                    log.info("Planned: {} at {}", entry.getKey().getName(), entry.getValue());
                }
            }
        }

        if (breakActive) {
            log.info("Break is active, pausing plugin");
            return;
        }

        state = getNextState();


        if (tickCounter % debugEveryXTicks == 0) {
            /*log.info("[PS] state={}, invFull={}, anim={}, moving={}",
                    state, Inventory.full(), client.getLocalPlayer().getAnimation(),
                    EthanApiPlugin.isMoving()); */
        }

        handleState();
    }

    private void handleState() {
        switch (state) {
            case BANK:
                // Handle bank PIN
                if (Widgets.search().withId(13959169).first().isPresent()) {
                    bankPin = true;
                    return;
                }

                // If bank is already open, deposit items
                if (Widgets.search().withId(786445).first().isPresent()) {
                    List<Widget> items = BankInventory.search().result();
                    for (Widget item : items) {
                        if (!isTool(item.getName().toLowerCase()) &&
                                !shouldKeep(item.getName().toLowerCase())) {
                            BankInventoryInteraction.useItem(item, "Deposit-All");
                            return;
                        }
                    }
                    // All items deposited, change state
                    state = State.FIND_OBJECT; // or whatever your next state is
                    return;
                }

                // Bank not open - try to open it
                // Try bank objects first
                Optional<TileObject> bankObject = TileObjects.search()
                        .withAction("Bank")
                        .nearestToPlayer();

                if (bankObject.isPresent()) {
                    TileObjectInteraction.interact(bankObject.get(), "Bank");
                    timeout = 5; // Wait for bank to open
                    return;
                }

                // Try bank chests
                Optional<TileObject> bankChest = TileObjects.search()
                        .withName("Bank chest")
                        .nearestToPlayer();

                if (bankChest.isPresent()) {
                    TileObjectInteraction.interact(bankChest.get(), "Use");
                    timeout = 5;
                    return;
                }

            /* Uncomment when fixed
            Optional<NPC> banker = NPCs.search()
                .withAction("Bank")
                .nearestToPlayer();

            if (banker.isPresent()) {
                NPCInteraction.interact(banker.get(), "Bank");
                timeout = 5;
                return;
            }
            */

                // Only show error if nothing was found
                EthanApiPlugin.sendClientMessage("Bank is not found, move to an area with a bank.");
                break;

            case TIMEOUT:
                timeout--;
                if (timeout <= 0) {
                    state = State.BANK; // or appropriate next state
                }
                break;

            case FIND_OBJECT:
                if (config.searchNpc()) {
                    findNpc();
                } else {
                    findObject();
                }
                setTimeout();
                break;

            case DROP_ITEMS:
                dropItems();
                break;
        }
    }

    private State getNextState() {
        // self-explanatory, we just return a State if the conditions are met.

        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            // this is to prevent clicks while animating/moving.
            return State.ANIMATING;
        }

        if (!hasTools()) {
            // if the user doesn't have tools we don't want it to do anything at all lol, maybe stop the plugin if you want.
            return State.MISSING_TOOLS;
        }
        if (timeout > 0) {
            // If the timeout is above 0, we want to decrement it and return the timeout state.
            return State.TIMEOUT;
        }

        if (shouldBank() && Inventory.full() || (Bank.isOpen() && !isInventoryReset())) {
            // If the user should be banking and the inventory is full, or if the bank is open and the inventory isn't reset, we want to bank.
            if (shouldBank() && !isInventoryReset()) {
                return State.BANK;
            }
        }

        if ((isDroppingItems() && !isInventoryReset()) || !shouldBank() && Inventory.full()) {
            // if the user should be dropping items, we'll check if they're done
            return State.DROP_ITEMS;
        }

        // default it'll look for an object.
        return State.FIND_OBJECT;
    }
        // Implementation details below
        // 1. search the runelite screen (tiles) for the GameObject input.
        // 2. If found, return the location of the gameobject (I think) or the gameobject..
        // check if gameobjects are unique, i.e. the location can be extracted later
        // Handle multiple matches, i.e.
        // 3. if not, return null

    private void findObject() {
        String objectName = config.objectToInteract();
        if (config.useForestryTreeNotClosest() && config.expectedAction().equalsIgnoreCase("chop")) {
            TileObjects.search().withName(objectName).nearestToPoint(getObjectWMostPlayers()).ifPresent(tileObject -> {
                ObjectComposition comp = TileObjectQuery.getObjectComposition(tileObject);
                TileObjectInteraction.interact(tileObject, comp.getActions()[0]);
            });
        } else {
            TileObjects.search().withName(objectName).nearestToPlayer().ifPresent(tileObject -> {
                ObjectComposition comp = TileObjectQuery.getObjectComposition(tileObject);
                TileObjectInteraction.interact(tileObject, comp.getActions()[0]); // find the object we're looking for.  this specific example will only work if the first Action the object has is the one that interacts with it.
                // don't *always* do this, you can manually type the possible actions. eg. "Mine", "Chop", "Cook", "Climb".
            });
        }
    }

    /**
     * Tile w most players on it within 2 tiles of the object we're looking for
     *
     * @return That tile or the player's tile if failed(such as doing forestry option when alone by trees)
     */
    public WorldPoint getObjectWMostPlayers() {
        String objectName = config.objectToInteract();
        Map<WorldPoint, Integer> playerCounts = new HashMap<>();
        WorldPoint mostPlayersTile = null;
        int highestCount = 0;
        List<TileObject> objects = TileObjects.search().withName(objectName).result();

        List<Player> players = Players.search().notLocalPlayer().result();

        for (TileObject object : objects) {
            for (Player player : players) {
                if (player.getWorldLocation().distanceTo(object.getWorldLocation()) <= 2) {
                    WorldPoint playerTile = player.getWorldLocation();
                    playerCounts.put(playerTile, playerCounts.getOrDefault(playerTile, 0) + 1);
                    if (playerCounts.get(playerTile) > highestCount) {
                        highestCount = playerCounts.get(playerTile);
                        mostPlayersTile = playerTile;
                    }
                }
            }
        }

        return mostPlayersTile == null ? client.getLocalPlayer().getWorldLocation() : mostPlayersTile;
    }


    private void findNpc() {
        String npcName = config.objectToInteract();
        NPCs.search().withName(npcName).nearestToPlayer().ifPresent(npc -> {
            NPCComposition comp = client.getNpcDefinition(npc.getId());
            if (Arrays.stream(comp.getActions()).anyMatch(action -> action.equalsIgnoreCase(config.expectedAction()))) {
                NPCInteraction.interact(npc, config.expectedAction()); // For fishing spots ?
            } else {
                NPCInteraction.interact(npc, comp.getActions()[0]);
            }

        });
    }

    private void dropItems() {
        List<Widget> itemsToDrop = Inventory.search()
                .filter(item -> !shouldKeep(item.getName()) && !isTool(item.getName())).result(); // filter the inventory to only get the items we want to drop

        for (int i = 0; i < Math.min(itemsToDrop.size(), RandomUtils.nextInt(config.dropPerTickOne(), config.dropPerTickTwo())); i++) {
            InventoryInteraction.useItem(itemsToDrop.get(i), "Drop"); // we'll loop through this at a max of 10 times.  can make this a config options.  drops x items per tick (x = 10 in this example)
        }
    }

    private boolean isInventoryReset() {
        List<Widget> inventory = Inventory.search().result();
        for (Widget item : inventory) {
            if (!shouldKeep(Text.removeTags(item.getName()))) { // using our shouldKeep method, we can filter the items here to only include the ones we want to drop.
                return false;
            }
        }
        return true; // we will know that the inventory is reset because the inventory only contains items we want to keep
    }

    private boolean isDroppingItems() {
        return state == State.DROP_ITEMS; // if the user is dropping items, we don't want it to proceed until they're all dropped.
    }


    private boolean shouldKeep(String name) {
        List<String> itemsToKeep = new ArrayList<>(List.of(config.itemsToKeep().split(","))); // split the items listed by comma. and add them to a list.
        itemsToKeep.addAll(List.of(config.toolsToUse().split(","))); //We must also check if the tools are included in the Inventory, Rather than equipped, so they are added here
        return itemsToKeep.stream()// stream the List using Collection.stream() from java.util
                .anyMatch(i -> Text.removeTags(name.toLowerCase()).contains(i.toLowerCase()));
        // we'll set everything to lowercase as well as remove the html tags that is included (The color of the item in game),
        // and check if the input name contains any of the items in the itemsToKeep list.
        // might seem silly, but this is to allow specific items you want to keep without typing the full name.
        // We also prefer names to ids here, but you can change this if you like.
    }

    private boolean hasTools() {
        //Updated from https://github.com/moneyprinterbrrr/ImpactPlugins/blob/experimental/src/main/java/com/impact/PowerGather/PowerGatherPlugin.java#L196
        //Big thanks hawkkkkkk
        String[] tools = config.toolsToUse().split(","); // split the tools listed by comma, no space.

        int numInventoryTools = Inventory.search()
                .filter(item -> isTool(item.getName())) // filter inventory by using out isTool method
                .result().size();
        int numEquippedTools = Equipment.search()
                .filter(item -> isTool(item.getName())) // filter inventory by using out isTool method
                .result().size();

        return numInventoryTools + numEquippedTools >= tools.length; // if the size of tools and the filtered inventory is the same, we have our tools.
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    private boolean isTool(String name) {
        String[] tools = config.toolsToUse().split(","); // split the tools listed by comma, no space.

        return Arrays.stream(tools) // stream the array using Arrays.stream() from java.util
                .anyMatch(i -> name.toLowerCase().contains(i.toLowerCase())); // more likely for user error than the shouldKeep option, but we'll follow the same idea as shouldKeep.
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    private boolean shouldBank() {
        return true;
        /*
        boolean configShouldBank = config.shouldBank();
        boolean tileBankPresent = TileObjects.search().withAction("Bank").nearestToPlayer().isPresent();
        boolean tileBankChestPresent = TileObjects.search().withName("Bank chest").nearestToPlayer().isPresent();
        boolean npcBankPresent = NPCs.search().withAction("Bank").nearestToPlayer().isPresent();
        boolean result = configShouldBank && tileBankPresent && npcBankPresent && !bankPin;
        /*log.info("Tileobject search first(): " + TileObjects.search().withAction("Bank").first());
        log.info("Tileobject search neareastToPlayer(): " + TileObjects.search().withAction("Bank").nearestToPlayer());
        log.info("Should bank? {} (config={}, tileBankPresent={}, npcBankPresent={}, bankPin={}, tileBankChestPresent={})",
                result, configShouldBank, tileBankPresent, npcBankPresent, bankPin, tileBankChestPresent); */ /*
        log.info("pathtogoal testing: "+ EthanApiPlugin.pathToGoalFromPlayerNoCustomTiles(new WorldPoint(3092,3246,0)));
        EthanApiPlugin.pathToGoalFromPlayerNoCustomTiles(new WorldPoint(3092,3246,0));

        return result; */
    }

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;

        if (started) {
            chinBreakHandler.startPlugin(this);

            // Force an immediate break for testing
            chinBreakHandler.planBreak(this, Instant.now().plusSeconds(5));
            log.info("Forced break planned in 5 seconds");
        }
    }
}