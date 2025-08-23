package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnhancedQuestData extends QuestData
{
    private int questPoints;
    private int progress; // 0-100
    private Map<String, Object> requirements;
    private Map<String, Object> rewards;
    private String currentStep;
    private List<String> completedSteps;
    private String difficulty;
    private String series;
    
    public EnhancedQuestData()
    {
        super();
    }
    
    public EnhancedQuestData(String playerName, String questName, QuestStatus status, 
                            int questPoints, Map<String, Object> requirements, Map<String, Object> rewards)
    {
        super(playerName, questName, status);
        this.questPoints = questPoints;
        this.requirements = requirements;
        this.rewards = rewards;
        this.progress = calculateProgress(status);
    }
    
    private int calculateProgress(QuestStatus status)
    {
        switch (status)
        {
            case NOT_STARTED:
                return 0;
            case IN_PROGRESS:
                return 50; // Could be more sophisticated with actual progress tracking
            case COMPLETED:
                return 100;
            default:
                return 0;
        }
    }
}