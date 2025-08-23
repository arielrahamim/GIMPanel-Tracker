package gimpanel.tracker.models;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class EnhancedInventoryData
{
    private String playerName;
    private String containerName;
    private List<EnhancedInventoryItem> items;
    private long timestamp;
    private long totalValue;
    private int totalItems;
    private int uniqueItems;
    private Map<String, CategoryBreakdown> categories;
    private Map<String, ValueRangeBreakdown> valueRanges;

    public EnhancedInventoryData(String playerName, String containerName, List<EnhancedInventoryItem> items,
                                long totalValue, int totalItems, int uniqueItems)
    {
        this.playerName = playerName;
        this.containerName = containerName;
        this.items = items;
        this.totalValue = totalValue;
        this.totalItems = totalItems;
        this.uniqueItems = uniqueItems;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Data
    public static class EnhancedInventoryItem
    {
        private int itemId;
        private String itemName;
        private int quantity;
        private int slot;
        private int itemValue;
        private long totalValue;
        private String category;
        private boolean noted;
        
        public EnhancedInventoryItem(int itemId, String itemName, int quantity, int slot,
                                   int itemValue, long totalValue, String category, boolean noted)
        {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.slot = slot;
            this.itemValue = itemValue;
            this.totalValue = totalValue;
            this.category = category;
            this.noted = noted;
        }
    }
    
    @Data
    public static class CategoryBreakdown
    {
        private String category;
        private int count;
        private long value;
        
        public CategoryBreakdown(String category, int count, long value)
        {
            this.category = category;
            this.count = count;
            this.value = value;
        }
    }
    
    @Data
    public static class ValueRangeBreakdown
    {
        private String range;
        private int count;
        private long value;
        
        public ValueRangeBreakdown(String range, int count, long value)
        {
            this.range = range;
            this.count = count;
            this.value = value;
        }
    }
}