package gimpanel.tracker;

import com.google.inject.Provides;
import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.managers.StateTracker;
import gimpanel.tracker.collectors.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
    name = "GIMPanel Tracker",
    description = "Real-time Group Ironman progress tracking for GIMPanel dashboard",
    tags = {"group ironman", "tracking", "dashboard", "stats", "progress"}
)
public class GIMPanelTrackerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private GIMPanelConfig config;

    @Inject
    private DataManager dataManager;

    @Inject
    private StateTracker stateTracker;

    @Inject
    private SkillCollector skillCollector;

    @Inject
    private DropCollector dropCollector;

    @Inject
    private LocationCollector locationCollector;

    @Inject
    private ResourceCollector resourceCollector; // NEW: Add resource collector

    @Inject
    private QuestCollector questCollector;

    @Inject
    private InventoryCollector inventoryCollector;

    @Override
    protected void startUp() throws Exception
    {
        log.info("GIMPanel Tracker started!");
        log.info("Config - URL: {}, Token: {}", config.gimpanelUrl(), config.authToken().isEmpty() ? "NOT SET" : "SET");
        
        if (config.gimpanelUrl().isEmpty() || config.authToken().isEmpty())
        {
            log.warn("GIMPanel URL or auth token not configured. Please configure the plugin.");
            return;
        }

        log.info("Initializing DataManager...");
        dataManager.initialize();
        log.info("Initializing StateTracker...");
        stateTracker.initialize();
        log.info("GIMPanel Tracker initialization complete!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("GIMPanel Tracker stopped!");
        dataManager.shutdown();
        stateTracker.shutdown();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (!config.enableSkillTracking()) return;
        
        log.debug("StatChanged event: {} {} -> {}", event.getSkill(), event.getXp(), event.getLevel());
        skillCollector.onStatChanged(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (!config.enableDropTracking()) return;
        
        log.debug("ChatMessage event: {}", event.getMessage());
        dropCollector.onChatMessage(event);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        log.debug("GameTick event");
        
        // OPTIMIZATION: Only process if player is logged in
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        
        locationCollector.onGameTick(event);
        resourceCollector.onGameTick(event); // NEW: Add resource tracking
        stateTracker.onGameTick(event);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (!config.enableQuestTracking()) return;
        
        questCollector.onVarbitChanged(event);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        stateTracker.onGameStateChanged(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        log.debug("ItemContainerChanged event: containerId={}", event.getContainerId());
        if (config.shareInventory())
        {
            inventoryCollector.onItemContainerChanged(event);
        }
    }

    @Provides
    GIMPanelConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GIMPanelConfig.class);
    }
}