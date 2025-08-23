package gimpanel.tracker.models;

import lombok.Data;
import java.util.Map;

@Data
public class CollectionLogData
{
    private String playerName;
    private String itemName;
    private String category;
    private int collectedItems;
    private int totalCollectionItems;
    private double completionPercentage;
    private Map<String, Integer> categoryProgress;
    private long timestamp;
    
    public CollectionLogData()
    {
        this.timestamp = System.currentTimeMillis();
    }
    
    public CollectionLogData(String playerName, String itemName, String category, 
                           int collectedItems, int totalCollectionItems)
    {
        this.playerName = playerName;
        this.itemName = itemName;
        this.category = category;
        this.collectedItems = collectedItems;
        this.totalCollectionItems = totalCollectionItems;
        this.completionPercentage = calculateCompletionPercentage();
        this.timestamp = System.currentTimeMillis();
    }
    
    private double calculateCompletionPercentage()
    {
        if (totalCollectionItems == 0) return 0.0;
        return (double) collectedItems / totalCollectionItems * 100.0;
    }
    
    public void updateProgress()
    {
        this.completionPercentage = calculateCompletionPercentage();
    }
}