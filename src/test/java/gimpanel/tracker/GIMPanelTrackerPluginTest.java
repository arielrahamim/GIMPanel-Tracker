package gimpanel.tracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GIMPanelTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GIMPanelTrackerPlugin.class);
		RuneLite.main(args);
	}
}