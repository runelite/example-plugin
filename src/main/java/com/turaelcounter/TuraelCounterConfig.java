package com.turaelcounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.nio.charset.StandardCharsets;

@ConfigGroup("turaelcounter")
public interface TuraelCounterConfig extends Config
{
	@ConfigSection(
			name = "Desired Slayer Tasks",
			description = "Select desired tasks",
			position = 0
	)
	String taskSection = "tasks";

	@ConfigItem(
			keyName = "chooseSmokeDevils",
			name = "Smoke Devils",
			description = "Choose if Smoke devils are desired",
			section = taskSection
	)
	default boolean isSmokeDevilDesired()
	{
		return false;
	}
	@ConfigItem(
			keyName = "chooseTzKal-Zuk",
			name = "TzKal-Zuk",
			description = "Choose if TzKal-Zuk is desired",
			section = taskSection
	)
	default boolean isTzKalZukDesired()
	{
		return true;
	}

	@ConfigItem(
			keyName = "chooseAbyssalDemons",
			name = "Abyssal Demons",
			description = "Choose if Abyssal demons are desired",
			section = taskSection
	)
	default boolean isAbyssalDemonDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "chooseHellhounds",
			name = "Hellhounds",
			description = "Choose if Hellhounds are desired",
			section = taskSection
	)
	default boolean isHellhoundDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "chooseGargoyles",
			name = "Gargoyles",
			description = "Choose if Gargoyles are desired",
			section = taskSection
	)
	default boolean isGargoyleDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "chooseLizardmen",
			name = "Lizardmen",
			description = "Choose if Lizardmen are desired",
			section = taskSection
	)
	default boolean isLizardmenDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "chooseRevenants",
			name = "Revenants",
			description = "Choose if Revenants are desired",
			section = taskSection
	)
	default boolean isRevenantDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "chooseHydras",
			name = "Hydras",
			description = "Choose if Hydras are desired",
			section = taskSection
	)
	default boolean isHydraDesired()
	{
		return false;
	}

	@ConfigItem(
			keyName = "streakReset",
			name = "",
			description = "",
			section = taskSection,
			hidden = true
	)
	int streakReset();

	@ConfigItem(
			keyName = "streakReset",
			name = "",
			description = "",
			section = taskSection
	)
	void streakReset(int streakReset);

}
