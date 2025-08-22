package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
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

        List<InventoryItem> items = new ArrayList<>();
        Item[] containerItems = container.getItems();

        for (int i = 0; i < containerItems.length; i++)
        {
            Item item = containerItems[i];
            if (item.getId() > 0 && item.getQuantity() > 0)
            {
                String itemName = client.getItemDefinition(item.getId()).getName();
                items.add(new InventoryItem(item.getId(), itemName, item.getQuantity(), i));
            }
        }

        InventoryData inventoryData = new InventoryData(playerName, containerName, items);
        
        log.debug("Inventory update: {} items in {} for {}", items.size(), containerName, playerName);
        
        dataManager.queueInventoryUpdate(inventoryData);
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