package ca.plugins.yamablindfold;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(YamaBlindfoldConfig.CONFIG_GROUP)
public interface YamaBlindfoldConfig extends Config
{
	String CONFIG_GROUP = "yamablindfold";

	String CONFIG_KEY_HIDE_SCENERY = "hideScenery";

	@ConfigItem(
		name = "Hide Scenery",
		description = "Hides various scenery." +
			"<br>e.g. trees, altars, dirt piles etc.",
		position = 0,
		keyName = CONFIG_KEY_HIDE_SCENERY
	)
	default boolean hideScenery()
	{
		return true;
	}

	@ConfigItem(
		name = "Hide Terrain",
		description = "Hides surrounding terrain." +
			"<br>e.g. lava pools" +
			"<br>No effect if a GPU plugin is not enabled.",
		position = 1,
		keyName = "hideTerrain"
	)
	default boolean hideTerrain()
	{
		return false;
	}

	@ConfigItem(
		name = "Exclude Stepping Stones",
		description = "Excludes stepping stones terrain from being hidden." +
			"<br>No effect if 'Hide Terrain' is disabled.",
		position = 2,
		keyName = "showTerrainStepStones"
	)
	default boolean showTerrainStepStones()
	{
		return false;
	}

	@ConfigItem(
		name = "Hide Fade-out Transitions",
		description = "Hide the fade-out transitions between phases." +
			"<br>i.e. teleporting to Judge of Yama.",
		position = 3,
		keyName = "hideFadeTransition"
	)
	default boolean hideFadeTransition()
	{
		return false;
	}
}
