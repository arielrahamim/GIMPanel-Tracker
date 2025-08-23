package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.AchievementDiaryData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class AchievementDiaryCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private final Map<String, DiaryProgress> diaryProgress = new HashMap<>();
    private final Map<Integer, DiaryTask> varbitToDiaryMap = new HashMap<>();
    
    @Inject
    public AchievementDiaryCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
        initializeDiaryMapping();
    }
    
    private void initializeDiaryMapping()
    {
        // Initialize known achievement diary varbit mappings
        // This is a simplified mapping - would need comprehensive mapping for all diaries
        
        // Lumbridge & Draynor Easy
        varbitToDiaryMap.put(2501, new DiaryTask("Lumbridge", "Easy", "Cook and eat a lobster in Lumbridge Castle", 1));
        varbitToDiaryMap.put(2502, new DiaryTask("Lumbridge", "Easy", "Obtain a full bucket of milk from a dairy cow", 2));
        
        // Varrock Easy
        varbitToDiaryMap.put(3095, new DiaryTask("Varrock", "Easy", "Browse Sarah's Farming Shop", 1));
        varbitToDiaryMap.put(3096, new DiaryTask("Varrock", "Easy", "Have Aubury teleport you to the essence mine", 2));
        
        // Falador Easy
        varbitToDiaryMap.put(2975, new DiaryTask("Falador", "Easy", "Fill a bucket from the well", 1));
        varbitToDiaryMap.put(2976, new DiaryTask("Falador", "Easy", "Kill a duck in Falador Park", 2));
        
        log.info("Achievement Diary collector initialized with {} diary task mappings", varbitToDiaryMap.size());
    }
    
    public void onVarbitChanged(VarbitChanged event)
    {
        if (!config.enableAchievementTracking())
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
        
        int varbitId = event.getVarbitId();
        int value = event.getValue();
        
        DiaryTask task = varbitToDiaryMap.get(varbitId);
        if (task != null)
        {
            handleDiaryTaskProgress(playerName, task, value > 0);
        }
    }
    
    private void handleDiaryTaskProgress(String playerName, DiaryTask task, boolean completed)
    {
        String diaryKey = task.getArea() + "_" + task.getDifficulty();
        DiaryProgress progress = diaryProgress.get(diaryKey);
        
        if (progress == null)
        {
            progress = new DiaryProgress(task.getArea(), task.getDifficulty());
            diaryProgress.put(diaryKey, progress);
        }
        
        boolean wasCompleted = progress.isTaskCompleted(task.getTaskId());
        progress.updateTask(task.getTaskId(), completed);
        
        // Only send update if task completion status changed
        if (completed && !wasCompleted)
        {
            log.info("Achievement Diary task completed! {} finished '{}' in {} {}", 
                playerName, task.getDescription(), task.getArea(), task.getDifficulty());
                
            // Send individual task completion
            AchievementDiaryData diaryData = new AchievementDiaryData(
                playerName, task.getArea(), task.getDifficulty(),
                progress.getCompletedCount(), progress.getTotalTasks(),
                getDiaryRewards(task.getArea(), task.getDifficulty())
            );
            
            dataManager.queueAchievementDiaryUpdate(diaryData);
            
            // Check if entire diary tier is now complete
            if (progress.isCompleted())
            {
                log.info("Achievement Diary tier completed! {} completed {} {} diary", 
                    playerName, task.getArea(), task.getDifficulty());
            }
        }
    }
    
    private List<String> getDiaryRewards(String area, String difficulty)
    {
        // Simplified reward mapping
        String key = area + "_" + difficulty;
        switch (key)
        {
            case "Lumbridge_Easy":
                return Arrays.asList("Explorer's ring 1", "Lumbridge teleports");
            case "Varrock_Easy":
                return Arrays.asList("Varrock armour 1", "Varrock teleports");
            case "Falador_Easy":
                return Arrays.asList("Falador shield 1", "Falador teleports");
            default:
                return Arrays.asList("Diary rewards");
        }
    }
    
    // Helper classes
    private static class DiaryTask
    {
        private final String area;
        private final String difficulty;
        private final String description;
        private final int taskId;
        
        public DiaryTask(String area, String difficulty, String description, int taskId)
        {
            this.area = area;
            this.difficulty = difficulty;
            this.description = description;
            this.taskId = taskId;
        }
        
        public String getArea() { return area; }
        public String getDifficulty() { return difficulty; }
        public String getDescription() { return description; }
        public int getTaskId() { return taskId; }
    }
    
    private static class DiaryProgress
    {
        private final String area;
        private final String difficulty;
        private final Map<Integer, Boolean> taskCompletion;
        private final int totalTasks;
        
        public DiaryProgress(String area, String difficulty)
        {
            this.area = area;
            this.difficulty = difficulty;
            this.taskCompletion = new HashMap<>();
            this.totalTasks = getTotalTasksForDiary(area, difficulty);
        }
        
        private int getTotalTasksForDiary(String area, String difficulty)
        {
            // Hardcoded task counts - would be loaded from external data in practice
            String key = area + "_" + difficulty;
            switch (key)
            {
                case "Lumbridge_Easy":
                    return 16;
                case "Varrock_Easy":
                    return 13;
                case "Falador_Easy":
                    return 11;
                default:
                    return 10; // Default assumption
            }
        }
        
        public void updateTask(int taskId, boolean completed)
        {
            taskCompletion.put(taskId, completed);
        }
        
        public boolean isTaskCompleted(int taskId)
        {
            return taskCompletion.getOrDefault(taskId, false);
        }
        
        public int getCompletedCount()
        {
            return (int) taskCompletion.values().stream().mapToInt(b -> b ? 1 : 0).sum();
        }
        
        public int getTotalTasks()
        {
            return totalTasks;
        }
        
        public boolean isCompleted()
        {
            return getCompletedCount() >= totalTasks;
        }
        
        public String getArea() { return area; }
        public String getDifficulty() { return difficulty; }
    }
}