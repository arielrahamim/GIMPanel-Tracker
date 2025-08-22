package gimpanel.tracker.models;

import lombok.Data;
import net.runelite.api.coords.WorldPoint;

@Data
public class ActivityData
{
    private String playerName;
    private String currentActivity;
    private String location;
    private WorldPoint worldPoint;
    private int worldId;
    private boolean isIdle;
    private long lastActivity;
    private String region;

    public ActivityData()
    {
        this.lastActivity = System.currentTimeMillis();
    }

    public ActivityData(String playerName, String currentActivity, String location)
    {
        this.playerName = playerName;
        this.currentActivity = currentActivity;
        this.location = location;
        this.lastActivity = System.currentTimeMillis();
    }
}