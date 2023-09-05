package com.alteredstats;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Altered Stats"
)
public class AlteredStatsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AlteredStatsOverlay alteredStatsOverlay;

	protected void startUp() throws Exception
	{
		overlayManager.add(alteredStatsOverlay);
	}
	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(alteredStatsOverlay);
	}

	@Provides
	AlteredStatsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AlteredStatsConfig.class);
	}
}
