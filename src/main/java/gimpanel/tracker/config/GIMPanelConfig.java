package gimpanel.tracker.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("gimpaneltracker")
public interface GIMPanelConfig extends Config
{
    @ConfigItem(
        keyName = "gimpanelUrl",
        name = "GIMPanel Backend URL",
        description = "Your GIMPanel backend URL (e.g., https://your-backend.com or http://localhost:3000)"
    )
    default String gimpanelUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "authToken",
        name = "Authentication Token",
        description = "Token provided by your GIMPanel group"
    )
    default String authToken()
    {
        return "";
    }

    @ConfigItem(
        keyName = "shareInventory",
        name = "Share Inventory",
        description = "Allow group members to see your inventory contents"
    )
    default boolean shareInventory()
    {
        return false;
    }

    @ConfigItem(
        keyName = "shareLocation",
        name = "Share Location",
        description = "Allow group members to see your current location"
    )
    default boolean shareLocation()
    {
        return true;
    }

    @ConfigItem(
        keyName = "shareResources",
        name = "Share Resources",
        description = "Allow group members to see your health, prayer, energy, and special attack"
    )
    default boolean shareResources()
    {
        return true;
    }

    @ConfigItem(
        keyName = "updateInterval",
        name = "Update Interval (seconds)",
        description = "How often to sync data with GIMPanel (minimum 5 seconds)"
    )
    @Range(min = 5, max = 300)
    default int updateInterval()
    {
        return 30;
    }

    @ConfigItem(
        keyName = "locationUpdateFrequency",
        name = "Location Update Frequency (ticks)",
        description = "How often to update location data (1 tick = ~0.6 seconds)"
    )
    @Range(min = 1, max = 50)
    default int locationUpdateFrequency()
    {
        return 1;
    }

    @ConfigItem(
        keyName = "resourceUpdateFrequency",
        name = "Resource Update Frequency (ticks)",
        description = "How often to update resource data (health, prayer, etc.)"
    )
    @Range(min = 5, max = 100)
    default int resourceUpdateFrequency()
    {
        return 10;
    }

    @ConfigItem(
        keyName = "enableDropTracking",
        name = "Track Drops",
        description = "Enable drop tracking and notifications"
    )
    default boolean enableDropTracking()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableSkillTracking",
        name = "Track Skills",
        description = "Enable skill and XP tracking"
    )
    default boolean enableSkillTracking()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableQuestTracking",
        name = "Track Quests",
        description = "Enable quest and achievement tracking"
    )
    default boolean enableQuestTracking()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableDifferentialUpdates",
        name = "Enable Differential Updates",
        description = "Only send data when significant changes occur (improves performance)"
    )
    default boolean enableDifferentialUpdates()
    {
        return true;
    }

    @ConfigItem(
        keyName = "enableHeartbeat",
        name = "Enable Heartbeat",
        description = "Send periodic heartbeat to maintain connection status"
    )
    default boolean enableHeartbeat()
    {
        return true;
    }

    @ConfigItem(
        keyName = "heartbeatInterval",
        name = "Heartbeat Interval (seconds)",
        description = "How often to send heartbeat signals"
    )
    @Range(min = 15, max = 300)
    default int heartbeatInterval()
    {
        return 30;
    }
}