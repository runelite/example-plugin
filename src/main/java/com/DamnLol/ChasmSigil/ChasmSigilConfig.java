package com.DamnLol.ChasmSigil;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chasmoffiresigil")
public interface ChasmSigilConfig extends Config
{
    @ConfigItem(
            keyName = "killCount",
            name = "Kill Count",
            description = "Current KC",
            hidden = true
    )
    default int killCount() { return 0; }

    @ConfigItem(
            keyName = "activeSigilName",
            name = "Active Sigil",
            description = "Last active sigil",
            hidden = true
    )
    default String activeSigilName() { return Sigil.UNKNOWN.name(); }

    @ConfigItem(
            keyName = "resetSize",
            name = "Reset overlay size",
            description = "reset overlay tests",
            hidden = true //testing resets
    )
    default boolean resetSize() { return false; }
}