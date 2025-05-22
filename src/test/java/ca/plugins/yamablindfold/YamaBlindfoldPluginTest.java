package ca.plugins.yamablindfold;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class YamaBlindfoldPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(YamaBlindfoldPlugin.class);
		RuneLite.main(args);
	}
}