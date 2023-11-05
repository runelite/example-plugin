package com.loadoutsaver;

import com.google.inject.Provides;
import com.loadoutsaver.implementations.LoadoutImpl;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
	name = "Loadout Saver"
)
public class LoadoutSaverPlugin extends Plugin
{
	public static final String CONFIG_GROUP_NAME = "LoadoutSaver";
	public static final String CONFIG_SAVED_LOADOUT_KEY = "savedloadouts";

	@Inject
	private Client client;

	@Inject
	private LoadoutSaverConfig config;

	@Inject
	private ConfigManager configManager;

	private LoadoutManager loadoutManager = new LoadoutManager();

	@Override
	protected void startUp() throws Exception
	{
		// Load from save file.
		System.out.println("Load from save file.");
		loadoutManager = new LoadoutManager(config);
		System.out.println("Load complete; loaded " + loadoutManager.size() + " loadouts.");
	}

	@Override
	protected void shutDown() throws Exception
	{
		// Save to save file.
		System.out.println("Saving " + loadoutManager.size() + " loadouts.");
		loadoutManager.save(configManager);
		System.out.println("Successfully saved to configuration.");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Autosave was set to " + config.autoSave(), null);
			if (loadoutManager.size() == 0) {
				System.out.println("Adding test loadout");
				try {
					loadoutManager.AddLoadout(new LoadoutImpl("Test loadout", client), configManager, config);
					System.out.println("Parsed test loadout.");
				}
				catch (IllegalArgumentException e) {
					System.out.println("Bad client state, probably.");
				}
			}
		}
	}

	@Provides
	LoadoutSaverConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LoadoutSaverConfig.class);
	}
}
