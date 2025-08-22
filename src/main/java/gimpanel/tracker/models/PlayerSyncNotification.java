package gimpanel.tracker.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class PlayerSyncNotification extends WebhookPayload
{
    public PlayerSyncNotification(PlayerData player)
    {
        super("PLAYER_SYNC", player.getUsername());
        
        // Extended data for player sync
        Map<String, Object> extra = new HashMap<>();
        extra.put("totalLevel", player.getTotalLevel());
        extra.put("combatLevel", player.getCombatLevel());
        extra.put("totalXp", player.getTotalXp());
        extra.put("isOnline", player.isOnline());
        extra.put("currentWorld", player.getCurrentWorld());
        extra.put("currentActivity", player.getCurrentActivity());
        extra.put("lastSeen", player.getLastSeen());
        
        if (player.getLocation() != null)
        {
            Map<String, Object> location = new HashMap<>();
            location.put("x", player.getLocation().getX());
            location.put("y", player.getLocation().getY());
            location.put("plane", player.getLocation().getPlane());
            extra.put("location", location);
        }
        
        if (player.getResources() != null)
        {
            Map<String, Object> resources = new HashMap<>();
            resources.put("health", player.getResources().getHealth());
            resources.put("maxHealth", player.getResources().getMaxHealth());
            resources.put("prayer", player.getResources().getPrayer());
            resources.put("maxPrayer", player.getResources().getMaxPrayer());
            resources.put("energy", player.getResources().getEnergy());
            resources.put("specialAttack", player.getResources().getSpecialAttack());
            extra.put("resources", resources);
        }
        
        setExtra(extra);

        // Simple text format (this is mainly for backend processing, not Discord)
        setText(String.format("Player sync: %s (Total Level: %d, Combat: %d)", 
            player.getUsername(), player.getTotalLevel(), player.getCombatLevel()));
    }
}