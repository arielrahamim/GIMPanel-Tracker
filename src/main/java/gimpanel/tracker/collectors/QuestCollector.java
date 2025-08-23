package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.QuestData;
import gimpanel.tracker.models.EnhancedQuestData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.VarbitChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class QuestCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private final Map<Quest, QuestState> previousQuestStates = new HashMap<>();
    private final Map<Integer, String> varbitToQuestMap = new HashMap<>();
    private long lastQuestCheck = 0;
    private static final long QUEST_CHECK_INTERVAL = 30000; // Check every 30 seconds

    @Inject
    public QuestCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
        initializePreviousQuestStates();
    }

    private void initializePreviousQuestStates()
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        // Initialize known quest varbit mappings to avoid reentrancy
        initializeVarbitMapping();
        
        // Don't call quest.getState() here to avoid reentrancy issues
        // Instead, we'll detect changes through varbit events
        log.info("Quest collector initialized with safe varbit tracking");
    }
    
    private void initializeVarbitMapping()
    {
        // Map known quest varbits to quest names
        // This is a simplified mapping - in practice, you'd have a comprehensive mapping
        varbitToQuestMap.put(32, "Cook's Assistant");
        varbitToQuestMap.put(29, "Demon Slayer");
        varbitToQuestMap.put(176, "Dragon Slayer I");
        varbitToQuestMap.put(146, "Recipe for Disaster");
        // Add more quest varbit mappings as needed
    }

    public void onVarbitChanged(VarbitChanged event)
    {
        if (!config.enableQuestTracking())
        {
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        // Use varbit changes to detect quest progress without reentrancy
        int varbitId = event.getVarbitId();
        int value = event.getValue();
        
        String questName = varbitToQuestMap.get(varbitId);
        if (questName != null)
        {
            handleQuestVarbitChange(playerName, questName, varbitId, value);
        }
        else
        {
            // Log unknown varbits for future mapping
            log.debug("Unknown quest varbit changed: {} = {}", varbitId, value);
        }
    }
    
    private void handleQuestVarbitChange(String playerName, String questName, int varbitId, int value)
    {
        QuestData.QuestStatus status = mapVarbitToQuestState(questName, value);
        
        // Create enhanced quest data with additional information
        EnhancedQuestData questData = new EnhancedQuestData(
            playerName, questName, status,
            getQuestPoints(questName), 
            getQuestRequirements(questName),
            getQuestRewards(questName)
        );
        
        if (status == QuestData.QuestStatus.COMPLETED)
        {
            log.info("Quest completed! {} finished '{}'", playerName, questName);
        }
        else if (status == QuestData.QuestStatus.IN_PROGRESS)
        {
            log.info("Quest progress! {} progressing in '{}'", playerName, questName);
        }
        
        dataManager.queueEnhancedQuestUpdate(questData);
    }
    
    private QuestData.QuestStatus mapVarbitToQuestState(String questName, int value)
    {
        // This is a simplified mapping - each quest has different completion values
        if (value == 0)
        {
            return QuestData.QuestStatus.NOT_STARTED;
        }
        else if (isQuestCompleteValue(questName, value))
        {
            return QuestData.QuestStatus.COMPLETED;
        }
        else
        {
            return QuestData.QuestStatus.IN_PROGRESS;
        }
    }
    
    private boolean isQuestCompleteValue(String questName, int value)
    {
        // Quest-specific completion values
        switch (questName)
        {
            case "Cook's Assistant":
                return value == 2;
            case "Demon Slayer":
                return value == 3;
            case "Dragon Slayer I":
                return value == 10;
            default:
                return value > 100; // General assumption
        }
    }
    
    private int getQuestPoints(String questName)
    {
        // Hardcoded quest points - could be loaded from external data
        switch (questName)
        {
            case "Cook's Assistant":
                return 1;
            case "Demon Slayer":
                return 3;
            case "Dragon Slayer I":
                return 2;
            case "Recipe for Disaster":
                return 10;
            default:
                return 1;
        }
    }
    
    private Map<String, Object> getQuestRequirements(String questName)
    {
        // Return quest requirements - simplified for now
        return Map.of("level", 1, "items", List.of());
    }
    
    private Map<String, Object> getQuestRewards(String questName)
    {
        // Return quest rewards - simplified for now
        return Map.of("xp", 1000, "items", List.of(), "access", List.of());
    }

    private void handleQuestStateChange(String playerName, Quest quest, QuestState oldState, QuestState newState)
    {
        QuestData.QuestStatus status = mapQuestState(newState);
        QuestData questData = new QuestData(playerName, quest.getName(), status);
        
        // Set quest points if completed
        if (newState == QuestState.FINISHED)
        {
            // Quest points are not directly available from Quest object in RuneLite API
            questData.setQuestPoints(1); // Default to 1 quest point
            log.info("Quest completed! {} finished '{}'", 
                playerName, quest.getName());
        }
        else if (newState == QuestState.IN_PROGRESS && oldState == QuestState.NOT_STARTED)
        {
            log.info("Quest started! {} began '{}'", playerName, quest.getName());
        }

        log.debug("Quest state change: {} - '{}' changed from {} to {}", 
            playerName, quest.getName(), oldState, newState);

        dataManager.queueQuestUpdate(questData);
    }

    private QuestData.QuestStatus mapQuestState(QuestState questState)
    {
        switch (questState)
        {
            case NOT_STARTED:
                return QuestData.QuestStatus.NOT_STARTED;
            case IN_PROGRESS:
                return QuestData.QuestStatus.IN_PROGRESS;
            case FINISHED:
                return QuestData.QuestStatus.COMPLETED;
            default:
                return QuestData.QuestStatus.NOT_STARTED;
        }
    }

    public void refreshAllQuests()
    {
        if (!config.enableQuestTracking())
        {
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        log.debug("Refreshing all quest states for {}", playerName);

        // Use a safer approach - don't call quest.getState() during script execution
        // Instead, just initialize the quest states to avoid the reentrant error
        log.info("Quest tracking initialized for {} (safe mode to avoid script reentrancy)", playerName);
        
        // We'll track quest changes through other means or defer this to a safer time
        // For now, just mark as initialized
    }
}