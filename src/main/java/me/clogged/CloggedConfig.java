package me.clogged;

import me.clogged.data.config.DisplayMethod;
import me.clogged.data.config.SyncMethod;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("clogged")
public interface CloggedConfig extends Config
{
	String PLUGIN_VERSION = "1.1.1";

	@ConfigItem(
		keyName = "enableSync",
		name = "Enable sync with Clogged.me",
		description = "This will sync your collection log with Clogged.me",
		position = 1
	)
	default boolean enableSync()
	{
		return false;
	}

	@ConfigItem(
			keyName = "enableLookup",
			name = "Enable lookups with Clogged.me",
			description = "This will allow you to view collection logs from other players.<br>This does not sync your log with Clogged.me.",
			position = 2
	)
	default boolean enableLookup()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showQuantity",
		name = "Show item quantities",
		description = "If enabled, the quantity of each item will be shown in the collection log message.",
		position = 3
	)
	default boolean showQuantity() {
		return true;
	}

	@ConfigItem(
			keyName = "showTotal",
			name = "Show clog totals",
			description = "If enabled, the total number of items in the collection log will be shown in the collection log message (e.g. 4/7).",
			position = 4
	)
	default boolean showTotal() {
		return false;
	}

	@ConfigItem(
			keyName = "showMissing",
			name = "Show missing items",
			description = "If enabled, the '!clog missing ...' command will show the items that are missing in the collection log.",
			position = 5
	)
	default boolean showMissing() {
		return true;
	}

	@ConfigItem(
		keyName = "syncMethod",
		name = "Sync method",
		description = "Manual: Must type '!clog sync' with collection log interface open.<br>" +
			"Automatic: Syncs whenever the collection log interface is open (might cause strange behavior for a split second when opening).",
		position = 6
	)
	default SyncMethod syncMethod()
	{
		return SyncMethod.MANUAL;
	}

	@ConfigItem(
			keyName = "displayMethod",
			name = "Display method",
			description = "Text: Collection log items will be displayed as text.<br>" +
					"Icons: Collection log items will be displayed as icons.",
			position = 7
	)

	default DisplayMethod displayMethod()
	{
		return DisplayMethod.TEXT;
	}

	@ConfigSection(
			name = "Proxy Settings",
			description = "Proxy settings for Clogged.me API",
			position = 8,
			closedByDefault = true
	)
	String proxySettingsSection = "proxySettingsSection";

	@ConfigItem(
			keyName = "proxyEnabled",
			name = "Enable proxy",
			description = "Clogged.me does not store nor associate your IP address with your account or client in any way.<br>" +
					"With that being said, enabling this will use the specified proxy settings to connect to the Clogged.me API.<br>" +
					"Only enable this if you know what you're doing.",
			section = proxySettingsSection,
			position = 9
	)
	default boolean proxyEnabled()
	{
		return false;
	}

	@ConfigItem(
			keyName = "proxyHost",
			name = "Proxy Host",
			description = "The host of the proxy server.",
			section = proxySettingsSection,
			position = 10
	)
	default String proxyHost()
	{
		return "";
	}

	@ConfigItem(
			keyName = "proxyPort",
			name = "Proxy Port",
			description = "The port of the proxy server.",
			section = proxySettingsSection,
			position = 11
	)
	default int proxyPort()
	{
		return 0;
	}

	@ConfigItem(
			keyName = "proxyUsername",
			name = "Proxy Username",
			description = "The username for the proxy server.",
			section = proxySettingsSection,
			position = 12
	)
	default String proxyUsername()
	{
		return "";
	}

	@ConfigItem(
			keyName = "proxyPassword",
			name = "Proxy Password",
			description = "The password for the proxy server.",
			section = proxySettingsSection,
			position = 13
	)
	default String proxyPassword()
	{
		return "";
	}
}
