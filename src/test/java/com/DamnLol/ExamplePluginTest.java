package com.DamnLol;

import com.DamnLol.ChasmSigil.ChasmSigilPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChasmSigilPlugin.class);
		RuneLite.main(args);
	}
}