package gimpanel.tracker.util;

import gimpanel.tracker.models.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class ApiClient
{
    private final OkHttpClient httpClient;
    private String baseUrl;
    private String authToken;
    private static final String USER_AGENT = "GIMPanelTracker/1.0.0";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Inject
    public ApiClient()
    {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public void configure(String baseUrl, String authToken)
    {
        if (baseUrl != null) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        } else {
            this.baseUrl = null;
        }
        this.authToken = authToken;
        log.info("ApiClient configured with URL: {}", this.baseUrl);
    }

    public CompletableFuture<Boolean> updateSkill(SkillData skillData)
    {
        return sendWebhook("LEVEL", skillData.getPlayerName(), createSkillExtra(skillData));
    }

    public CompletableFuture<Boolean> updateXp(SkillData skillData)
    {
        return sendWebhook("XP_GAIN", skillData.getPlayerName(), createXpExtra(skillData));
    }

    public CompletableFuture<Boolean> updateDrop(DropData dropData)
    {
        return sendWebhook("LOOT", dropData.getPlayerName(), createDropExtra(dropData));
    }

    public CompletableFuture<Boolean> updateActivity(ActivityData activityData)
    {
        return sendWebhook("PLAYER_SYNC", activityData.getPlayerName(), createActivityExtra(activityData));
    }

    public CompletableFuture<Boolean> updateQuest(QuestData questData)
    {
        return sendWebhook("QUEST", questData.getPlayerName(), createQuestExtra(questData));
    }

    public CompletableFuture<Boolean> syncPlayerData(PlayerData playerData)
    {
        return sendWebhook("PLAYER_SYNC", playerData.getUsername(), createPlayerSyncExtra(playerData));
    }

    public CompletableFuture<Boolean> heartbeat(String playerName)
    {
        return sendWebhook("HEARTBEAT", playerName, createHeartbeatExtra(playerName));
    }

    private CompletableFuture<Boolean> sendWebhook(String type, String playerName, Map<String, Object> extra)
    {
        if (baseUrl == null || authToken == null)
        {
            log.warn("ApiClient not properly configured");
            return CompletableFuture.completedFuture(false);
        }

        try
        {
            WebhookPayload payload = new WebhookPayload(type, playerName);
            payload.setExtra(extra);
            payload.setSource("gimpanel-tracker");

            String json = new com.google.gson.Gson().toJson(payload);
            
            // Create form data with payload_json field (backend expects this format)
            RequestBody formBody = new FormBody.Builder()
                .add("payload_json", json)
                .build();
            
            Request request = new Request.Builder()
                .url(baseUrl + "/api/webhook?token=" + authToken)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute())
                {
                    if (response.isSuccessful())
                    {
                        log.debug("Successfully sent {} webhook for {}", type, playerName);
                        return true;
                    }
                    else
                    {
                        log.warn("Failed to send {} webhook for {}: HTTP {}", type, playerName, response.code());
                        return false;
                    }
                }
                catch (Exception e)
                {
                    log.error("Error sending {} webhook for {}: {}", type, playerName, e.getMessage());
                    return false;
                }
            });
        }
        catch (Exception e)
        {
            log.error("Error creating webhook payload for {}: {}", type, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    private Map<String, Object> createSkillExtra(SkillData skillData)
    {
        return Map.of(
            "skill", skillData.getSkillName(),
            "level", skillData.getLevel(),
            "xp", skillData.getXp(),
            "xpGained", skillData.getXpGained()
            // Note: totalLevel and totalXp are not available in SkillData model
        );
    }

    private Map<String, Object> createXpExtra(SkillData skillData)
    {
        return Map.of(
            "skill", skillData.getSkillName(),
            "level", skillData.getLevel(),
            "xp", skillData.getXp(),
            "xpGained", skillData.getXpGained()
        );
    }

    private Map<String, Object> createDropExtra(DropData dropData)
    {
        return Map.of(
            "itemName", dropData.getItemName(),
            "itemId", dropData.getItemId(),
            "quantity", dropData.getQuantity(),
            "source", dropData.getSource(),
            "rarity", dropData.getRarity(),
            "value", dropData.getValue(),
            "location", dropData.getLocation()
            // Note: items array is not available in DropData model
        );
    }

    private Map<String, Object> createActivityExtra(ActivityData activityData)
    {
        return Map.of(
            "currentActivity", activityData.getCurrentActivity(), // Use correct method name
            "location", activityData.getLocation(),
            "worldId", activityData.getWorldId(),
            "region", activityData.getRegion()
        );
    }

    private Map<String, Object> createQuestExtra(QuestData questData)
    {
        return Map.of(
            "questName", questData.getQuestName(),
            "status", questData.getStatus()
        );
    }

    private Map<String, Object> createPlayerSyncExtra(PlayerData playerData)
    {
        return Map.of(
            "totalLevel", playerData.getTotalLevel(),
            "combatLevel", playerData.getCombatLevel(),
            "totalXp", playerData.getTotalXp(),
            "isOnline", playerData.isOnline(),
            "currentWorld", playerData.getCurrentWorld(),
            "currentActivity", playerData.getCurrentActivity(),
            "location", playerData.getLocation() != null ? 
                Map.of("x", playerData.getLocation().getX(), 
                       "y", playerData.getLocation().getY(), 
                       "plane", playerData.getLocation().getPlane()) : null,
            "resources", playerData.getResources() != null ? 
                Map.of("health", playerData.getResources().getHealth(),
                       "maxHealth", playerData.getResources().getMaxHealth(),
                       "prayer", playerData.getResources().getPrayer(),
                       "maxPrayer", playerData.getResources().getMaxPrayer(),
                       "energy", playerData.getResources().getEnergy(),
                       "specialAttack", playerData.getResources().getSpecialAttack()) : null
        );
    }

    private Map<String, Object> createHeartbeatExtra(String playerName)
    {
        return Map.of(
            "timestamp", System.currentTimeMillis(),
            "status", "online"
        );
    }
}