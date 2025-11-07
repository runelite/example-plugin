package com.example.PiggyUtils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "<html><font color=\"#FF9DF9\">[PP]</font> PiggyUtils</html>",
        description = "Utility Plugin for PiggyPlugins",
        tags = {"piggy","ethan"})
@Slf4j
public class PiggyUtilsPlugin extends Plugin {
    @Override
    protected void startUp() throws Exception {
        log.info("[PiggyUtils] Piggy Utils started");
    }
}