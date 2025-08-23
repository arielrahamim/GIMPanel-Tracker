package gimpanel.tracker.models;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AchievementDiaryData
{
    private String playerName;
    private String area;
    private String difficulty;
    private boolean completed;
    private int completedTasks;
    private int totalTasks;
    private List<String> rewards;
    private Map<String, Object> taskProgress;
    private long timestamp;
    
    public AchievementDiaryData()
    {
        this.timestamp = System.currentTimeMillis();
    }
    
    public AchievementDiaryData(String playerName, String area, String difficulty, 
                               int completedTasks, int totalTasks, List<String> rewards)
    {
        this.playerName = playerName;
        this.area = area;
        this.difficulty = difficulty;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.completed = completedTasks >= totalTasks;
        this.rewards = rewards;
        this.timestamp = System.currentTimeMillis();
    }
    
    public double getCompletionPercentage()
    {
        if (totalTasks == 0) return 0.0;
        return (double) completedTasks / totalTasks * 100.0;
    }
}