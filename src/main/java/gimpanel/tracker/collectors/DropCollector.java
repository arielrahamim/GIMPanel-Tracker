package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.DropData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class DropCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    // Pattern to match loot messages in chat
    private static final Pattern LOOT_PATTERN = Pattern.compile(".*received.*?(\\d+)\\s*x\\s*([^.]+).*", Pattern.CASE_INSENSITIVE);

    @Inject
    public DropCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onChatMessage(ChatMessage event)
    {
        if (!config.enableDropTracking())
        {
            return;
        }

        // Only process certain types of chat messages that might contain loot info
        if (event.getType() != ChatMessageType.GAMEMESSAGE && 
            event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : "Unknown";
        if (playerName == null)
        {
            return;
        }

        String message = event.getMessage();
        if (message == null)
        {
            return;
        }

        parseLootFromChatMessage(playerName, message);
    }

    private void parseLootFromChatMessage(String playerName, String message)
    {
        // This is a basic implementation - in production you might want more sophisticated parsing
        // or use inventory change detection instead of chat parsing
        
        if (message.toLowerCase().contains("you receive") || 
            message.toLowerCase().contains("received"))
        {
            Matcher matcher = LOOT_PATTERN.matcher(message);
            if (matcher.find())
            {
                try
                {
                    int quantity = Integer.parseInt(matcher.group(1));
                    String itemName = matcher.group(2).trim();
                    
                    // Try to find item by name (this is approximate)
                    processLootItem(playerName, itemName, quantity, "Unknown Source");
                }
                catch (NumberFormatException e)
                {
                    log.debug("Could not parse quantity from message: {}", message);
                }
            }
        }
    }

    private void processLootItem(String playerName, String itemName, int quantity, String source)
    {
        // Since we can't easily get item ID from name, we'll use a default ID
        // In production, you'd want a proper item name -> ID mapping
        int itemId = -1; // Unknown item ID
        long itemValue = 0; // We can't determine value without proper item data

        DropData dropData = new DropData(playerName, itemName, itemId, quantity, source);
        dropData.setValue(itemValue);
        dropData.setRarity(DropData.Rarity.COMMON.getValue()); // Default to common since we can't calculate
        
        String location = "Unknown";
        if (client.getLocalPlayer() != null) {
            // Use proper instance handling like other plugins
            LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
            WorldPoint worldLocation = WorldPoint.fromLocalInstance(client, localPoint);
            if (worldLocation != null) {
                location = worldLocation.toString();
            }
        }
        dropData.setLocation(location);

        log.debug("Drop detected from chat: {} x{} from {} for {}", 
            itemName, quantity, source, playerName);

        dataManager.queueDropUpdate(dropData);
    }
}