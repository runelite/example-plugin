package com.loadoutsaver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LoadoutSaverPlugin.CONFIG_GROUP_NAME)
public interface LoadoutSaverConfig extends Config
{

	@ConfigItem(
		keyName = "autosave",
		name = "Enable Autosave",
		description = "Automatically saves loadout list when loadouts are added or deleted."
	)
	default boolean autoSave()
	{
		return true;
	}

	@ConfigItem(
			keyName = LoadoutSaverPlugin.CONFIG_SAVED_LOADOUT_KEY,
			name = "Saved Loadouts",
			description = "Saved loadouts",
			hidden = true
	)
	default String loadouts() {
		return "";
	}
}
