package me.clogged;

import me.clogged.data.config.DisplayMethod;
import me.clogged.data.config.SyncMethod;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("clogged")
public interface CloggedConfig extends Config
{
	String PLUGIN_VERSION = "1.1.2";

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
		keyName = "profileVisibility",
		name = "Make profile visible on Clogged.me",
		description = "If enabled, your profile will be publicly viewable on the Clogged.me website.",
		position = 3
	)
	default boolean profileVisibility()
	{
		return false;
	}

	@ConfigItem(
		keyName = "syncMethod",
		name = "Sync method",
		description = "Manual: Must type '!clog sync' with collection log interface open.<br>" +
				"Automatic: Syncs whenever the collection log interface is open (might cause strange behavior for a split second when opening).",
		position = 4
	)
	default SyncMethod syncMethod()
	{
		return SyncMethod.MANUAL;
	}

	@ConfigSection(
		name = "Display Settings",
		description = "Display settings for Clogged.me in-game messages.",
		position = 5
    )
	String displaySettingsSection = "displaySettingsSection";

	@ConfigItem(
		keyName = "displayMethod",
		name = "Display method",
		description = "Text: Collection log items will be displayed as text.<br>" +
				"Icons: Collection log items will be displayed as icons.",
		position = 6,
		section = displaySettingsSection
	)
	default DisplayMethod displayMethod()
	{
		return DisplayMethod.TEXT;
	}

	@ConfigItem(
		keyName = "showQuantity",
		name = "Show item quantities",
		description = "If enabled, the quantity of each item will be shown in the collection log message.",
		position = 7,
		section = displaySettingsSection
	)
	default boolean showQuantity() {
		return true;
	}

	@ConfigItem(
		keyName = "showTotal",
		name = "Show clog progress",
		description = "If enabled, the progress of the collection log will be shown in the collection log message (e.g. 4/7).",
		position = 8,
		section = displaySettingsSection
	)
	default boolean showTotal() {
		return false;
	}

	@ConfigItem(
		keyName = "showMissing",
		name = "Show missing items",
		description = "If enabled, the '!clog missing ...' command will show the items that are missing in the collection log. (e.g. !clog missing cox)",
		position = 9,
		section = displaySettingsSection
	)
	default boolean showMissing() {
		return true;
	}

	@ConfigItem(
		keyName = "customChatColor",
		name = "Enable custom chat message color",
		description = "If enabled, chat messages sent by Clogged.me will be the color you choose below.",
		position = 10,
		section = displaySettingsSection
	)
	default boolean enableCustomColor() {
		return false;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Chat message color",
		description = "All text messages sent by the plugin will be this color if 'Enable custom chat message color' is enabled.",
		position = 11,
		section = displaySettingsSection
	)
	default Color textColor()
	{
		return Color.BLACK;
	}

	@ConfigSection(
		name = "Proxy Settings",
		description = "Proxy settings for Clogged.me API",
		position = 12,
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
		position = 13
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
		position = 14
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
		position = 15
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
		position = 16
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
		position = 17
	)
	default String proxyPassword()
	{
		return "";
	}
}
