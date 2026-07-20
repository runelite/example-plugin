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

import com.google.gson.Gson;
import com.settingsprofile.data.Profile;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Handles reading and writing profile JSON files under
 * {@code .runelite/settings-profiles/}.
 * <p>
 * Global profiles live in {@code settings-profiles/global/}.
 * Account-specific profiles live in {@code settings-profiles/<playerName>/}.
 */
@Slf4j
@Singleton
public class ProfileManager
{
	private static final File PROFILES_DIR = new File(RuneLite.RUNELITE_DIR, "settings-profiles");

	@Inject
	private Gson gson;

	/**
	 * Persists {@code profile} to disk, creating directories as needed.
	 */
	public void save(Profile profile)
	{
		File file = getProfileFile(profile.getName(), profile.getAccountName());
		file.getParentFile().mkdirs();
		try (FileWriter writer = new FileWriter(file))
		{
			gson.toJson(profile, writer);
			log.debug("Saved profile '{}' to {}", profile.getName(), file);
		}
		catch (IOException e)
		{
			log.error("Failed to save profile '{}': {}", profile.getName(), e.getMessage());
		}
	}

	/**
	 * Loads the named profile for the given account (or the global namespace when
	 * {@code accountName} is {@code null}).
	 *
	 * @return the loaded profile, or {@code null} if not found / parse error
	 */
	public Profile load(String name, String accountName)
	{
		File file = getProfileFile(name, accountName);
		if (!file.exists())
		{
			return null;
		}
		try (FileReader reader = new FileReader(file))
		{
			Profile profile = gson.fromJson(reader, Profile.class);
			log.debug("Loaded profile '{}' from {}", name, file);
			return profile;
		}
		catch (IOException e)
		{
			log.error("Failed to load profile '{}': {}", name, e.getMessage());
			return null;
		}
	}

	/**
	 * Deletes the named profile.
	 *
	 * @return {@code true} if the file existed and was removed
	 */
	public boolean delete(String name, String accountName)
	{
		File file = getProfileFile(name, accountName);
		if (file.exists() && file.delete())
		{
			log.debug("Deleted profile '{}' at {}", name, file);
			return true;
		}
		return false;
	}

	/**
	 * Returns profile names available for the given account.
	 * <p>
	 * When {@code accountName} is non-null, lists both account-specific profiles
	 * and global profiles (global profiles appear with a " (global)" suffix).
	 * When {@code accountName} is {@code null}, lists only global profiles.
	 */
	public List<String> list(String accountName)
	{
		List<String> names = new ArrayList<>();

		if (accountName != null)
		{
			File accountDir = new File(PROFILES_DIR, sanitize(accountName));
			appendProfileNames(accountDir, names, "");
		}

		File globalDir = new File(PROFILES_DIR, "global");
		String suffix = accountName != null ? " (global)" : "";
		appendProfileNames(globalDir, names, suffix);

		Collections.sort(names);
		return names;
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private File getProfileFile(String name, String accountName)
	{
		File dir = accountName != null
			? new File(PROFILES_DIR, sanitize(accountName))
			: new File(PROFILES_DIR, "global");
		return new File(dir, sanitize(name) + ".json");
	}

	private void appendProfileNames(File dir, List<String> out, String suffix)
	{
		if (!dir.isDirectory())
		{
			return;
		}
		File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
		if (files == null)
		{
			return;
		}
		for (File f : files)
		{
			String n = f.getName();
			out.add(n.substring(0, n.length() - 5) + suffix); // strip ".json"
		}
	}

	/**
	 * Strips characters that are unsafe in file-system paths, keeping letters,
	 * digits, spaces, hyphens, underscores and dots.
	 */
	static String sanitize(String name)
	{
		return name.replaceAll("[^\\w\\s.\\-]", "_").trim();
	}
}
