/*
 * Copyright (c) 2024, Settings Profiles Plugin
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.settingsprofile;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(SettingsProfileConfig.GROUP)
public interface SettingsProfileConfig extends Config
{
	String GROUP = "settings-profiles";

	// ── Sections ─────────────────────────────────────────────────────────────

	@ConfigSection(
		name = "Profile",
		description = "Profile name and account association",
		position = 0
	)
	String profileSection = "profile";

	@ConfigSection(
		name = "Actions",
		description = "Check a checkbox to perform the action; it resets automatically",
		position = 1
	)
	String actionsSection = "actions";

	// ── Profile section ───────────────────────────────────────────────────────

	@ConfigItem(
		keyName = "profileName",
		name = "Profile Name",
		description = "Name of the profile to save, load, or delete",
		section = "profile",
		position = 0
	)
	default String profileName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "accountSpecific",
		name = "Account-Specific",
		description = "<html>When enabled, the profile is saved and loaded under the current<br>"
			+ "player name so different accounts can have separate profiles.</html>",
		section = "profile",
		position = 1
	)
	default boolean accountSpecific()
	{
		return false;
	}

	// ── Actions section ───────────────────────────────────────────────────────

	@ConfigItem(
		keyName = "saveProfile",
		name = "Save Current Settings",
		description = "Check to capture the current game settings and save them as the named profile.",
		section = "actions",
		position = 0
	)
	default boolean saveProfile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "loadProfile",
		name = "Load Profile",
		description = "Check to restore the named profile. Some settings (audio, brightness) may need "
			+ "you to open the in-game Options panel once to take full effect.",
		section = "actions",
		position = 1
	)
	default boolean loadProfile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "deleteProfile",
		name = "Delete Profile",
		description = "Check to permanently delete the named profile from disk.",
		section = "actions",
		position = 2
	)
	default boolean deleteProfile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "listProfiles",
		name = "List Profiles in Chat",
		description = "Check to print available profiles to the game chat.",
		section = "actions",
		position = 3
	)
	default boolean listProfiles()
	{
		return false;
	}
}
