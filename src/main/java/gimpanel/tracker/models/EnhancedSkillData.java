package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnhancedSkillData extends SkillData
{
    private int totalLevel;
    private int combatLevel;
    private double xpPerHour;
    private long timeSinceLastUpdate;
    private double efficiency;
    
    public EnhancedSkillData()
    {
        super();
    }
    
    public EnhancedSkillData(String playerName, String skillName, int level, long xp, 
                            int totalLevel, int combatLevel, double xpPerHour, int rank)
    {
        super(playerName, skillName, level, xp);
        this.totalLevel = totalLevel;
        this.combatLevel = combatLevel;
        this.xpPerHour = xpPerHour;
        this.setRank(rank);
        this.efficiency = calculateEfficiency(xpPerHour, level);
    }
    
    private double calculateEfficiency(double xpPerHour, int level)
    {
        if (level == 99) return 100.0;
        if (xpPerHour <= 0) return 0.0;
        
        // Simple efficiency calculation based on XP/hour and level
        // Higher levels require more XP but are more efficient per action
        double baseEfficiency = Math.min(100.0, (xpPerHour / 1000.0) * 10.0);
        double levelMultiplier = 1.0 + (level - 1) * 0.01; // 1% bonus per level
        
        return Math.min(100.0, baseEfficiency * levelMultiplier);
    }
    
    public void updateTimingData(long previousUpdateTime, long currentTime)
    {
        this.timeSinceLastUpdate = currentTime - previousUpdateTime;
        
        if (this.timeSinceLastUpdate > 0 && this.getXpGained() > 0)
        {
            // Calculate XP per hour: (XP gained / time in ms) * ms per hour
            this.xpPerHour = (double) this.getXpGained() / this.timeSinceLastUpdate * 3600000.0;
            this.efficiency = calculateEfficiency(this.xpPerHour, this.getLevel());
        }
    }
}