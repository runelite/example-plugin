package com.example;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import com.example.PowerSkiller.PowerSkillerPlugin;
import com.example.PowerSkiller.PSDummy;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.PacketUtilsPlugin;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PacketUtilsPlugin.class, EthanApiPlugin.class, PowerSkillerPlugin.class);
		RuneLite.main(args);
	}
}