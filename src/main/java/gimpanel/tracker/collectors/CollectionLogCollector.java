package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.CollectionLogData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class CollectionLogCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private final Set<String> collectedItems = new HashSet<>();
    private final Map<String, Integer> collectionProgress = new HashMap<>();
    
    // Collection log notification patterns
    private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
        "New item added to your collection log: (.+)"
    );
    
    private static final Pattern COLLECTION_LOG_DUPLICATE = Pattern.compile(
        "You have a funny feeling like you would have been followed\\.\\.\\."
    );
    
    @Inject
    public CollectionLogCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
        initializeCollectionCategories();
    }
    
    private void initializeCollectionCategories()
    {
        // Initialize collection log categories with estimated totals
        // This would ideally be loaded from external data
        collectionProgress.put("Bosses", 0);
        collectionProgress.put("Raids", 0);
        collectionProgress.put("Clue Scrolls", 0);
        collectionProgress.put("Minigames", 0);
        collectionProgress.put("Other", 0);
        
        log.info("Collection Log collector initialized");
    }
    
    public void onChatMessage(ChatMessage event)
    {
        if (!config.enableCollectionLogTracking())
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
        
        if (event.getType() == ChatMessageType.GAMEMESSAGE)
        {
            String message = event.getMessage();
            
            // Check for collection log notifications
            Matcher matcher = COLLECTION_LOG_PATTERN.matcher(message);
            if (matcher.find())
            {
                String itemName = matcher.group(1);
                handleCollectionLogDrop(playerName, itemName);
            }
            
            // Check for duplicate drops (items that would have been collection log)
            if (COLLECTION_LOG_DUPLICATE.matcher(message).find())
            {
                // This indicates the player got a drop they already have in collection log
                log.debug("Player {} got duplicate collection log item", playerName);
            }
        }
    }
    
    private void handleCollectionLogDrop(String playerName, String itemName)
    {
        if (!collectedItems.contains(itemName))
        {
            collectedItems.add(itemName);
            
            // Categorize the item
            String category = categorizeCollectionLogItem(itemName);
            collectionProgress.merge(category, 1, Integer::sum);
            
            log.info("Collection Log item obtained! {} collected '{}' in category '{}'", 
                playerName, itemName, category);
            
            // Create collection log data
            CollectionLogData logData = new CollectionLogData(
                playerName, itemName, category,
                collectedItems.size(), getTotalCollectionItems()
            );
            logData.setCategoryProgress(new HashMap<>(collectionProgress));
            
            dataManager.queueCollectionLogUpdate(logData);
        }
    }
    
    private String categorizeCollectionLogItem(String itemName)
    {
        String itemLower = itemName.toLowerCase();
        
        // Boss drops
        if (itemLower.contains("pet") || 
            itemLower.contains("dragon") ||
            itemLower.contains("whip") ||
            itemLower.contains("dagger") ||
            itemLower.contains("axe") ||
            itemLower.contains("mace") ||
            itemLower.contains("spear") ||
            itemLower.contains("bow") ||
            itemLower.contains("staff") ||
            itemLower.contains("shield") ||
            itemLower.contains("helm") ||
            itemLower.contains("platebody") ||
            itemLower.contains("platelegs") ||
            itemLower.contains("boots") ||
            itemLower.contains("gloves"))
        {
            return "Bosses";
        }
        
        // Raids
        if (itemLower.contains("twisted") ||
            itemLower.contains("kodai") ||
            itemLower.contains("elder") ||
            itemLower.contains("ancestral") ||
            itemLower.contains("dragon claws") ||
            itemLower.contains("dinhs") ||
            itemLower.contains("dragon hunter"))
        {
            return "Raids";
        }
        
        // Clue scrolls
        if (itemLower.contains("3rd age") ||
            itemLower.contains("gilded") ||
            itemLower.contains("elegant") ||
            itemLower.contains("blessed") ||
            itemLower.contains("ranger") ||
            itemLower.contains("wizard") ||
            itemLower.contains("robin") ||
            itemLower.contains("infinity"))
        {
            return "Clue Scrolls";
        }
        
        // Minigames
        if (itemLower.contains("void") ||
            itemLower.contains("fighter") ||
            itemLower.contains("barbarian") ||
            itemLower.contains("penance") ||
            itemLower.contains("castle wars") ||
            itemLower.contains("pest control"))
        {
            return "Minigames";
        }
        
        return "Other";
    }
    
    private int getTotalCollectionItems()
    {
        // Estimated total collection log items
        // This would be more accurate with real data
        return 1400;
    }
    
    public int getCollectedItemCount()
    {
        return collectedItems.size();
    }
    
    public Map<String, Integer> getCategoryProgress()
    {
        return new HashMap<>(collectionProgress);
    }
}