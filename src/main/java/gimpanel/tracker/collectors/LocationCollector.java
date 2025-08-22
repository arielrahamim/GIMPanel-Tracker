package gimpanel.tracker.collectors;

import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.managers.DataManager;
import gimpanel.tracker.models.ActivityData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class LocationCollector
{
    private final Client client;
    private final GIMPanelConfig config;
    private final DataManager dataManager;
    
    private WorldPoint lastLocation;
    private String lastActivity;
    private int ticksSinceLastUpdate = 0;
    private static final int UPDATE_FREQUENCY = 1; // Update every tick (~0.6 seconds) like other plugins

    @Inject
    public LocationCollector(Client client, GIMPanelConfig config, DataManager dataManager)
    {
        this.client = client;
        this.config = config;
        this.dataManager = dataManager;
    }

    public void onGameTick(GameTick event)
    {
        if (!config.shareLocation())
        {
            return;
        }

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

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        // Use proper instance handling like other plugins
        LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
        WorldPoint currentLocation = WorldPoint.fromLocalInstance(client, localPoint);
        if (currentLocation == null)
        {
            return;
        }

        // OPTIMIZATION: Use DataManager's differential update system
        String currentActivity = determineActivity();
        
        // Only update if location has changed significantly or activity changed
        if (lastLocation != null && currentLocation.distanceTo(lastLocation) < 1 && 
            lastActivity != null && lastActivity.equals(currentActivity))
        {
            return; // Skip if player hasn't moved much and activity hasn't changed
        }

        // OPTIMIZATION: Use DataManager's optimized location update
        dataManager.updatePlayerLocation(currentLocation, currentActivity);

        log.debug("Location update: {} at {} ({})", playerName, getLocationName(currentLocation), currentActivity);

        lastLocation = currentLocation;
        lastActivity = currentActivity;
    }

    private String determineActivity()
    {
        if (client.getLocalPlayer() == null)
        {
            return "Unknown";
        }

        // Check if player is in combat
        if (client.getLocalPlayer().getInteracting() != null)
        {
            return "In Combat";
        }

        // Check if player is animating (doing an activity)
        if (client.getLocalPlayer().getAnimation() != -1)
        {
            return "Active";
        }

        // Check if player is moving
        if (client.getLocalPlayer().getPoseAnimation() != client.getLocalPlayer().getIdlePoseAnimation())
        {
            return "Moving";
        }

        return "Idle";
    }

    private String getLocationName(WorldPoint location)
    {
        // This is a simplified location naming system
        // In a more complete implementation, you'd want to use region names or area detection
        
        int regionId = location.getRegionID();
        
        // Some common region IDs for major areas
        switch (regionId)
        {
            case 12850: return "Lumbridge";
            case 12597: return "Varrock";
            case 12342: return "Falador";
            case 11828: return "Draynor Village";
            case 12954: return "Al Kharid";
            case 10547: return "Rimmington";
            case 12596: return "Barbarian Village";
            case 13105: return "Edgeville";
            case 12853: return "Port Sarim";
            case 11319: return "Karamja";
            default:
                return "Region " + regionId;
        }
    }
}