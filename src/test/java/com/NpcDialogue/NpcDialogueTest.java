package com.NpcDialogue;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NpcDialogueTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NpcDialoguePlugin.class);
		RuneLite.main(args);
	}
}