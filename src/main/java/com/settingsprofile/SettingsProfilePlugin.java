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

import com.google.inject.Provides;
import com.settingsprofile.data.Profile;
import com.settingsprofile.data.TrackedVarClientInt;
import com.settingsprofile.data.TrackedVarPlayer;
import com.settingsprofile.data.TrackedVarbit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Settings Profiles",
	description = "Save and restore game settings (volume, brightness, keybinds, XP drops, chat colours) "
		+ "as named profiles, with optional per-account support.",
	tags = {"settings", "profile", "keybinds", "hotkeys", "volume", "brightness", "xpdrops", "chat"}
)
public class SettingsProfilePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SettingsProfileConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ProfileManager profileManager;

	@Override
	protected void startUp()
	{
		log.debug("Settings Profiles started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Settings Profiles stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SettingsProfileConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		// Each action key is a boolean; flip it back off after handling.
		switch (event.getKey())
		{
			case "saveProfile":
				if (Boolean.parseBoolean(event.getNewValue()))
				{
					handleSave();
					configManager.setConfiguration(SettingsProfileConfig.GROUP, "saveProfile", false);
				}
				break;

			case "loadProfile":
				if (Boolean.parseBoolean(event.getNewValue()))
				{
					handleLoad();
					configManager.setConfiguration(SettingsProfileConfig.GROUP, "loadProfile", false);
				}
				break;

			case "deleteProfile":
				if (Boolean.parseBoolean(event.getNewValue()))
				{
					handleDelete();
					configManager.setConfiguration(SettingsProfileConfig.GROUP, "deleteProfile", false);
				}
				break;

			case "listProfiles":
				if (Boolean.parseBoolean(event.getNewValue()))
				{
					handleList();
					configManager.setConfiguration(SettingsProfileConfig.GROUP, "listProfiles", false);
				}
				break;
		}
	}

	// ── Action handlers ───────────────────────────────────────────────────────

	private void handleSave()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			notify("You must be logged in to save a profile.");
			return;
		}

		String name = config.profileName().trim();
		if (name.isEmpty())
		{
			notify("Please enter a profile name first.");
			return;
		}

		String accountName = resolveAccountName();
		Profile profile = captureSettings(name, accountName);
		profileManager.save(profile);
		notify("Settings saved as profile '" + name + "'.");
	}

	private void handleLoad()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			notify("You must be logged in to load a profile.");
			return;
		}

		String name = config.profileName().trim();
		if (name.isEmpty())
		{
			notify("Please enter a profile name first.");
			return;
		}

		Profile profile = profileManager.load(name, resolveAccountName());
		if (profile == null && config.accountSpecific())
		{
			// Fall back to a global profile with the same name.
			profile = profileManager.load(name, null);
		}
		if (profile == null)
		{
			notify("Profile '" + name + "' not found.");
			return;
		}

		restoreSettings(profile);
		notify("Profile '" + name + "' loaded. Open the in-game Options panel once if some settings haven't applied.");
	}

	private void handleDelete()
	{
		String name = config.profileName().trim();
		if (name.isEmpty())
		{
			notify("Please enter a profile name first.");
			return;
		}

		String accountName = config.accountSpecific() && client.getGameState() == GameState.LOGGED_IN
			? client.getLocalPlayer().getName()
			: null;

		boolean deleted = profileManager.delete(name, accountName);
		if (!deleted && accountName != null)
		{
			// Try the global namespace as a fallback.
			deleted = profileManager.delete(name, null);
		}
		notify(deleted ? "Profile '" + name + "' deleted." : "Profile '" + name + "' not found.");
	}

	private void handleList()
	{
		String accountName = config.accountSpecific() && client.getGameState() == GameState.LOGGED_IN
			? client.getLocalPlayer().getName()
			: null;

		List<String> profiles = profileManager.list(accountName);
		if (profiles.isEmpty())
		{
			notify("No profiles found.");
		}
		else
		{
			notify("Profiles: " + String.join(", ", profiles));
		}
	}

	// ── Settings capture ──────────────────────────────────────────────────────

	/**
	 * Reads the current values of every tracked setting from the game client.
	 */
	private Profile captureSettings(String name, String accountName)
	{
		Map<String, Integer> varps = new LinkedHashMap<>();
		Map<String, Integer> varbits = new LinkedHashMap<>();
		Map<String, Integer> varCInts = new LinkedHashMap<>();

		for (TrackedVarPlayer t : TrackedVarPlayer.values())
		{
			varps.put(String.valueOf(t.getId()), client.getVarpValue(t.getId()));
		}

		for (TrackedVarbit t : TrackedVarbit.values())
		{
			varbits.put(String.valueOf(t.getId()), client.getVarbitValue(t.getId()));
		}

		for (TrackedVarClientInt t : TrackedVarClientInt.values())
		{
			varCInts.put(String.valueOf(t.getId()), client.getVarcIntValue(t.getId()));
		}

		long now = System.currentTimeMillis();
		return new Profile(name, accountName, varps, varbits, varCInts, now, now);
	}

	// ── Settings restore ──────────────────────────────────────────────────────

	/**
	 * Writes saved values back into the game client's varps, varbits, and
	 * VarClientInts.
	 *
	 * <p>VarPlayer values are written by mutating the varps array directly (the
	 * same reference the engine reads from). Varbits delegate to
	 * {@code client.setVarbitValue} so the bit packing is handled correctly.
	 * VarClientInts use the dedicated {@code client.setVarcIntValue} setter.
	 */
	private void restoreSettings(Profile profile)
	{
		int[] varps = client.getVarps();

		if (profile.getVarpValues() != null)
		{
			for (Map.Entry<String, Integer> entry : profile.getVarpValues().entrySet())
			{
				try
				{
					int id = Integer.parseInt(entry.getKey());
					if (id >= 0 && id < varps.length)
					{
						varps[id] = entry.getValue();
					}
				}
				catch (NumberFormatException ignored)
				{
					log.debug("Skipping malformed varp key: {}", entry.getKey());
				}
			}
		}

		// Apply varbits after writing raw varps so bit-field writes don't clobber
		// VarPlayer values we just set (the backing varps for each varbit are
		// different from the VarPlayers we track above).
		if (profile.getVarbitValues() != null)
		{
			for (Map.Entry<String, Integer> entry : profile.getVarbitValues().entrySet())
			{
				try
				{
					int id = Integer.parseInt(entry.getKey());
					client.setVarbitValue(varps, id, entry.getValue());
				}
				catch (NumberFormatException ignored)
				{
					log.debug("Skipping malformed varbit key: {}", entry.getKey());
				}
			}
		}

		if (profile.getVarClientIntValues() != null)
		{
			for (Map.Entry<String, Integer> entry : profile.getVarClientIntValues().entrySet())
			{
				try
				{
					int id = Integer.parseInt(entry.getKey());
					client.setVarcIntValue(id, entry.getValue());
				}
				catch (NumberFormatException ignored)
				{
					log.debug("Skipping malformed varCInt key: {}", entry.getKey());
				}
			}
		}
	}

	// ── Utilities ─────────────────────────────────────────────────────────────

	private String resolveAccountName()
	{
		if (config.accountSpecific() && client.getGameState() == GameState.LOGGED_IN)
		{
			return client.getLocalPlayer().getName();
		}
		return null;
	}

	private void notify(String message)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "[Settings Profiles] " + message, null);
		}
		else
		{
			log.info("[Settings Profiles] {}", message);
		}
	}

	@Provides
	SettingsProfileConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SettingsProfileConfig.class);
	}
}
