package com.loadoutsaver;

import com.loadoutsaver.interfaces.ILoadout;
import net.runelite.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LoadoutManager {
    private final List<ILoadout> loadouts;

    public LoadoutManager() {
        this(new ArrayList<>());
    }

    public LoadoutManager(LoadoutSaverConfig config) {
        this(DataIO.Parse(config.loadouts()));
    }

    public void save(ConfigManager configManager) {
        String serialized = DataIO.FullSerialize(loadouts);
        configManager.setConfiguration(LoadoutSaverPlugin.CONFIG_GROUP_NAME, "savedloadouts", serialized);
    }

    private LoadoutManager(List<ILoadout> loadouts) {
        this.loadouts = loadouts;
    }

    public Stream<ILoadout> GetLoadouts() {
        return loadouts.stream();
    }

    public void AddLoadout(ILoadout loadout, ConfigManager configManager, LoadoutSaverConfig config) {
        this.loadouts.add(loadout);
        if (config.autoSave()) {
            System.out.println("Autosaving...");
            this.save(configManager);
            System.out.println("Autosave complete.");
        }
    }

    public int size() {
        return loadouts.size();
    }
}
