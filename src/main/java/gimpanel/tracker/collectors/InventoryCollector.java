package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.EnhancedInventoryData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class InventoryCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private int ticksSinceLastUpdate = 0;
    private static final int UPDATE_FREQUENCY = 100; // Update every 100 ticks (~60 seconds)

    @Inject
    public InventoryCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.shareInventory())
        {
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        // Rate limit inventory updates to avoid spam
        ticksSinceLastUpdate++;
        if (ticksSinceLastUpdate < UPDATE_FREQUENCY)
        {
            return;
        }
        ticksSinceLastUpdate = 0;

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        int containerId = event.getContainerId();
        ItemContainer container = event.getItemContainer();

        if (container == null)
        {
            return;
        }

        String containerName = getContainerName(containerId);
        if (containerName == null)
        {
            return; // Skip unknown containers
        }

        List<EnhancedInventoryData.EnhancedInventoryItem> items = new ArrayList<>();
        Item[] containerItems = container.getItems();
        long totalValue = 0;
        Map<String, Long> categoryValues = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (int i = 0; i < containerItems.length; i++)
        {
            Item item = containerItems[i];
            if (item.getId() > 0 && item.getQuantity() > 0)
            {
                String itemName = client.getItemDefinition(item.getId()).getName();
                
                // Calculate item value (simplified - in practice would use GE API)
                int itemValue = getItemValue(item.getId());
                long itemTotalValue = (long) itemValue * item.getQuantity();
                totalValue += itemTotalValue;
                
                // Categorize item
                String category = categorizeItem(itemName);
                categoryValues.merge(category, itemTotalValue, Long::sum);
                categoryCounts.merge(category, item.getQuantity(), Integer::sum);
                
                items.add(new EnhancedInventoryData.EnhancedInventoryItem(
                    item.getId(), itemName, item.getQuantity(), i,
                    itemValue, itemTotalValue, category, itemName.contains("noted")
                ));
            }
        }

        // Create category breakdown
        Map<String, EnhancedInventoryData.CategoryBreakdown> categories = categoryValues.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new EnhancedInventoryData.CategoryBreakdown(
                    entry.getKey(),
                    categoryCounts.get(entry.getKey()),
                    entry.getValue()
                )
            ));
            
        // Create value range breakdown
        Map<String, EnhancedInventoryData.ValueRangeBreakdown> valueRanges = createValueRangeBreakdown(items);
        
        EnhancedInventoryData inventoryData = new EnhancedInventoryData(
            playerName, containerName, items, totalValue,
            items.size(), getUniqueItemCount(items)
        );
        inventoryData.setCategories(categories);
        inventoryData.setValueRanges(valueRanges);
        
        log.debug("Enhanced inventory update: {} items ({} unique), {} total value in {} for {}", 
            items.size(), inventoryData.getUniqueItems(), totalValue, containerName, playerName);
        
        dataManager.queueEnhancedInventoryUpdate(inventoryData);
    }

    private String getContainerName(int containerId)
    {
        if (containerId == InventoryID.INVENTORY.getId())
        {
            return "inventory";
        }
        else if (containerId == InventoryID.BANK.getId())
        {
            return "bank";
        }
        else if (containerId == InventoryID.EQUIPMENT.getId())
        {
            return "equipment";
        }
        
        // Skip other containers for privacy/performance reasons
        return null;
    }
    
    private int getItemValue(int itemId)
    {
        // Simplified item value calculation
        // In practice, this would query Grand Exchange API or use cached prices
        
        // Hardcoded values for common items (simplified)
        switch (itemId)
        {
            case 995: // Coins
                return 1;
            case 314: // Feather
                return 2;
            case 1511: // Logs
                return 50;
            case 438: // Dwarf remains
                return 1;
            default:
                return 1; // Default fallback
        }
    }
    
    private String categorizeItem(String itemName)
    {
        String itemLower = itemName.toLowerCase();
        
        // Categorize items based on name patterns
        if (itemLower.contains("rune") || itemLower.contains("sword") || itemLower.contains("bow"))
        {
            return "Weapons & Armor";
        }
        else if (itemLower.contains("potion") || itemLower.contains("food"))
        {
            return "Consumables";
        }
        else if (itemLower.contains("ore") || itemLower.contains("bar") || itemLower.contains("gem"))
        {
            return "Resources";
        }
        else if (itemLower.contains("seed") || itemLower.contains("herb"))
        {
            return "Farming";
        }
        else if (itemLower.contains("log") || itemLower.contains("plank"))
        {
            return "Woodcutting";
        }
        else if (itemLower.contains("fish"))
        {
            return "Fishing";
        }
        else if (itemName.contains("noted"))
        {
            return "Noted Items";
        }
        else
        {
            return "Miscellaneous";
        }
    }
    
    private Map<String, EnhancedInventoryData.ValueRangeBreakdown> createValueRangeBreakdown(
        List<EnhancedInventoryData.EnhancedInventoryItem> items)
    {
        Map<String, Integer> rangeCounts = new HashMap<>();
        Map<String, Long> rangeValues = new HashMap<>();
        
        for (EnhancedInventoryData.EnhancedInventoryItem item : items)
        {
            String range = getValueRange(item.getTotalValue());
            rangeCounts.merge(range, item.getQuantity(), Integer::sum);
            rangeValues.merge(range, item.getTotalValue(), Long::sum);
        }
        
        return rangeValues.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new EnhancedInventoryData.ValueRangeBreakdown(
                    entry.getKey(),
                    rangeCounts.get(entry.getKey()),
                    entry.getValue()
                )
            ));
    }
    
    private String getValueRange(long value)
    {
        if (value == 0) return "0";
        if (value < 1000) return "1-999";
        if (value < 10000) return "1k-9.9k";
        if (value < 100000) return "10k-99.9k";
        if (value < 1000000) return "100k-999.9k";
        if (value < 10000000) return "1m-9.9m";
        return "10m+";
    }
    
    private int getUniqueItemCount(List<EnhancedInventoryData.EnhancedInventoryItem> items)
    {
        return (int) items.stream()
            .map(EnhancedInventoryData.EnhancedInventoryItem::getItemId)
            .distinct()
            .count();
    }

    public static class InventoryData
    {
        private final String playerName;
        private final String containerName;
        private final List<InventoryItem> items;
        private final long timestamp;

        public InventoryData(String playerName, String containerName, List<InventoryItem> items)
        {
            this.playerName = playerName;
            this.containerName = containerName;
            this.items = items;
            this.timestamp = System.currentTimeMillis();
        }

        public String getPlayerName()
        {
            return playerName;
        }

        public String getContainerName()
        {
            return containerName;
        }

        public List<InventoryItem> getItems()
        {
            return items;
        }

        public long getTimestamp()
        {
            return timestamp;
        }
    }

    public static class InventoryItem
    {
        private final int itemId;
        private final String itemName;
        private final int quantity;
        private final int slot;

        public InventoryItem(int itemId, String itemName, int quantity, int slot)
        {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.slot = slot;
        }

        public int getItemId()
        {
            return itemId;
        }

        public String getItemName()
        {
            return itemName;
        }

        public int getQuantity()
        {
            return quantity;
        }

        public int getSlot()
        {
            return slot;
        }
    }
}