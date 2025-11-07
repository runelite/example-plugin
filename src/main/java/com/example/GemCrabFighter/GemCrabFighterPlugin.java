package com.example.GemCrabFighter;

import com.example.GemCrabFighter.data.*;
import com.example.PiggyUtils.API.*;
import com.example.PacketUtils.WidgetInfoExtended;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import lombok.extern.slf4j.Slf4j;
import com.example.EthanApiPlugin.EthanApiPlugin;

import javax.net.ssl.KeyManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@PluginDescriptor(
        name = "Gem Crab Fighter",
        description = "Automatically fights the Gemrock crab with optional combat style switching",
        tags = {"KungKnudas", "GemCrab", "ethan"}
)
@Slf4j
public class GemCrabFighterPlugin extends Plugin {
    public static Player player;
    public static NPC currentNPC;
    public static WorldPoint deathLocation;

    public static List<Integer> inventorySetup = new ArrayList<>();

    @Getter
    private State state;
    @Getter
    private SubState subState;

    // booleans
    @Getter
    private boolean started;
    private boolean deposited = false;

    // timers
    private Instant timer;
    private int timeout;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private GemCrabFighterConfig config;
    @Inject
    private GemCrabFighterOverlay overlay;
    @Inject
    private KeyManager keyManager;

    @Inject
    public PlayerUtil playerUtil;

    @Override
    protected void startUp() throws Exception {
        log.info("Gemcrab Fighter started!");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Gemcrab Fighter stopped!");
    }

    @Provides
    private GemCrabFighterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(GemCrabFighterConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            // We do an early return if the user isn't logged in\
            return;
        }
        if (!playerUtil.isAutoRetaliating()) {
            EthanApiPlugin.sendClientMessage("[GemCrabAIO]TURN ON AUTO RETALIATE");
            EthanApiPlugin.stopPlugin(this);
            return;
        }
        player = client.getLocalPlayer();
        if (player == null || !started) {
            return;
        }
        if (client.getWidget(WidgetInfo.BANK_PIN_CONTAINER) != null) {
            log.info("Enter bank pin manually");
            return;
        }
        state = getCurrentState();
        subState = getCurrentSubState();
        switch (state) {
            case TIMEOUT:
                timeout--;
                return;
            case LOGOUT:
                EthanApiPlugin.sendClientMessage("We are missing a teleport to house tab, stopping plugin.");
                resetPlugin();
                return;
            case ANIMATING:
                return;
        }
        switch (subState) {
            case DRINK_POTIONS:
                drinkPotions();
                timeout = tickDelay();
                break;
            case EAT_FOOD:
                eatFood();
                break;
            case EQUIP_GEAR:
                equipGear();
                break;
            case USE_SPECIAL:
                useSpec();
                break;
            case TELE_GE:
                teleportToGe();
                break;
            case TELE_CIVITAS:
                teleportCivitas();
                break;
            case ATTACK_CRAB:
                attackCrab();
                break;
            case MINE_CRAB:
                mineCrab();
                break;
            case MOVE_DOWNSTAIRS:
                walkDownstairs();
                break;
            case OPEN_DOOR:
                enterDownstairs();
                break;
            case WALK_DOOR:
                walkDoor();
                break;
            case ENTER_LAIR:
                enterLair();
                break;
            case FIND_BANK:
                interactWithBank();
                break;
            case DEPOSIT:
                depositItems();
                break;
            case WITHDRAW:
                withdrawItems();
                break;
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged e) {
        if (e.getGroup().equals("GemCrabFighter") && e.getKey().equals("startButton")) {
            toggle();
        }
    }

    // Utils
    public State getCurrentState() {
        if (timeout > 0) {
            return State.TIMEOUT;
        }

        if (EthanApiPlugin.isMoving()) {
            timeout = tickDelay();
            return State.ANIMATING;
        }

        if(shouldBank() && !InventoryUtil.hasItems("Fire Rune", "Earth Rune", "Law Rune") || InventoryUtil.hasItem(ItemID.RUNE_PICKAXE)) {
            return State.LOGOUT;
        }

        if(inCrabArea() && !shouldBank()) {
            return State.FIND_CRAB;
        }
        }
    }

    private void resetPlugin() {
        started = false;
        overlayManager.remove(runeDragonsOverlay);
        overlayManager.remove(runeDragonsTileOverlay);
        inventorySetup.clear();
        itemsToLoot.clear();
        timer = null;
        currentNPC = null;
        timeout = 0;
        deposited = false;

    }

    protected int tickDelay() {
        return MathUtil.random(config.tickDelayMin(), config.tickDelayMax());
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged e) {
        if (e.getGroup().equals("RuneDragons") && e.getKey().equals("startButton")) {
            toggle();
        }
    }

    @Subscribe
    private void onActorDeath(ActorDeath event) {
        if (!started) {
            return;
        }
        if (event.getActor() == currentNPC) {
            deathLocation = event.getActor().getWorldLocation();
            log.debug("Our npc died, updating deathLocation: {}", deathLocation.toString());
            currentNPC = null;
            killCount++;
        }
    }

    @Subscribe
    private void onItemSpawned(ItemSpawned event) {
        if (!started || !inDragons()) {
            return;
        }
        if (lootableItem(event)) {
            log.debug("Adding loot item: {}", client.getItemDefinition(event.getItem().getId()).getName());
            itemsToLoot.add(event);
        }
    }

    @Subscribe
    private void onItemDespawned(ItemDespawned itemDespawned) {
        if (!started || !inDragons()) {
            return;
        }
        itemsToLoot.removeIf(itemSpawned -> itemSpawned.getItem().getId() == itemDespawned.getItem().getId());
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned)
    {
        NPC npc = npcSpawned.getNpc();

        if (npc.getId() == Constants.GEMSTONE_CRAB_ID)
        {
            log.debug("Gemstone Crab boss spawned");
            bossPresent = true;
            notificationSent = false;


            resetTunnel();
            // Start a new DPS tracking session
            // This is where we reset stats - when a new boss spawns
            resetDpsTracking();
            isTop16Damager = false;
            fightInProgress = true;
            fightStartTime = System.currentTimeMillis();
            setLastMiningAttempt();
            log.debug("New boss spawned, resetting DPS stats");
        }
        // Track crab shells in the scene
        else if (npc.getId() == Constants.GEMSTONE_CRAB_SHELL_ID)
        {
            WorldPoint location = npc.getWorldLocation();
            shells.put(location, npc);
            log.debug("Crab shell NPC spawned at {}", location);

        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned)
    {
        NPC npc = npcDespawned.getNpc();

        if (npc.getId() == Constants.GEMSTONE_CRAB_ID)
        {
            log.debug("Gemstone Crab boss despawned");
            bossPresent = false;

            // When the boss dies, highlight the nearest tunnel
            if (config.highlightTunnel())
            {
                findNearestTunnel();
                shouldHighlightTunnel = true;
                log.debug("Boss died, highlighting nearest tunnel");
            }

            // Also find and highlight the shell in red by default
            updateCrabShell();
            // Set to true but not top 16 yet (will be red)
            shouldHighlightShell = true;

            log.debug("Boss died, highlighting shell in red");

        }
        else if (npc.getId() == Constants.GEMSTONE_CRAB_SHELL_ID)
        {
            // If the despawned NPC is our tracked shell, clear it
            if (crabShell != null && npc.equals(crabShell))
            {
                crabShell = null;
            }
            // Also remove from our shell map
            shells.remove(npc.getWorldLocation());
            mineCountShell = 0;
        }
    }

    /**
     * Check if the player is within any of the three Gemstone Crab areas
     * @return true if player is in any of the three areas
     */
    public boolean isPlayerInGemstoneArea()
    {
        if (client == null || client.getLocalPlayer() == null)
        {
            return false;
        }

        // Get player's world location
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (playerLocation == null)
        {
            return false;
        }

        // Checks if player is close enough to any of the crabs
        return playerLocation.distanceTo2D(Constants.EAST_CRAB) <= Constants.DISTANCE_THRESHOLD
                || playerLocation.distanceTo2D(Constants.SOUTH_CRAB) <= Constants.DISTANCE_THRESHOLD
                || playerLocation.distanceTo2D(Constants.NORTH_CRAB) <= Constants.DISTANCE_THRESHOLD;
    }

}
