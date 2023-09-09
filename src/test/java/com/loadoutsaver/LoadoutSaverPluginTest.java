package com.loadoutsaver;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LoadoutSaverPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LoadoutSaverPlugin.class);
		RuneLite.main(args);
	}
}