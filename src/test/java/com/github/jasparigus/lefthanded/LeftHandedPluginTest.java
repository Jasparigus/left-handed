package com.github.jasparigus.lefthanded;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LeftHandedPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LeftHandedPlugin.class);
		RuneLite.main(args);
	}
}
