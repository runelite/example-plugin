package com.example.GemCrabFighter;

import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

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

    @ConfigItem(keyName="trainMagic", name="Train Magic", description="Whether to train magic or not.", position=91, section="damageConfig")
    default boolean trainMagic()
    {
        return false;
    }

    @ConfigItem(keyName = "mainId", name = "Main Weapon ID", description = "Main weapon ID to use.", position = 92, section = "damageConfig")
    default int mainId()
    {
        return 22978;
    }

    @ConfigItem(keyName = "specId", name = "Spec Weapon ID", description = "Spec weapon ID to use.", position = 91, section = "damageConfig")
    default int specId()
    {
        return 13576;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "specTreshhold", name = "Spec threshhold", description = "Amount of spec % before using spec.", position = 96, section = "damageConfig")
    default int specTreshhold()
    {
        return 70;
    }

    @ConfigItem(keyName = "startButton", name = "Start/Stop", description = "Button to start or stop the plugin", position = 33)
    default boolean startButton()
    {
        return false;
    }

    @ConfigItem(keyName = "debugMode", name = "Debug", description = "Button to show debug message", position = 34)
    default boolean debugMode()
    {
        return true;
    }
}
