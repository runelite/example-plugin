package com.barracudatrial;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("barracudatrial")
public interface BarracudaTrialConfig extends Config
{
	@ConfigSection(
		name = "Path Display",
		description = "Settings for the optimal path overlay",
		position = 0
	)
	String pathSection = "pathSection";

	@ConfigSection(
		name = "Lost Supplies",
		description = "Settings for lost supplies highlighting",
		position = 1
	)
	String lostSuppliesSection = "lostSuppliesSection";

	@ConfigSection(
		name = "Lightning Clouds (Storms)",
		description = "Settings for lightning cloud highlighting",
		position = 2
	)
	String cloudSection = "cloudSection";

	@ConfigSection(
		name = "Other Hazards",
		description = "Settings for rocks and other hazards",
		position = 3
	)
	String hazardSection = "hazardSection";

	@ConfigSection(
		name = "Debug",
		description = "Debug and development settings",
		position = 4
	)
	String debugSection = "debugSection";

	@ConfigItem(
		keyName = "showOptimalPath",
		name = "Show Optimal Path",
		description = "Display the optimal path to collect all lost supplies",
		section = pathSection,
		position = 0
	)
	default boolean showOptimalPath()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pathLookahead",
		name = "Path Lookahead",
		description = "Number of waypoints to calculate ahead. Lower values improve performance and reduce visual clutter.",
		section = pathSection,
		position = 2
	)
	@Range(min = 1, max = 10)
	default int pathLookahead()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "pathColor",
		name = "Path Color",
		description = "Color of the optimal path line",
		section = pathSection,
		position = 3
	)
	@Alpha
	default Color pathColor()
	{
		return new Color(0, 255, 0, 180);
	}

	@ConfigItem(
		keyName = "tracerColor",
		name = "Tracer Color",
		description = "Color of the animated tracer pulse",
		section = pathSection,
		position = 4
	)
	@Alpha
	default Color tracerColor()
	{
		return new Color(255, 255, 0, 255);
	}

	@ConfigItem(
		keyName = "pathWidth",
		name = "Path Width",
		description = "Width of the path line",
		section = pathSection,
		position = 5
	)
	@Range(min = 1, max = 10)
	default int pathWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "showPathTracer",
		name = "Animated Tracer",
		description = "Show an animated tracer along the path",
		section = pathSection,
		position = 6
	)
	default boolean showPathTracer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightLostSupplies",
		name = "Highlight Lost Supplies",
		description = "Highlight lost supplies in the trial area",
		section = lostSuppliesSection,
		position = 0
	)
	default boolean highlightLostSupplies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lostSuppliesColor",
		name = "Lost Supplies Color",
		description = "Color for lost supplies highlights",
		section = lostSuppliesSection,
		position = 1
	)
	@Alpha
	default Color lostSuppliesColor()
	{
		return new Color(255, 215, 0, 180);
	}

	@ConfigItem(
		keyName = "showLostSuppliesTile",
		name = "Show Lost Supplies Tile",
		description = "Highlight the tile under lost supplies",
		section = lostSuppliesSection,
		position = 2
	)
	default boolean showLostSuppliesTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightRumLocations",
		name = "Highlight Rum Locations",
		description = "Highlight rum pickup and dropoff locations",
		section = lostSuppliesSection,
		position = 3
	)
	default boolean highlightRumLocations()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rumLocationColor",
		name = "Rum Location Color",
		description = "Color for rum location highlights",
		section = lostSuppliesSection,
		position = 4
	)
	@Alpha
	default Color rumLocationColor()
	{
		return new Color(128, 0, 128, 180);
	}

	@ConfigItem(
		keyName = "highlightClouds",
		name = "Highlight Lightning Clouds",
		description = "Highlight dangerous lightning clouds",
		section = cloudSection,
		position = 0
	)
	default boolean highlightClouds()
	{
		return true;
	}

	@ConfigItem(
		keyName = "cloudColor",
		name = "Cloud Color",
		description = "Color for lightning cloud highlights",
		section = cloudSection,
		position = 1
	)
	@Alpha
	default Color cloudColor()
	{
		return new Color(255, 0, 0, 120);
	}

	@ConfigItem(
		keyName = "cloudDangerRadius",
		name = "Cloud Danger Radius",
		description = "Radius in tiles for the cloud danger area",
		section = cloudSection,
		position = 2
	)
	@Range(min = 0, max = 5)
	default int cloudDangerRadius()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "highlightRocks",
		name = "Highlight Rocks",
		description = "Rocks stop your momentum when hit",
		section = hazardSection,
		position = 0
	)
	default boolean highlightRocks()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rockColor",
		name = "Rock Color",
		description = "Color for rock highlights",
		section = hazardSection,
		position = 1
	)
	@Alpha
	default Color rockColor()
	{
		return new Color(128, 128, 128, 150);
	}

	@ConfigItem(
		keyName = "highlightSpeedBoosts",
		name = "Highlight Speed Boosts",
		description = "Highlight speed boost areas",
		section = hazardSection,
		position = 2
	)
	default boolean highlightSpeedBoosts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "speedBoostColor",
		name = "Speed Boost Color",
		description = "Color for speed boost highlights",
		section = hazardSection,
		position = 3
	)
	@Alpha
	default Color speedBoostColor()
	{
		return new Color(0, 255, 0, 150); // Bright green for speed!
	}

	// Debug Settings
	@ConfigItem(
		keyName = "debugMode",
		name = "Debug Mode",
		description = "Show debug overlays (exclusion zone, all rocks, lap info, supplies counts, performance timings)",
		section = debugSection,
		position = 0
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showIDs",
		name = "Show IDs",
		description = "Show object IDs on all game objects (rocks, lost supplies, clouds, etc.)",
		section = debugSection,
		position = 1
	)
	default boolean showIDs()
	{
		return false;
	}
}
