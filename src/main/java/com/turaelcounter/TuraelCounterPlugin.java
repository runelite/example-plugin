package com.turaelcounter;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.Client;


@Slf4j
@PluginDescriptor(
	name = "Turael Counter"
)
public class TuraelCounterPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private TuraelCounterConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	private int streakReset = 0;

	private int streakVarbit = Varbits.SLAYER_TASK_STREAK;

	int previousStreakValue = -1;

//			client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

	@Override
	protected void startUp() throws Exception {
		log.info("Reset Counter started");
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Reset Counter ended");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
//			current streak value goes to 0 on streak reset
			int currentStreakValue = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);
			log.info("slayer streak varbit value is: " + currentStreakValue);
		}
	}

	@Provides
	TuraelCounterConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TuraelCounterConfig.class);
	}

	private void updateStreakResetCount() {

		if (streakReset == 0) {
			log.info("Infobox created here");

//			infoBoxManager.addInfoBox(Integer.format(turaelInteractionsCounter));
		}
		streakReset++;
		log.info("Slayer streak reset. Current count is " + streakReset);

	}

	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		int varbitId = varbitChanged.getVarbitId();
		log.info("current varbit id =" + varbitId);

		if (varbitId == streakVarbit)
		{
			log.info("varbit id is equal to streak varbit" + varbitId);
			int currentStreakValue = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

			if (previousStreakValue != 0 && currentStreakValue < previousStreakValue)
			{
				updateStreakResetCount();
				log.info("increasing reset counter");
			}
			previousStreakValue = currentStreakValue;
		}
	}

//	@Subscribe
//	public void onInteractingChanged(InteractingChanged interactingChanged)
//	{
//		Actor interactingNpc = client.getLocalPlayer().getInteracting();
//
//		//Turael ID = 401
//		if (interactingNpc instanceof NPC && ((NPC) interactingNpc).getId() == 401)
//		{
//			updateStreakResetCount();
//		}
//	}
//test if this changed
}
