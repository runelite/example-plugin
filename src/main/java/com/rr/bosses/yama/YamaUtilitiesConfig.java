package com.rr.bosses.yama;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("yamautilities")
public interface YamaUtilitiesConfig extends Config
{
	@ConfigSection(
			name = "Boss Damage Contribution",
			description = "Configure settings for the boss damage contribution settings",
			position = 0
	)
	String BOSS_DAMAGE_SETTINGS = "bossDamageSettings";

	@ConfigItem(
			keyName = "printDamageToChat",
			name = "Print Damage To Chat",
			description = "Print personal damage and percentage of total damage to the chat",
			section = BOSS_DAMAGE_SETTINGS,
			position = 0
	)
	default boolean printDamageToChat()
	{
		return true;
	}

	@ConfigSection(
			name = "Duo Name Autofill",
			description = "Configure duo's name auto fill settings",
			position = 1
	)
	String AUTOFILL_SETTINGS = "autofillSettings";

	@ConfigItem(
			keyName = "autofillKeybind",
			name = "Autofill Keybind",
			description = "DOES NOT SUPPORT MODIFIERS",
			section = AUTOFILL_SETTINGS,
			position = 0
	)
	default Keybind autofillKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "enterKeybind",
			name = "Enter Keybind",
			description = "DOES NOT SUPPORT MODIFIERS",
			section = AUTOFILL_SETTINGS,
			position = 1
	)
	default Keybind enterKeybind()
	{
		return Keybind.NOT_SET;
	}

	@ConfigSection(
			name = "Glyph Protection Indicator",
			description = "Toggle whether to show glyph protection indicator and configure its colors.",
			position = 2
	)
	String GLYPH_PROTECTION_SETTINGS = "glyphProtectionSettings";

	@ConfigItem(
			keyName = "showGlyphProtectionIndicator",
			name = "Show Glyph Protection Indicator",
			description = "Toggle whether to highlight the player when protected by a glyph.",
			section = GLYPH_PROTECTION_SETTINGS,
			position = 0
	)
	default boolean showGlyphProtectionIndicator()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightBorderWidth",
			name = "Highlight Border Width",
			description = "Configure the thickness of the player highlight, 1-10",
			section = GLYPH_PROTECTION_SETTINGS,
			position = 1
	)
	@Range(
			min = 1,
			max = 10
	)
	default int highlightBorderWidth()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "fireGlyphColor",
			name = "Fire Glyph Color",
			description = "Configures the color of the player highlight when protected by a Fire Glyph.",
			section = GLYPH_PROTECTION_SETTINGS,
			position = 2
	)
	default Color fireGlyphColor()
	{
		return Color.RED;
	}

	@ConfigItem(
			keyName = "shadowGlyphColor",
			name = "Shadow Glyph Color",
			description = "Configures the color of the player highlight when protected by a Shadow Glyph.",
			section = GLYPH_PROTECTION_SETTINGS,
			position = 3
	)
	default Color shadowGlyphColor()
	{
		return new Color(73,2,137); //dark purple
	}

	@ConfigItem(
			keyName = "bothGlyphsColor",
			name = "Both Glyphs Color",
			description = "Configures the color of the player highlight when protected by a both Glyphs.",
			section = GLYPH_PROTECTION_SETTINGS,
			position = 4
	)
	default Color bothGlyphsColor()
	{
		return Color.MAGENTA;
	}
}
