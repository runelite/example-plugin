package com.RuneLingual;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RuneLingualConfig.GROUP)
public interface RuneLingualConfig extends Config
{
	String GROUP = "lingualConfig";
	@ConfigSection(
			name = "Live translating",
			description = "Player chat translation options",
			position = 1,
			closedByDefault = false
	)
	String SECTION_CHAT_SETTINGS = "chatSettings";

	@ConfigItem(
		name = "Service API Key",
		description = "Your API key for the chosen translating service",
		section = "chatSettings",
		keyName = "APIKey",
		position = 1
	)
	String APIKey();

	@ConfigItem(
			name = "Translating service",
			description = "Select the translating service you'd like to use",
			section = "chatSettings",
			keyName = "translatingService",
			position = 2
	)
	default TranslatingServiceSelectableList getService() {return TranslatingServiceSelectableList.GOOGLE_TRANSLATE;}

	default String getAPIKey() {return APIKey();}
	@ConfigItem(
			name = "Enable API translating",
			description = "Mostly for player messages",
			section = "chatSettings",
			keyName = "enableAPI",
			position = 2
	)
	default boolean allowAPI() {return false;}

	@ConfigSection(
		name = "General translation settings",
		description = "General translation settings",
		position = 2,
		closedByDefault = false
	)
	String SECTION_GENERAL_SETTINGS = "generalSettings";

	@ConfigItem(
		name = "Target language",
		description = "Select the language to be translated to",
		section = "generalSettings",
		keyName = "targetLang",
		position = 1
	)
	default LangCodeSelectableList presetLang() {return LangCodeSelectableList.PORTUGUÊS_BRASILEIRO;}

	@ConfigItem(
			name = "Translate public chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translatePublic",
			position = 2
	)
	default boolean getAllowPublic() {return false;}
	@ConfigItem(
			name = "Translate friends chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateFriends",
			position = 3
	)
	default boolean getAllowFriends() {return false;}
	@ConfigItem(
			name = "Translate clan chat",
			description = "Requires a valid API key",
			section = "generalSettings",
			keyName = "translateClan",
			position = 4
	)
	default boolean getAllowClan() {return false;}
	@ConfigItem(
			name = "Translate own chat messages",
			description = "With this other players should recieve your messages in english",
			section = "generalSettings",
			keyName = "allowLocalPlayerTranslate",
			position = 5
	)
	default boolean getAllowLocal() {return false;}

	@ConfigItem(
			name = "Translate game messages",
			description = "Translate all game messages (boss kc, item charges...)",
			section = "generalSettings",
			keyName = "allowGameTranslate",
			position = 6
	)
	default boolean getAllowGame() {return true;}
	@ConfigItem(
			name = "Translate overhead text",
			description = "Translate overhead messages",
			section = "generalSettings",
			keyName = "allowOverHeadTranslate",
			position = 7
	)
	default boolean getAllowOverHead() {return false;}
	@ConfigItem(
			name = "Translate item names",
			description = "Change item names",
			section = "generalSettings",
			keyName = "allowItemTranslate",
			position = 8
	)
	default boolean getAllowItems() {return true;}
	@ConfigItem(
			name = "Change NPC names",
			description = "Translate NPC names",
			section = "generalSettings",
			keyName = "allowNameTranslate",
			position = 9
	)
	default boolean getAllowName() {return true;}

	@ConfigSection(
			name = "Contribution settings",
			description = "Options regarding crowdsourcing",
			position = 3,
			closedByDefault = false
	)
	String CONTRIBUTION = "contributionSettings";
	@ConfigItem(
			name = "Contribute with new transcriptions",
			description = "Send found untranslated text to our public repository",
			section = "contributionSettings",
			keyName = "allowLocalPlayerTranslate",
			position = 1
	)
	default boolean allowContributions() {return false;}

	@ConfigItem(
			name = "Send contributions when",
			description = "Set when to send new contibutions to the public repository",
			section = "contributionSettings",
			keyName = "contributePeriod",
			position = 2
	)
	default SendUpdates presetContribute() {return SendUpdates.ANYTIME;}

}
