package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.SkillData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class SkillCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private final Map<Skill, Integer> previousLevels = new HashMap<>();
    private final Map<Skill, Integer> previousXp = new HashMap<>();

    @Inject
    public SkillCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
        initializePreviousStats();
    }

    private void initializePreviousStats()
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }
            
            int level = client.getRealSkillLevel(skill);
            int xp = client.getSkillExperience(skill);
            
            previousLevels.put(skill, level);
            previousXp.put(skill, xp);
        }
    }

    public void onStatChanged(StatChanged event)
    {
        if (!config.enableSkillTracking())
        {
            return;
        }

        if (client.getLocalPlayer() == null)
        {
            return;
        }

        Skill skill = event.getSkill();
        if (skill == Skill.OVERALL)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        int currentLevel = client.getRealSkillLevel(skill);
        int currentXp = client.getSkillExperience(skill);
        
        Integer prevLevel = previousLevels.get(skill);
        Integer prevXp = previousXp.get(skill);

        if (prevLevel == null || prevXp == null)
        {
            prevLevel = currentLevel;
            prevXp = currentXp;
        }

        SkillData skillData = new SkillData(playerName, skill.getName(), currentLevel, currentXp);
        skillData.setXpGained(currentXp - prevXp);
        skillData.setLevelsGained(currentLevel - prevLevel);

        // Send updates immediately - no batching
        if (skillData.getLevelsGained() > 0)
        {
            log.info("Level up! {} reached level {} in {} (+{} XP)", 
                playerName, skillData.getLevel(), skillData.getSkillName(), skillData.getXpGained());
            dataManager.queueSkillUpdate(skillData);
        }
        else if (skillData.getXpGained() > 0)
        {
            log.info("XP gained: {} +{} XP in {}", 
                playerName, skillData.getXpGained(), skillData.getSkillName());
            dataManager.queueXpUpdate(skillData);
        }

        previousLevels.put(skill, currentLevel);
        previousXp.put(skill, currentXp);
    }
}