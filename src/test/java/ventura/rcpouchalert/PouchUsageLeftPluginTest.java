package ventura.rcpouchalert;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PouchUsageLeftPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PouchUsageLeftPlugin.class);
		RuneLite.main(args);
	}
}