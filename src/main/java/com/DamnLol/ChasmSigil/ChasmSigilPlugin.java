package com.DamnLol.ChasmSigil;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Chasm Sigils",
        description = "Information on current Sigil in the Chasm of Fire.",
        tags = {"chasm", "chasm of fire", "demon", "sigil", "contract", "pvm", "slayer"}
)
public class ChasmSigilPlugin extends Plugin
{
    private static final String CONFIG_GROUP = "chasmoffiresigil";
    private static final String KEY_KILLS = "killCount";
    private static final String KEY_SIGIL = "activeSigilName";

    private static final int[] CHASM_REGIONS = {5789, 5790, 5277, 5278};

    private static final int[] DEMON_IDS = {
            7656, 7657, 7664,
            NpcID.GREATER_DEMON, 2030, 2031, 2032,
            NpcID.BLACK_DEMON, 240, 5874, 5875, 5876, 5877,
    };

    static final int GRAPHIC_RED = 6400;
    static final int GRAPHIC_YELLOW = 6401;
    static final int GRAPHIC_GREEN = 6402;
    static final int GRAPHIC_PURPLE = 6403;
    static final int GRAPHIC_BLUE = 6404;

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private ChasmSigilOverlay overlay;
    @Inject private ConfigManager configManager;

    @Getter private Sigil activeSigil = Sigil.UNKNOWN;
    @Getter private boolean inChasm = false;
    @Getter private int killCount = 0;

    private final Set<Integer> damagedByPlayer = new HashSet<>();
    private boolean stateLoaded = false;

    @Provides
    ChasmSigilConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ChasmSigilConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        stateLoaded = false;
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            loadSavedState();
            stateLoaded = true;
        }
    }

    @Override
    protected void shutDown()
    {
        saveState();
        overlayManager.remove(overlay);
        activeSigil = Sigil.UNKNOWN;
        inChasm = false;
        stateLoaded = false;
        damagedByPlayer.clear();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals(CONFIG_GROUP)) return;
        if (!event.getKey().equals("resetSize")) return;
        overlay.resetSize();
        configManager.setConfiguration(CONFIG_GROUP, "resetSize", false);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        //keep previous sigil/kc data
        GameState state = event.getGameState();
        if (state == GameState.LOGGED_IN && !stateLoaded)
        {
            loadSavedState();
            stateLoaded = true;
        }
        else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING)
        {
            saveState();
            stateLoaded = false;
            inChasm = false;
            activeSigil = Sigil.UNKNOWN;
            damagedByPlayer.clear();
        }
        else if (state == GameState.LOADING)
        {
            inChasm = false;
            damagedByPlayer.clear();
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (!inChasm) return;
        if (!(event.getActor() instanceof NPC)) return;
        NPC npc = (NPC) event.getActor();
        if (isNotDemon(npc)) return;
        if (!event.getHitsplat().isMine()) return;
        damagedByPlayer.add(npc.getIndex());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        if (!inChasm) return;
        NPC npc = event.getNpc();
        if (isNotDemon(npc)) return;
        if (!npc.isDead()) return;
        if (!damagedByPlayer.remove(npc.getIndex())) return;
        killCount = Math.min(killCount + 1, 999); //counter max
        saveState();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        inChasm = checkInChasm();

        if (!inChasm)
            return;

        Sigil newSigil = scanDemonsForSigil();
        if (newSigil != Sigil.UNKNOWN)
        {
            if (newSigil != activeSigil)
            {
                killCount = 0;
                damagedByPlayer.clear();
                saveState();
            }
            activeSigil = newSigil;
        }
    }

    private void saveState()
    {
        if (!stateLoaded) return;
        configManager.setRSProfileConfiguration(CONFIG_GROUP, KEY_KILLS, killCount);
        configManager.setRSProfileConfiguration(CONFIG_GROUP, KEY_SIGIL, activeSigil.name());
    }

    private void loadSavedState()
    {
        String sigilName = configManager.getRSProfileConfiguration(CONFIG_GROUP, KEY_SIGIL);
        String countStr = configManager.getRSProfileConfiguration(CONFIG_GROUP, KEY_KILLS);

        activeSigil = Sigil.UNKNOWN;
        killCount = 0;

        if (sigilName != null)
        {
            try
            {
                Sigil saved = Sigil.valueOf(sigilName);
                if (saved != Sigil.UNKNOWN)
                {
                    activeSigil = saved;
                }
            }
            catch (IllegalArgumentException ignored) {}
        }

        if (countStr != null)
        {
            try { killCount = Integer.parseInt(countStr); }
            catch (NumberFormatException ignored) {}
        }
    }

    private boolean checkInChasm()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        int region = client.getLocalPlayer().getWorldLocation().getRegionID();
        for (int r : CHASM_REGIONS)
            if (r == region) return true;
        return false;
    }

    private Sigil scanDemonsForSigil()
    {
        for (NPC npc : client.getNpcs())
        {
            if (isNotDemon(npc)) continue;
            Sigil found = sigilFromOverhead(npc);
            if (found != Sigil.UNKNOWN) return found;
        }
        return Sigil.UNKNOWN;
    }

    private Sigil sigilFromOverhead(NPC npc)
    {
        int[] archiveIds = npc.getOverheadArchiveIds();
        short[] spriteIds = npc.getOverheadSpriteIds();
        if (archiveIds == null || spriteIds == null) return Sigil.UNKNOWN;

        for (int i = 0; i < archiveIds.length; i++)
        {
            if (archiveIds[i] == -1) continue;
            if (archiveIds[i] == GRAPHIC_RED) return Sigil.RED;
            if (archiveIds[i] == GRAPHIC_YELLOW) return Sigil.YELLOW;
            if (archiveIds[i] == GRAPHIC_GREEN) return Sigil.GREEN;
            if (archiveIds[i] == GRAPHIC_PURPLE) return Sigil.PURPLE;
            if (archiveIds[i] == GRAPHIC_BLUE) return Sigil.BLUE;
        }
        return Sigil.UNKNOWN;
    }

    private boolean isNotDemon(NPC npc)
    {
        int id = npc.getId();
        for (int did : DEMON_IDS)
            if (did == id) return false;
        String name = npc.getName();
        return name == null || (
                !name.equals("Lesser demon") &&
                        !name.equals("Greater demon") &&
                        !name.equals("Black demon")
        );
    }
}