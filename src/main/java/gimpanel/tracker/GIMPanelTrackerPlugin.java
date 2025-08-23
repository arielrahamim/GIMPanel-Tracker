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

    @Inject
    private StashCollector stashCollector;

    @Inject
    private GroupStorageCollector groupStorageCollector;
    
    @Inject
    private AchievementDiaryCollector achievementDiaryCollector;
    
    @Inject
    private CollectionLogCollector collectionLogCollector;
    
    @Inject
    private CombatAchievementCollector combatAchievementCollector;

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
        if (!config.enableSkillTracking()) {
            log.debug("StatChanged skipped - skill tracking disabled");
            return;
        }
        
        log.info("StatChanged event: {} XP: {} Level: {}", event.getSkill(), event.getXp(), event.getLevel());
        skillCollector.onStatChanged(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        log.debug("ChatMessage event: {}", event.getMessage());
        
        if (config.enableDropTracking())
        {
            dropCollector.onChatMessage(event);
        }
        
        if (config.enableCollectionLogTracking())
        {
            collectionLogCollector.onChatMessage(event);
        }
        
        if (config.enableCombatAchievementTracking())
        {
            combatAchievementCollector.onChatMessage(event);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // OPTIMIZATION: Only process if player is logged in
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        
        // Disable game tick logging but keep essential processing
        // locationCollector.onGameTick(event);  // Commented out - not needed right now
        // resourceCollector.onGameTick(event);  // Commented out - not needed right now
        stateTracker.onGameTick(event);  // Keep for login/logout detection
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (config.enableQuestTracking())
        {
            questCollector.onVarbitChanged(event);
        }
        
        if (config.enableAchievementTracking())
        {
            achievementDiaryCollector.onVarbitChanged(event);
        }
        
        if (config.enableCombatAchievementTracking())
        {
            combatAchievementCollector.onVarbitChanged(event);
        }
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
        
        // Always process stash and group storage (they have their own config checks)
        stashCollector.onItemContainerChanged(event);
        groupStorageCollector.onItemContainerChanged(event);
    }

    @Provides
    GIMPanelConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GIMPanelConfig.class);
    }
}