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
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.util.regex.*;

@Slf4j
@PluginDescriptor(
	name = "Turael Counter",
	description = "Counts streak resets"
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

	@Inject
	private TuraelCounterOverlay overlay;

	public static int streakReset = 0;

	private int streakVarbit = Varbits.SLAYER_TASK_STREAK;

	private int previousStreakValue = -1;

	private int overlayVisible;

	private TuraelCounterOverlay counterBox;

	@Override
	protected void startUp() throws Exception {
		log.info("Reset Counter started");
		overlayVisible = -1;
		updateInfobox();

	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Reset Counter ended");
		removeInfobox();
	}

	private void updateInfobox()
	{
		removeInfobox();
		BufferedImage image = ImageUtil.loadImageResource(getClass(), "tzkal-zuk.png");

//		counterBox = new TuraelCounterOverlay(config, client, this, image);

		counterBox = new TuraelCounterOverlay(this,streakReset, image );
		infoBoxManager.addInfoBox(counterBox);
	}

	private void removeInfobox()
	{
			infoBoxManager.removeInfoBox(counterBox);
			counterBox = null;

	}

	@Provides
	TuraelCounterConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TuraelCounterConfig.class);
	}

	private void updateStreakResetCount() {

		if (streakReset == 0) {
			log.info("Infobox created here");
			updateInfobox();
		}
		streakReset++;
		log.info("Slayer streak reset. Current count is " + streakReset);

	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged) {
		int varbitId = varbitChanged.getVarbitId();

		if (varbitId == streakVarbit) {
			int currentStreakValue = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

			if (previousStreakValue != 0 && currentStreakValue < previousStreakValue) {
				updateStreakResetCount();
			}
			previousStreakValue = currentStreakValue;
		}
	}
}
