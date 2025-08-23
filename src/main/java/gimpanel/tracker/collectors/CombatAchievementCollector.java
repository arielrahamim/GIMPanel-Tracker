package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.CombatAchievementData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;

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
public class CombatAchievementCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private final Set<String> completedAchievements = new HashSet<>();
    private final Map<String, Integer> tierProgress = new HashMap<>();
    private final Map<Integer, String> varbitToAchievementMap = new HashMap<>();
    
    // Combat achievement notification patterns
    private static final Pattern CA_COMPLETION_PATTERN = Pattern.compile(
        "Congratulations, you've completed a combat task: (.+?)\\."
    );
    
    private static final Pattern CA_TIER_COMPLETE_PATTERN = Pattern.compile(
        "Congratulations, you've completed all (.+?) combat tasks!"
    );
    
    @Inject
    public CombatAchievementCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
        initializeCombatAchievements();
    }
    
    private void initializeCombatAchievements()
    {
        // Initialize tier progress tracking
        for (CombatAchievementData.Tier tier : CombatAchievementData.Tier.values())
        {
            tierProgress.put(tier.getDisplayName(), 0);
        }
        
        // Initialize known combat achievement varbit mappings
        // This is a simplified mapping - would need comprehensive mapping for all CAs
        initializeVarbitMapping();
        
        log.info("Combat Achievement collector initialized");
    }
    
    private void initializeVarbitMapping()
    {
        // Map known combat achievement varbits
        // This would be a comprehensive mapping in practice
        varbitToAchievementMap.put(14002, "First Steps");
        varbitToAchievementMap.put(14003, "Kalphite Rookie");
        varbitToAchievementMap.put(14004, "Giant Mole Rookie");
        // Add more mappings as needed
        
        log.debug("Initialized {} combat achievement varbit mappings", varbitToAchievementMap.size());
    }
    
    public void onChatMessage(ChatMessage event)
    {
        if (!config.enableCombatAchievementTracking())
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
            
            // Check for combat achievement completion
            Matcher completionMatcher = CA_COMPLETION_PATTERN.matcher(message);
            if (completionMatcher.find())
            {
                String achievementName = completionMatcher.group(1);
                handleCombatAchievementCompletion(playerName, achievementName);
            }
            
            // Check for tier completion
            Matcher tierMatcher = CA_TIER_COMPLETE_PATTERN.matcher(message);
            if (tierMatcher.find())
            {
                String tierName = tierMatcher.group(1);
                handleTierCompletion(playerName, tierName);
            }
        }
    }
    
    public void onVarbitChanged(VarbitChanged event)
    {
        if (!config.enableCombatAchievementTracking())
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
        
        int varbitId = event.getVarbitId();
        int value = event.getValue();
        
        String achievementName = varbitToAchievementMap.get(varbitId);
        if (achievementName != null && value > 0)
        {
            handleCombatAchievementVarbit(playerName, achievementName, value);
        }
    }
    
    private void handleCombatAchievementCompletion(String playerName, String achievementName)
    {
        if (!completedAchievements.contains(achievementName))
        {
            completedAchievements.add(achievementName);
            
            // Determine tier and category
            String tier = determineAchievementTier(achievementName);
            String category = determineAchievementCategory(achievementName);
            int points = getAchievementPoints(tier);
            
            // Update tier progress
            tierProgress.merge(tier, 1, Integer::sum);
            
            log.info("Combat Achievement completed! {} completed '{}' ({} - {} points)", 
                playerName, achievementName, tier, points);
            
            // Create combat achievement data
            CombatAchievementData caData = new CombatAchievementData(
                playerName, achievementName, tier, category, true, points
            );
            caData.setTierProgress(new HashMap<>(tierProgress));
            caData.setDescription(getAchievementDescription(achievementName));
            
            dataManager.queueCombatAchievementUpdate(caData);
        }
    }
    
    private void handleCombatAchievementVarbit(String playerName, String achievementName, int value)
    {
        // Handle varbit-based achievement tracking
        // This would be used for achievements that don't have chat notifications
        log.debug("Combat achievement varbit updated: {} for {}", achievementName, playerName);
    }
    
    private void handleTierCompletion(String playerName, String tierName)
    {
        log.info("Combat Achievement tier completed! {} completed all {} tasks", 
            playerName, tierName);
        
        // Could send special tier completion notification
    }
    
    private String determineAchievementTier(String achievementName)
    {
        // Simplified tier determination based on achievement name patterns
        String nameLower = achievementName.toLowerCase();
        
        if (nameLower.contains("rookie") || nameLower.contains("novice"))
        {
            return "Easy";
        }
        else if (nameLower.contains("adept") || nameLower.contains("intermediate"))
        {
            return "Medium";
        }
        else if (nameLower.contains("expert") || nameLower.contains("advanced"))
        {
            return "Hard";
        }
        else if (nameLower.contains("elite") || nameLower.contains("superior"))
        {
            return "Elite";
        }
        else if (nameLower.contains("master") || nameLower.contains("veteran"))
        {
            return "Master";
        }
        else if (nameLower.contains("grandmaster") || nameLower.contains("legendary"))
        {
            return "Grandmaster";
        }
        else
        {
            return "Easy"; // Default assumption
        }
    }
    
    private String determineAchievementCategory(String achievementName)
    {
        String nameLower = achievementName.toLowerCase();
        
        if (nameLower.contains("kalphite"))
        {
            return "Kalphite Queen";
        }
        else if (nameLower.contains("mole"))
        {
            return "Giant Mole";
        }
        else if (nameLower.contains("zulrah"))
        {
            return "Zulrah";
        }
        else if (nameLower.contains("vorkath"))
        {
            return "Vorkath";
        }
        else if (nameLower.contains("hydra"))
        {
            return "Alchemical Hydra";
        }
        else if (nameLower.contains("cox") || nameLower.contains("chambers"))
        {
            return "Chambers of Xeric";
        }
        else if (nameLower.contains("tob") || nameLower.contains("theatre"))
        {
            return "Theatre of Blood";
        }
        else
        {
            return "General";
        }
    }
    
    private int getAchievementPoints(String tier)
    {
        try
        {
            CombatAchievementData.Tier tierEnum = CombatAchievementData.Tier.valueOf(tier.toUpperCase());
            return tierEnum.getPoints();
        }
        catch (IllegalArgumentException e)
        {
            return 1; // Default points
        }
    }
    
    private String getAchievementDescription(String achievementName)
    {
        // Simplified description lookup
        // In practice, this would come from a comprehensive database
        return "Complete the " + achievementName + " combat task";
    }
    
    public int getCompletedAchievementCount()
    {
        return completedAchievements.size();
    }
    
    public Map<String, Integer> getTierProgress()
    {
        return new HashMap<>(tierProgress);
    }
    
    public int getTotalPoints()
    {
        return tierProgress.entrySet().stream()
            .mapToInt(entry -> entry.getValue() * getAchievementPoints(entry.getKey()))
            .sum();
    }
}