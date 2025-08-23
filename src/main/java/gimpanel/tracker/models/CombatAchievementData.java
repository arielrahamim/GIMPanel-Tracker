package gimpanel.tracker.models;

import lombok.Data;
import java.util.Map;

@Data
public class CombatAchievementData
{
    private String playerName;
    private String achievementName;
    private String tier;
    private String category;
    private boolean completed;
    private int points;
    private String description;
    private Map<String, Integer> tierProgress;
    private long timestamp;
    
    public CombatAchievementData()
    {
        this.timestamp = System.currentTimeMillis();
    }
    
    public CombatAchievementData(String playerName, String achievementName, String tier, 
                                String category, boolean completed, int points)
    {
        this.playerName = playerName;
        this.achievementName = achievementName;
        this.tier = tier;
        this.category = category;
        this.completed = completed;
        this.points = points;
        this.timestamp = System.currentTimeMillis();
    }
    
    public enum Tier
    {
        EASY("Easy", 1),
        MEDIUM("Medium", 2),
        HARD("Hard", 4),
        ELITE("Elite", 10),
        MASTER("Master", 25),
        GRANDMASTER("Grandmaster", 50);
        
        private final String displayName;
        private final int points;
        
        Tier(String displayName, int points)
        {
            this.displayName = displayName;
            this.points = points;
        }
        
        public String getDisplayName() { return displayName; }
        public int getPoints() { return points; }
    }
}