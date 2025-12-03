package com.example.GemCrabFighter;

import net.runelite.client.config.*;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("GemCrabFighter")
public interface GemCrabFighterConfig extends Config {

    @ConfigItem(keyName = "trainMelee", name = "Train Melee", description = "Whether to train melee or not.", position = 91, section = "damageConfig")
    default boolean trainMelee()
    {
        return true;
    }

    @ConfigItem(keyName = "trainRange", name = "Train Range", description = "Whether to train range or not.", position = 91, section = "damageConfig")
    default boolean trainRange()
    {
        return false;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "tickDelayMin", name = "Game Tick Min", description = "", position = 8, section = "tickConfig")
    default int tickDelayMin()
    {
        return 1;
    }

    @Range(min = 0, max = 10)
    @ConfigItem(keyName = "tickDelayMax", name = "Game Tick Max", description = "", position = 9, section = "tickConfig")
    default int tickDelayMax()
    {
        return 3;
    }

    @ConfigItem(keyName = "mainId", name = "Main Weapon ID", description = "Main weapon ID to use.", position = 92, section = "damageConfig")
    default int mainId()
    {
        return 4587;
    }

    @ConfigItem(keyName = "shouldMine", name = "Should Mine Crab", description = "Enable to mine gems from the Gemstone Crab shells.", position = 95)
    default boolean shouldMine()
    {
        return false;
    }

    @ConfigItem(keyName = "useSpec", name = "Use Spec Weapon", description = "Enable to use Spec Weapon", position = 90, section = "damageConfig")
    default boolean useSpec()
    {
        return true;
    }

    @ConfigItem(keyName = "specId", name = "Spec Weapon ID", description = "Spec weapon ID to use.", position = 91, section = "damageConfig")
    default int specId()
    {
        return 1377;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "specTreshhold", name = "Spec threshhold", description = "Amount of spec % before using spec.", position = 96, section = "damageConfig")
    default int specTreshhold()
    {
        return 100;
    }

    @ConfigItem(keyName = "rangeAmmoId", name = "Ranged Ammo ID", description = "Ranged ammo ID to use.", position = 93, section = "damageConfig")
    default int rangeAmmoId()
    {
        return 9144;
    }

    @ConfigItem(
            keyName = "toggle",
            name = "Toggle",
            description = "",
            position = -2
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(keyName = "debugMode", name = "Debug", description = "Button to show debug message", position = 34)
    default boolean debugMode()
    {
        return true;
    }
}
