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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class StashCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    // Track last known stash states
    private final Map<Integer, Item[]> lastStashStates = new HashMap<>();
    
    // STASH unit container IDs (these may need adjustment based on actual RuneLite values)
    private static final int[] STASH_CONTAINER_IDS = {
        // These IDs may need to be updated with actual RuneLite STASH container IDs
        // For now using placeholders - we'll need to detect actual values
    };

    @Inject
    public StashCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.enableStashTracking())
        {
            log.debug("Stash tracking disabled in configuration");
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        int containerId = event.getContainerId();
        
        // Check if this is a STASH unit container
        if (!isStashContainer(containerId))
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
        Item[] previousItems = lastStashStates.get(containerId);

        if (previousItems == null)
        {
            // First time seeing this stash - store state and report
            lastStashStates.put(containerId, Arrays.copyOf(currentItems, currentItems.length));
            reportStashSync(playerName, containerId, currentItems, "initial_sync");
            return;
        }

        // Compare current vs previous state
        if (!Arrays.equals(currentItems, previousItems))
        {
            log.info("STASH unit {} changed for {}", containerId, playerName);
            
            // Determine if items were added or removed
            String action = determineStashAction(previousItems, currentItems);
            
            // Update stored state
            lastStashStates.put(containerId, Arrays.copyOf(currentItems, currentItems.length));
            
            // Report the change
            reportStashSync(playerName, containerId, currentItems, action);
        }
    }

    private boolean isStashContainer(int containerId)
    {
        // For now, we'll use a broad approach and detect STASH containers
        // This may need refinement based on actual RuneLite container IDs
        
        // STASH containers typically have IDs in certain ranges
        // We'll need to identify the actual ranges used by RuneLite
        
        // Placeholder logic - may need adjustment
        return containerId >= 500 && containerId <= 600; // Adjust range as needed
    }

    private String determineStashAction(Item[] previous, Item[] current)
    {
        int prevCount = countNonEmptyItems(previous);
        int currCount = countNonEmptyItems(current);
        
        if (currCount > prevCount)
        {
            return "deposit";
        }
        else if (currCount < prevCount)
        {
            return "withdraw";
        }
        else
        {
            return "reorganize";
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

    private void reportStashSync(String playerName, int stashId, Item[] items, String action)
    {
        try
        {
            // Count total items and calculate value if possible
            int itemCount = countNonEmptyItems(items);
            
            ActivityData activityData = new ActivityData(
                playerName,
                "STASH Unit " + action.toUpperCase(),
                "STASH Unit " + stashId
            );
            
            // Add metadata about the stash operation
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("stashId", stashId);
            metadata.put("action", action);
            metadata.put("itemCount", itemCount);
            metadata.put("timestamp", System.currentTimeMillis());
            
            // Add item details if not too many
            if (itemCount <= 10 && items != null)
            {
                StringBuilder itemList = new StringBuilder();
                for (Item item : items)
                {
                    if (item.getId() > 0 && item.getQuantity() > 0)
                    {
                        if (itemList.length() > 0) itemList.append(", ");
                        itemList.append("Item ").append(item.getId()).append(" x").append(item.getQuantity());
                    }
                }
                metadata.put("items", itemList.toString());
            }
            
            dataManager.queueActivityUpdate(activityData);
            
            log.info("STASH {} reported for {}: {} items in stash {}", 
                action, playerName, itemCount, stashId);
        }
        catch (Exception e)
        {
            log.error("Error reporting STASH sync for {}: {}", playerName, e.getMessage());
        }
    }

    public void syncAllStashUnits(String reason)
    {
        if (!config.enableStashTracking())
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

        log.info("Syncing all STASH units for {} (reason: {})", playerName, reason);

        // Clear previous states to force a resync
        lastStashStates.clear();

        // Trigger sync by checking all possible STASH containers
        // This is a brute-force approach that may need optimization
        for (int containerId = 500; containerId <= 600; containerId++)
        {
            ItemContainer container = client.getItemContainer(containerId);
            if (container != null)
            {
                Item[] items = container.getItems();
                if (items != null && items.length > 0)
                {
                    lastStashStates.put(containerId, Arrays.copyOf(items, items.length));
                    reportStashSync(playerName, containerId, items, "sync_" + reason);
                }
            }
        }
    }
}