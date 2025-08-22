package gimpanel.tracker.models;

import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class PlayerData
{
    private String username;
    private String displayName;
    private int totalLevel;
    private int combatLevel;
    private long totalXp;
    private boolean isOnline;
    private String currentWorld;
    private String currentActivity;
    private WorldPoint location;
    private ResourceState resources;
    private long lastSeen;

    @Data
    public static class ResourceState
    {
        private int health;
        private int maxHealth;
        private int prayer;
        private int maxPrayer;
        private int energy;
        private int maxEnergy;
        private int specialAttack;
    }
}