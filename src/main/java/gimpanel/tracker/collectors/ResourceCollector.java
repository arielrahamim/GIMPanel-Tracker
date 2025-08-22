package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.GameTick;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class ResourceCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private int lastHealth = -1;
    private int lastMaxHealth = -1;
    private int lastPrayer = -1;
    private int lastMaxPrayer = -1;
    private int lastEnergy = -1;
    private int lastSpecial = -1;
    private int ticksSinceLastUpdate = 0;
    private static final int UPDATE_FREQUENCY = 10; // Update every 10 ticks (~6 seconds)

    @Inject
    public ResourceCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onGameTick(GameTick event)
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        ticksSinceLastUpdate++;
        
        // Only update every UPDATE_FREQUENCY ticks to avoid spam
        if (ticksSinceLastUpdate < UPDATE_FREQUENCY)
        {
            return;
        }

        ticksSinceLastUpdate = 0;

        // Get current resource values
        int currentHealth = client.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);
        int currentMaxHealth = client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
        int currentPrayer = client.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER);
        int currentMaxPrayer = client.getRealSkillLevel(net.runelite.api.Skill.PRAYER);
        int currentEnergy = client.getEnergy();
        int currentSpecial = client.getVar(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10;

        // Check if resources have changed significantly
        boolean hasChanged = 
            currentHealth != lastHealth ||
            currentMaxHealth != lastMaxHealth ||
            currentPrayer != lastPrayer ||
            currentMaxPrayer != lastMaxPrayer ||
            Math.abs(currentEnergy - lastEnergy) > 10 || // Energy changes more frequently
            Math.abs(currentSpecial - lastSpecial) > 10; // Special attack changes

        if (hasChanged)
        {
            // OPTIMIZATION: Use DataManager's optimized resource update
            dataManager.updatePlayerResources(
                currentHealth, currentMaxHealth,
                currentPrayer, currentMaxPrayer,
                currentEnergy, currentSpecial
            );

            log.debug("Resource update: HP {}/{}, Prayer {}/{}, Energy {}, Special {}%", 
                currentHealth, currentMaxHealth,
                currentPrayer, currentMaxPrayer,
                currentEnergy, currentSpecial);

            // Update last values
            lastHealth = currentHealth;
            lastMaxHealth = currentMaxHealth;
            lastPrayer = currentPrayer;
            lastMaxPrayer = currentMaxPrayer;
            lastEnergy = currentEnergy;
            lastSpecial = currentSpecial;
        }
    }
}
