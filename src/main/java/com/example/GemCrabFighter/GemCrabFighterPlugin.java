package com.example.GemCrabFighter;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.PathFinding.GlobalCollisionMap;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.GemCrabFighter.data.*;
import com.example.GemCrabFighter.data.Constants;
import com.example.InteractionApi.InventoryInteraction;
import com.example.KnudsUtils.Walker;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.example.PathingTesting.PathingTesting;
import com.example.PiggyUtils.API.*;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.PiggyUtils.PiggyUtilsPlugin;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ItemID;
//import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.gameval.InterfaceID;
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
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.input.KeyManager;
import ChinBreakHandler.ChinBreakHandler;


import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.example.GemCrabFighter.data.Constants.*;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[KK] </font>Knudas Gemcrab</html>",
        description = "Simple fighter for Gemcrab boss. Supports banking and mining shells.",
        tags = {"ethan", "knudas", "GemCrab", "crab", "fighter", "aio", "combat"}
)
@PluginDependency(EthanApiPlugin.class)
@Slf4j
public class GemCrabFighterPlugin extends Plugin {
    public static Player player;
    public static NPC currentNPC;

    public static List<Integer> inventorySetup = new ArrayList<>();

    @Getter
    State state;
    @Getter
    private SubState subState;

    // booleans
    @Getter
    public boolean started;
    private boolean deposited = false;

    // Crab Chat messages
    private static final String GEMSTONE_CRAB_DEATH_MESSAGE = "The gemstone crab burrows away, leaving a piece of its shell behind.";
    private static final String GEMSTONE_CRAB_MINE_SUCCESS_MESSAGE = "You swing your pick at the crab shell.";
    private static final String GEMSTONE_CRAB_MINE_FAIL_MESSAGE = "Your understanding of the gemstone crab is not great enough to mine its shell.";
    private static final String GEMSTONE_CRAB_GEM_MINE_MESSAGE = "You mine an uncut ";
    private static final String GEMSTONE_CRAB_TOP16_MESSAGE = "You gained enough understanding of the crab to mine from its remains.";
    private static final String GEMSTONE_CRAB_MINED_ALL_MESSAGE = "You have mined all you can from the shell.";


    // Add these constants to your GemCrabFighterPlugin class
    private static final int GEMSTONE_CRAB_SHELL_ID = 14780;
    private static final long MINING_COOLDOWN_MS = 5000; // 5 seconds cooldown between mining attempts

    // Mining statistics tracking
    private int miningAttempts = 0;      // Total number of mining attempts
    private int minedCount = 0;          // Number of successful mines
    private int miningFailedCount = 0;   // Number of failed mining attempts
    private int gemsMined = 0;           // Total gems obtained from mining

    // Add these tracking variables
    private long lastMiningAttempt = 0;
    private boolean isTop16Damager = false;
    private int mineCountShell = 0;
    private final Map<WorldPoint, NPC> shells = new HashMap<>();

    // timers
    private Instant timer;
    private int timeout;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private KeyManager keyManager;
    @Inject
    private GemCrabFighterConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private GemCrabFighterOverlay gemCrabFighterOverlay;
    @Inject
    private ChinBreakHandler chinBreakHandler;

    @Inject
    public PlayerUtil playerUtil;

    // FIX 4: Alternative - Remove overlay from startUp() and only add it when toggled
    @Override
    protected void startUp() throws Exception {
        log.info("Gemcrab Fighter started!");
        keyManager.registerKeyListener(toggle);
        chinBreakHandler.registerPlugin(this);
        overlayManager.add(gemCrabFighterOverlay);
        clientThread.invoke(() -> {
            EthanApiPlugin.sendClientMessage("[GemCrabFighter] Plugin loaded!");
        });
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Gemcrab Fighter stopped!");
        overlayManager.remove(gemCrabFighterOverlay);
        chinBreakHandler.unregisterPlugin(this);
        resetPlugin();
    }

    @Provides
    private GemCrabFighterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(GemCrabFighterConfig.class);
    }

    private void logDebug(String message) {
        if (config.debugMode()) {
            clientThread.invoke(() -> {
                EthanApiPlugin.sendClientMessage("[GCF] " + message);
            });
        }
    }

    private void logError(String message) {
        log.error("[GCF] " + message);
        // Use clientThread to safely send messages
        clientThread.invoke(() -> {
            EthanApiPlugin.sendClientMessage("[GCF ERROR] " + message);
        });
    }


    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            return;
        }

        player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        if (Walker.isWalking()) {
            logDebug(" PathWalker active - handling walking");
            Walker.handleWalking(client);
            return; // Don't process other states while walking
        }

        // Add null check for playerUtil and try-catch for safety
        try {
            if (playerUtil != null && !playerUtil.isAutoRetaliating()) {
                logError("Auto retaliate is OFF - stopping plugin");
                EthanApiPlugin.stopPlugin(this);
                return;
            }
        } catch (Exception e) {
            logDebug("Could not check auto retaliate status: " + e.getMessage());
        }

        if (client.getWidget(WidgetInfo.BANK_PIN_CONTAINER) != null) {
            logDebug("Bank PIN detected - waiting");
            return;
        }

        if (timeout > 0) {
            timeout--;
            return;
        }

        state = getCurrentState();
        subState = getCurrentSubState();

        if (state == State.LOGOUT) {
            resetPlugin();
            return;
        }

        // Handle ANIMATING state
        if (state == State.ANIMATING) {
            return;
        }

        switch (subState) {
            case USE_SPECIAL:
                useSpec();
                break;
            case EQUIP_GEAR:
                equipGear();
                break;
            case ATTACK_CRAB:
                attackCrab();
                break;
            case MINE_CRAB:
                mineCrab();
                break;
            case MOVE_TO_CRAB:
                moveToCrab();
                break;
            case ENTER_CRAB_TUNNEL:
                enterTunnel();
                break;
            case TELE_FEROX:
                teleportFerox();
                break;
            case TELE_CIVITAS:
                teleportCivitas();
                break;
            case MOVE_TO_BIRD:
                moveToBird();
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
            case IDLE:
                break;
        }
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged e) {
        if (e.getGroup().equals("GemCrabFighter") && e.getKey().equals("startButton")) {
            toggle();
        }
    }


    public State getCurrentState() {
        // Check if player is moving/animating
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            timeout = tickDelay();
            return State.ANIMATING;
        }

        if (inCrabCombatArea() && !atCrabEntrance()) {
            return State.COMBAT;
        }

        if (inFeroxArea()) {
            if (shouldRestock()) {
                return State.BANKING;
            } else {
                return State.TRAVEL;
            }
        }

        if (inCivitasArea()) {
            return State.TRAVEL;
        }

        if (inTelTeklanArea()) {
            return State.TRAVEL;
        }

        // Check for required items
        /*if (!InventoryUtil.hasItems("Fire rune", "Earth rune", "Law rune")) {
            logError("Missing required runes!");
            return State.LOGOUT;
        } */

        if (!hasAnyRingOfDueling()) {
            logError("Missing Ring of Dueling!");
            return State.LOGOUT;
        }

        return State.TRAVEL;
    }

    public SubState getCurrentSubState() {

            // IMPORTANT: Only check for restock when we're NOT already in BANKING state
            // and when we don't have the required items
            if (state == State.TRAVEL && shouldRestock()) {
                if (inFeroxArea()) {
                    return SubState.FIND_BANK;
                } else {
                    return SubState.TELE_FEROX;
                }
            }

        if (state == State.COMBAT) {
            // Look for new crab
            if (currentNPC == null || !isValidCrab(currentNPC)) {

                currentNPC = NPCs.search()
                        .alive()
                        .withName("Gemstone Crab")
                        .filter(this::isValidCrab)
                        .filter(npc -> npc.getInteracting() == null)
                        .nearestToPlayer()
                        .orElse(null);

                if (currentNPC == null) {
                    currentNPC = NPCs.search()
                            .alive()
                            .withName("Gemstone Crab")
                            .filter(this::isValidCrab)
                            .filter(npc -> npc.getInteracting() != player)
                            .nearestToPlayer()
                            .orElse(null);
                }

                if (currentNPC != null) {
                    return SubState.ATTACK_CRAB;
                } else {
                    if (config.shouldMine() && !Inventory.full() && canMineShell()) {
                        NPC shell = NPCs.search().withId(GEMSTONE_CRAB_SHELL_ID)
                                .nearestToPlayer().orElse(null);
                        if (shell != null) {
                            return SubState.MINE_CRAB;
                        } else {
                            logDebug("No shells available");
                        }
                    }

                    timeout = 2 + tickDelay();
                    return SubState.IDLE;
                }
            }

            if (currentNPC != null && isValidCrab(currentNPC)) {
                if (player.getInteracting() != currentNPC) {
                    return SubState.ATTACK_CRAB;
                }

                // Special attack logic (if enabled in config)
                if (config.useSpec()) {
                    Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
                    if (mainWeapon != null) {
                        int specPercent = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);
                        boolean specEnabled = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 1;

                        if (specPercent >= config.specTreshhold() * 10 &&
                                mainWeapon.getId() != config.specId() &&
                                currentNPC == player.getInteracting()) {
                            return SubState.EQUIP_GEAR;
                        }

                        if (currentNPC == player.getInteracting() &&
                                specPercent >= config.specTreshhold() * 10 &&
                                mainWeapon.getId() == config.specId() &&
                                !specEnabled) {
                            return SubState.USE_SPECIAL;
                        }

                        if (specPercent < config.specTreshhold() * 10 &&
                                mainWeapon.getId() == config.specId()) {
                            return SubState.EQUIP_GEAR;
                        }
                    }
                }

                return SubState.IDLE;
            }
        }

        if (state == State.TRAVEL) {

            if (config.shouldMine() && !Inventory.full() && canMineShell()) {
                NPC shell = NPCs.search().withId(GEMSTONE_CRAB_SHELL_ID)
                        .nearestToPlayer().orElse(null);
                if (shell != null) {
                    return SubState.MINE_CRAB;
                }
            }

            if (atCrabEntrance()) {
                return SubState.ENTER_CRAB_TUNNEL;
            } else if (inFeroxArea()) {
                if (!shouldRestock()) {
                    return SubState.TELE_CIVITAS;
                } else {
                    return SubState.FIND_BANK;
                }
            } else if (inCivitasArea()) {
                return SubState.MOVE_TO_BIRD;
            }else if (inTelTeklanArea()) {
                return SubState.MOVE_TO_CRAB;
            }else if (!inCrabArea() && !inFeroxArea() && !inCivitasArea() && !inTelTeklanArea()) {
                logDebug("We are in the wrong area");
            }
        }

        if (state == State.BANKING) {
            if (!Bank.isOpen()) {
                return SubState.FIND_BANK;
            } else {
                if (!deposited) {
                    return SubState.DEPOSIT;
                } else {
                    return SubState.WITHDRAW;
                }
            }
        }

        return SubState.IDLE;
    }



    public String getElapsedTime() {
        if (timer == null) {
            return "00:00:00";
        }

        Duration duration = Duration.between(timer, Instant.now());
        long durationInMillis = duration.toMillis();
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;

        if (started) {
            timer = Instant.now();
            chinBreakHandler.startPlugin(this);
            initInventory();
        }
    }


    // Replace your initInventory() method with this:
    private void initInventory() {
        inventorySetup.clear();
        if (config.shouldMine()) {
            inventorySetup.add(ItemID.RUNE_PICKAXE);
        }
        if (config.useSpec()) {
            inventorySetup.add(config.specId());
        }
        inventorySetup.add(ItemID.FIRERUNE);
        inventorySetup.add(ItemID.EARTHRUNE);
        inventorySetup.add(ItemID.LAWRUNE);
        // Only add the (8) version - we'll check for any ring of dueling separately
        inventorySetup.add(ItemID.RING_OF_DUELING_8);
        log.info("required inventory items: {}", inventorySetup.toString());
    }

    // Add this helper method to check if player has ANY ring of dueling
    private boolean hasAnyRingOfDueling() {
        return Inventory.search().nameContains("Ring of dueling(").first().isPresent();
    }

    private void resetPlugin() {
        started = false;
        inventorySetup.clear();
        timer = null;
        currentNPC = null;
        timeout = 0;
        deposited = false;
        keyManager.unregisterKeyListener(toggle);
    }

    protected int tickDelay() {
        return MathUtil.random(config.tickDelayMin(), config.tickDelayMax());
    }

    protected boolean isValidCrab(NPC npc) {
        if (npc == null) return false;
        int hp = npc.getHealthRatio();
        // HP of -1 or 0 means dead/despawned
        return hp > 0 && !npc.isDead();
    }


    protected boolean atCrabEntrance() {
        // At entrance ONLY if:
        // 1. We can see the Cave object nearby
        // 2. We CANNOT see any Gemstone Crabs (means we're outside)
        boolean canSeeCave = TileObjects.search().withName("Cave")
                .withinDistance(10)
                .first()
                .isPresent();

        boolean canSeeCrabs = NPCs.search()
                .withName("Gemstone Crab")
                .first()
                .isPresent();

        // At entrance = can see cave BUT cannot see crabs
        return canSeeCave && !canSeeCrabs;
    }

    protected boolean inCrabCombatArea() {
        // Inside combat area if we can see Gemstone Crabs
        return NPCs.search()
                .withName("Gemstone Crab")
                .first()
                .isPresent();
    }

    // Helper method to check if we can mine shells
    private boolean canMineShell() {
        // Check mining cooldown (prevents duplicate counting)
        if (isMiningBeforeCooldown()) {
            if (config.debugMode()) {
                EthanApiPlugin.sendClientMessage("Mining on cooldown");
            }
            return false;
        }

        // Check if we have a pickaxe equipped or in inventory
        Item weapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
        boolean hasPickaxe = (weapon != null && weapon.getId() == ItemID.RUNE_PICKAXE) ||
                InventoryUtil.hasItem(ItemID.RUNE_PICKAXE);

        if (!hasPickaxe) {
            if (config.debugMode()) {
                EthanApiPlugin.sendClientMessage("No pickaxe available");
            }
            return false;
        }

        // Check if we're a top 16 damager (required to mine successfully)
        if (!isTop16Damager) {
            if (config.debugMode()) {
                EthanApiPlugin.sendClientMessage("Not top 16 damager - cannot mine");
                timeout = tickDelay();
            }
            return false;
        }

        return true;
    }

    private void mineCrab() {
        NPC shell = NPCs.search().withId(GEMSTONE_CRAB_SHELL_ID)
                .nearestToPlayer().orElse(null);

        if (shell != null) {
            NPCInteraction.interact(shell, "Mine");
        } else {
            timeout = tickDelay();
        }
    }

    // Cooldown check to prevent duplicate mining counts
    private boolean isMiningBeforeCooldown() {
        return System.currentTimeMillis() - lastMiningAttempt < MINING_COOLDOWN_MS;
    }

    // Set last mining attempt time
    private void setLastMiningAttempt() {
        lastMiningAttempt = System.currentTimeMillis();
    }

    protected boolean inCrabArea() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(CRAB_REGIONS::contains);
        if (config.debugMode()) {
            EthanApiPlugin.sendClientMessage("We are in CrabArea - " + status);
        }
        return status;
    }

    protected boolean inFeroxArea() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(FEROX_REGIONS::contains);
        if (config.debugMode()) {
            EthanApiPlugin.sendClientMessage("We are in FeroxArea - " + status);
        }
        return status;
    }

    protected boolean inCivitasArea() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(CIVITAS_REGIONS::contains);
        if (config.debugMode()) {
            EthanApiPlugin.sendClientMessage("We are in CivitasArea - " + status);
        }
        return status;
    }

    protected boolean inTelTeklanArea() {
        boolean status = Arrays.stream(client.getMapRegions()).anyMatch(TEL_TEKLAN_REGIONS::contains);
        if (config.debugMode()) {
            EthanApiPlugin.sendClientMessage("We are in Tel Teklan Area - " + status);
        }
        return status;
    }

    protected Item getWeapon(int slot) {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

        if (equipment == null) {
            return null;
        }

        return equipment.getItem(slot);
    }

    public boolean shouldRestock() {
        boolean needToRestock = InventoryUtil.emptySlots() == 0;

        if (!hasAnyRingOfDueling()) {
            needToRestock = true;
        }

        if (config.shouldMine() && !InventoryUtil.hasItem(ItemID.RUNE_PICKAXE) && !EquipmentUtil.hasItem(ItemID.RUNE_PICKAXE)) {
            needToRestock = true;
        }

        if (config.trainMelee() && !InventoryUtil.hasItem(config.mainId()) && !EquipmentUtil.hasItem(config.mainId())) {
            needToRestock = true;
        }

        if(config.useSpec() && !InventoryUtil.hasItem(config.specId()) && !EquipmentUtil.hasItem(config.specId())) {
            needToRestock = true;
        }

        if (config.trainRange() && !InventoryUtil.hasItem(config.rangeAmmoId())) {
            needToRestock = true;
        }

        if (config.debugMode()) {
            EthanApiPlugin.sendClientMessage("Should restock: " + needToRestock);
        }
        return needToRestock;
    }

    private void equipGear() {
        Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (mainWeapon == null) {
            logError("equipGear called but no weapon equipped!");
            timeout = tickDelay();
            return;
        }

        int specPercent = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT);

        if (specPercent >= config.specTreshhold() * 10 && mainWeapon.getId() != config.specId()) {
            InventoryInteraction.useItem(config.specId(), "Wield");
            timeout = 2 + tickDelay(); // Give extra time for weapon switch
            return;
        }

        if (specPercent < config.specTreshhold() * 10 && mainWeapon.getId() == config.specId()) {
            InventoryInteraction.useItem(config.mainId(), "Wield");
            timeout = 2 + tickDelay(); // Give extra time for weapon switch
            return;
        }

        timeout = tickDelay();
    }

    private void useSpec() {
        Item mainWeapon = getWeapon(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (mainWeapon == null) {
            logError("useSpec called but no weapon equipped!");
            timeout = tickDelay();
            return;
        }

        if (mainWeapon.getId() != config.specId()) {
            logError("useSpec called but spec weapon not equipped! Current: " + mainWeapon.getId() + " | Expected: " + config.specId());
            timeout = tickDelay();
            return;
        }

        boolean specEnabled = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 1;
        int specPercent = client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10;

        if (specEnabled) {
            timeout = tickDelay();
            return;
        }
        int specId = 38862887;
        Widget specWidget = client.getWidget(specId);

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(specWidget, "Use Special Attack");
        timeout = tickDelay();
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE ||
                chatMessage.getType() == ChatMessageType.SPAM) {
            String message = chatMessage.getMessage();

            if (message.equalsIgnoreCase(GEMSTONE_CRAB_MINE_SUCCESS_MESSAGE)) {
                if (!isMiningBeforeCooldown()) {
                    miningAttempts++;
                    minedCount++;
                    setLastMiningAttempt();
                }
            } else if (message.equalsIgnoreCase(GEMSTONE_CRAB_MINE_FAIL_MESSAGE)) {
                if (!isMiningBeforeCooldown()) {
                    miningAttempts++;
                    miningFailedCount++;
                    setLastMiningAttempt();
                }
            } else if (message.contains(GEMSTONE_CRAB_GEM_MINE_MESSAGE)) {
                gemsMined++;
            } else if (message.contains(GEMSTONE_CRAB_TOP16_MESSAGE)) {
                isTop16Damager = true;
                timeout = tickDelay();
            } else if (message.contains(GEMSTONE_CRAB_MINED_ALL_MESSAGE)) {
                isTop16Damager = false;
                timeout = tickDelay();
            }
             else if (message.equalsIgnoreCase(GEMSTONE_CRAB_DEATH_MESSAGE)) {
                timeout = tickDelay();
            }
        }
    }

    // ==================== ACTION METHODS WITH LOGGING ====================

    // Also update attackCrab to be more defensive:
    private void attackCrab() {
        if (currentNPC == null) {
            logError("attackCrab called but currentNPC is null!");
            return;
        }

        if (currentNPC.isDead() || currentNPC.getHealthRatio() == 0) {
            currentNPC = null;
            return;
        }

        if (player.getInteracting() == currentNPC) {
            timeout = tickDelay();
            return;
        }

        NPCInteraction.interact(currentNPC, "Attack");
        timeout = tickDelay();
    }

    private void moveToCrab() {
        WorldPoint northWestCrabSpawn = new WorldPoint(1275, 3170, 0);
        WorldPoint playerPos = EthanApiPlugin.playerPosition();

        int distance = playerPos.distanceTo(northWestCrabSpawn);

        if (distance < 5) {
            timeout = tickDelay();
            return;
        } else {
            Walker.walkTo(northWestCrabSpawn);
        }

        if (northWestCrabSpawn.isInScene(client)) {
            if (!Walker.isWalking()) {
                logDebug("Starting PathWalker to crab spawn");
                if (Walker.walkTo(northWestCrabSpawn)) {
                    // Successfully started PathWalker
                    Walker.handleWalking(client);
                    return;
                } else {
                    logDebug("PathWalker failed, using direct click");
                }
            } else {
                logDebug("Else loop - PathWalker already active");
                // Continue existing walk
                boolean stillWalking = Walker.handleWalking(client);
                logDebug("still walking with PathWalker..."+stillWalking);
                if (stillWalking) {
                    return;
                }
            }
        }
        timeout = 2 + tickDelay();
    }


    private void teleportFerox() {
        Optional<Widget> row = Inventory.search().nameContains("Ring of dueling(").first();
        if (row.isPresent()) {
            InventoryInteraction.useItem(row.get(), "Rub");
            timeout = tickDelay();
            MousePackets.queueClickPacket();
            WidgetPackets.queueResumePause(14352385, 3);
        } else {
            logError("No Ring of Dueling found!");
        }
        timeout = 2 + tickDelay();
    }

    public boolean isPluginStarted() { // Create a public accessor for the state
        return started;
    }

    private void teleportCivitas() {
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetActionPacket(1, 14286891, -1, -1);
        timeout = 3 + tickDelay();
    }

// Replace your moveToBird() method with this fixed version:

    private void moveToBird() {

        Optional<NPC> renu = NPCs.search().withId(13350).nearestToPlayer();
        if (renu.isPresent()) {
            NPCInteraction.interact(renu.get(), "Last-destination");
            //NPCInteraction.interact(renu.get(), "Travel");
            timeout = 3 + tickDelay(); // Wait for dialog to open
        } else {
            WorldPoint renuLocation = new WorldPoint(1697, 3140, 0);
            WorldPoint playerPos = EthanApiPlugin.playerPosition();

            int distance = playerPos.distanceTo(renuLocation);

            if (distance < 5) {
                logDebug("Already at renu location");
                timeout = tickDelay();
                return;
            } else {
                Walker.walkTo(renuLocation);
            }
            // Try PathWalker first if destination is in scene
            if (renuLocation.isInScene(client)) {
                if (!Walker.isWalking()) {
                    logDebug("Starting PathWalker to crab spawn");
                    if (Walker.walkTo(renuLocation)) {
                        // Successfully started PathWalker
                        Walker.handleWalking(client);
                        return;
                    } else {
                        logDebug("PathWalker failed, using direct click");
                    }
                } else {
                    logDebug("Else loop - PathWalker already active");
                    // Continue existing walk
                    boolean stillWalking = Walker.handleWalking(client);
                    logDebug("still walking with PathWalker..."+stillWalking);
                    if (stillWalking) {
                        return;
                    }
                }
            }
            timeout = 2 + tickDelay();
        }

        // First check if the travel dialog is already open
        Optional<Widget> talTeklanOption = Widgets.search()
                .withAction("Tal Teklan")
                .first();
        if (talTeklanOption.isPresent()) {
            logDebug("Selecting Tal Teklan from travel dialog");
            /* MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(1, 57278476, -1, 11); */
            timeout = 5 + tickDelay();
        }
    }

    // Banking
    protected void interactWithBank() {
        Optional<TileObject> bank = TileObjects.search().withName("Bank chest").withAction("Use").nearestToPlayer();
        if (bank.isPresent()) {
            TileObjectInteraction.interact(bank.get(), "Use");
        } else {
            walkFeroxBank();
        }
    }

    private void walkFeroxBank() {
        WorldPoint feroxBank = new WorldPoint(3130, 3631, 0);
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(feroxBank);
        timeout = tickDelay();
    }
    // FIX 3: Update withdrawItems to look for ANY ring of dueling
    protected void withdrawItems() {

        // Check for required items
        Optional<Widget> lawRune = Bank.search().withId(ItemID.LAWRUNE).first();
        Optional<Widget> earthRune = Bank.search().withId(ItemID.EARTHRUNE).first();
        Optional<Widget> fireRune = Bank.search().withId(ItemID.FIRERUNE).first();
        Optional<Widget> runePick = Bank.search().withId(ItemID.RUNE_PICKAXE).first();

        // Search for ANY ring of dueling in bank (not just the (8) version)
        Optional<Widget> duelRing = Bank.search().nameContains("Ring of dueling(").first();
        Optional<Widget> specWeapon = Bank.search().withId(config.specId()).first();

        if (lawRune.isEmpty() || earthRune.isEmpty() || fireRune.isEmpty() ||
                (runePick.isEmpty() && config.shouldMine()) || duelRing.isEmpty()) {
            logError("Missing required items in bank - stopping");
            resetPlugin();
            return;
        }

        // Withdraw items one by one
        if (!InventoryUtil.hasItem(ItemID.LAWRUNE)) {
            BankInteraction.withdrawX(lawRune.get(), 2);
            timeout = 2+tickDelay();
            return;
        }
        if (!InventoryUtil.hasItem(ItemID.EARTHRUNE)) {
            BankInteraction.withdrawX(earthRune.get(), 1);
            timeout = 2 + tickDelay();
            return;
        }
        if (!InventoryUtil.hasItem(ItemID.FIRERUNE)) {
            BankInteraction.withdrawX(fireRune.get(), 1);
            timeout = 2 + tickDelay();
            return;
        }
        if (config.shouldMine() && !InventoryUtil.hasItem(ItemID.RUNE_PICKAXE)) {
            BankInteraction.useItem(runePick.get(), "Withdraw-1");
            timeout = 3 + tickDelay();
            return;
        }
        // Check if we have ANY ring of dueling in inventory
        if (!hasAnyRingOfDueling()) {
            BankInteraction.useItem(duelRing.get(), "Withdraw-1");
            timeout = 3 + tickDelay();
            return;
        }
        if (config.useSpec() && specWeapon.isPresent() && !InventoryUtil.hasItem(config.specId())) {
            BankInteraction.useItem(specWeapon.get(), "Withdraw-1");
            timeout = 2+tickDelay();
        }
        else if (config.useSpec() && specWeapon.isEmpty() && !InventoryUtil.hasItem(config.specId())) {
            resetPlugin();
            return;
        }

        if (!Widgets.search().withTextContains("Enter amount:").empty()) {
            client.runScript(ScriptID.MESSAGE_LAYER_CLOSE, 1, 1, 0);
        }

        timeout = 2 + tickDelay();
        deposited = false;
        client.runScript(29);

    }

    protected void depositItems() {
        BankUtil.depositAll();
        if (!BankUtil.containsExcept(inventorySetup) || Inventory.getEmptySlots() == 28) {
            deposited = true;
        }
        timeout = tickDelay();
    }

    private void enterTunnel() {
        Optional<TileObject> cave = TileObjects.search().withName("Cave").nearestToPlayer();
        if (cave.isPresent()) {
            TileObjectInteraction.interact(cave.get(), "Crawl-through");
            timeout = 30 + tickDelay();
        } else {
            logError("No tunnel found!");
            timeout = tickDelay();
        }
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

}