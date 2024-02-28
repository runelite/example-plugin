package com.turaelcounter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TuraelCounterTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TuraelCounterPlugin.class);
		RuneLite.main(args);
	}
}