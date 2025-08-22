package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.ActivityData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class GroupStorageCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    // Track last known group storage state
    private Item[] lastGroupStorageState = null;
    
    // Group Ironman shared storage container ID
    private static final int GROUP_STORAGE_CONTAINER_ID = InventoryID.GROUP_STORAGE.getId();

    @Inject
    public GroupStorageCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.enableGroupStorageTracking())
        {
            log.debug("Group storage tracking disabled in configuration");
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        int containerId = event.getContainerId();
        
        // Check if this is the group storage container
        if (containerId != GROUP_STORAGE_CONTAINER_ID)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        ItemContainer container = event.getItemContainer();
        if (container == null)
        {
            return;
        }

        Item[] currentItems = container.getItems();

        if (lastGroupStorageState == null)
        {
            // First time seeing group storage - store state and report
            lastGroupStorageState = Arrays.copyOf(currentItems, currentItems.length);
            reportGroupStorageSync(playerName, currentItems, "initial_sync");
            return;
        }

        // Compare current vs previous state
        if (!Arrays.equals(currentItems, lastGroupStorageState))
        {
            log.info("Group storage changed for {}", playerName);
            
            // Analyze the changes
            GroupStorageChange change = analyzeStorageChange(lastGroupStorageState, currentItems);
            
            // Update stored state
            lastGroupStorageState = Arrays.copyOf(currentItems, currentItems.length);
            
            // Report the change
            reportGroupStorageChange(playerName, change);
        }
    }

    private GroupStorageChange analyzeStorageChange(Item[] previous, Item[] current)
    {
        GroupStorageChange change = new GroupStorageChange();
        
        // Create maps for easier comparison
        Map<Integer, Integer> prevItems = createItemMap(previous);
        Map<Integer, Integer> currItems = createItemMap(current);
        
        // Find items that were added (deposited)
        for (Map.Entry<Integer, Integer> entry : currItems.entrySet())
        {
            int itemId = entry.getKey();
            int currQuantity = entry.getValue();
            int prevQuantity = prevItems.getOrDefault(itemId, 0);
            
            if (currQuantity > prevQuantity)
            {
                change.deposited.add(new ItemChange(itemId, currQuantity - prevQuantity));
            }
        }
        
        // Find items that were removed (withdrawn)
        for (Map.Entry<Integer, Integer> entry : prevItems.entrySet())
        {
            int itemId = entry.getKey();
            int prevQuantity = entry.getValue();
            int currQuantity = currItems.getOrDefault(itemId, 0);
            
            if (prevQuantity > currQuantity)
            {
                change.withdrawn.add(new ItemChange(itemId, prevQuantity - currQuantity));
            }
        }
        
        return change;
    }

    private Map<Integer, Integer> createItemMap(Item[] items)
    {
        Map<Integer, Integer> itemMap = new HashMap<>();
        if (items != null)
        {
            for (Item item : items)
            {
                if (item.getId() > 0 && item.getQuantity() > 0)
                {
                    itemMap.put(item.getId(), item.getQuantity());
                }
            }
        }
        return itemMap;
    }

    private void reportGroupStorageChange(String playerName, GroupStorageChange change)
    {
        try
        {
            // Report deposits
            if (!change.deposited.isEmpty())
            {
                ActivityData activityData = new ActivityData(
                    playerName,
                    "GROUP_STORAGE_DEPOSIT",
                    "Group Storage"
                );
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("action", "deposit");
                metadata.put("itemCount", change.deposited.size());
                metadata.put("items", formatItemChanges(change.deposited));
                metadata.put("timestamp", System.currentTimeMillis());
                
                dataManager.queueActivityUpdate(activityData);
                
                log.info("Group storage deposit by {}: {} items", 
                    playerName, change.deposited.size());
            }
            
            // Report withdrawals
            if (!change.withdrawn.isEmpty())
            {
                ActivityData activityData = new ActivityData(
                    playerName,
                    "GROUP_STORAGE_WITHDRAW",
                    "Group Storage"
                );
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("action", "withdraw");
                metadata.put("itemCount", change.withdrawn.size());
                metadata.put("items", formatItemChanges(change.withdrawn));
                metadata.put("timestamp", System.currentTimeMillis());
                
                dataManager.queueActivityUpdate(activityData);
                
                log.info("Group storage withdrawal by {}: {} items", 
                    playerName, change.withdrawn.size());
            }
        }
        catch (Exception e)
        {
            log.error("Error reporting group storage change for {}: {}", playerName, e.getMessage());
        }
    }

    private void reportGroupStorageSync(String playerName, Item[] items, String reason)
    {
        try
        {
            int itemCount = countNonEmptyItems(items);
            
            ActivityData activityData = new ActivityData(
                playerName,
                "GROUP_STORAGE_SYNC",
                "Group Storage"
            );
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("action", "sync");
            metadata.put("reason", reason);
            metadata.put("itemCount", itemCount);
            metadata.put("timestamp", System.currentTimeMillis());
            
            dataManager.queueActivityUpdate(activityData);
            
            log.info("Group storage sync for {} ({}): {} items", 
                playerName, reason, itemCount);
        }
        catch (Exception e)
        {
            log.error("Error reporting group storage sync for {}: {}", playerName, e.getMessage());
        }
    }

    private int countNonEmptyItems(Item[] items)
    {
        if (items == null) return 0;
        
        int count = 0;
        for (Item item : items)
        {
            if (item.getId() > 0 && item.getQuantity() > 0)
            {
                count++;
            }
        }
        return count;
    }

    private String formatItemChanges(List<ItemChange> changes)
    {
        StringBuilder sb = new StringBuilder();
        for (ItemChange change : changes)
        {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Item ").append(change.itemId).append(" x").append(change.quantity);
        }
        return sb.toString();
    }

    public void syncGroupStorage(String reason)
    {
        if (!config.enableGroupStorageTracking())
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

        log.info("Syncing group storage for {} (reason: {})", playerName, reason);

        ItemContainer container = client.getItemContainer(GROUP_STORAGE_CONTAINER_ID);
        if (container != null)
        {
            Item[] items = container.getItems();
            if (items != null)
            {
                lastGroupStorageState = Arrays.copyOf(items, items.length);
                reportGroupStorageSync(playerName, items, reason);
            }
        }
        else
        {
            log.debug("Group storage container not found for {}", playerName);
        }
    }

    // Helper classes
    private static class GroupStorageChange
    {
        public final List<ItemChange> deposited = new ArrayList<>();
        public final List<ItemChange> withdrawn = new ArrayList<>();
    }

    private static class ItemChange
    {
        public final int itemId;
        public final int quantity;

        public ItemChange(int itemId, int quantity)
        {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }
}