package gimpanel.tracker.models;

import lombok.Data;

@Data
public class SkillData
{
    private String playerName;
    private String skillName;
    private int level;
    private long xp;
    private int rank;
    private long lastUpdated;
    private long xpGained;
    private int levelsGained;

    public SkillData()
    {
        this.lastUpdated = System.currentTimeMillis();
    }

    public SkillData(String playerName, String skillName, int level, long xp)
    {
        this.playerName = playerName;
        this.skillName = skillName;
        this.level = level;
        this.xp = xp;
        this.lastUpdated = System.currentTimeMillis();
        this.rank = -1; // Will be populated from hiscores if available
    }
}