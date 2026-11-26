package com.github.jasparigus.lefthanded;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LeftHandedConfig.GROUP)
public interface LeftHandedConfig extends Config
{
	String GROUP = "left-handed";

	public enum LeftHandMode
	{
		OFF,
		ALWAYS,
		BOW_ONLY
	}

	@ConfigItem(
		keyName = "mode",
		name = "Mode",
		description = "OFF: nothing. ALWAYS: mirror your character always. BOW_ONLY: mirror while a bow is wielded",
		position = 0
	)
	default LeftHandMode mode()
	{
		return LeftHandMode.OFF;
	}

	@ConfigItem(
		keyName = "mirrorCrossbows",
		name = "Mirror crossbows",
		description = "In BOW ONLY mode, crossbows also trigger the mirror. OSRS crossbows are one-handed, " +
			"so unchecking this (no mirror for crossbows) is reasonable if you prefer to think of them as " +
			"trigger-hand weapons.",
		position = 1
	)
	default boolean mirrorCrossbows()
	{
		return true;
	}
}
