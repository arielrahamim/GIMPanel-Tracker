package gimpanel.tracker.util;

import gimpanel.tracker.models.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true);
            
        // For development: disable SSL verification for ngrok domains
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };
            
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> {
                // Allow all hostnames for development (ngrok domains)
                log.debug("SSL hostname verification bypassed for: {}", hostname);
                return true;
            });
        } catch (Exception e) {
            log.warn("Failed to configure SSL bypass: {}", e.getMessage());
        }
        
        this.httpClient = builder.build();
    }

    public void configure(String baseUrl, String authToken)
    {
        if (baseUrl != null) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        } else {
            this.baseUrl = null;
        }
        this.authToken = authToken;
        log.info("ApiClient configured with URL: {}, Token: {} chars", this.baseUrl, authToken != null ? authToken.length() : 0);
        
        // For debugging: temporarily disable token to test default group
        if (authToken != null && authToken.length() > 50) {
            log.warn("Long token detected - trying without token for debugging");
            this.authToken = ""; // Empty token instead of null to keep configuration valid
        }
    }

    public CompletableFuture<Boolean> updateSkill(SkillData skillData)
    {
        return sendWebhook("LEVEL", skillData.getPlayerName(), createSkillExtra(skillData));
    }

    public CompletableFuture<Boolean> updateXp(SkillData skillData)
    {
        return sendWebhook("XP_GAIN", skillData.getPlayerName(), createXpExtra(skillData));
    }
    
    public CompletableFuture<Boolean> updateEnhancedSkill(gimpanel.tracker.models.EnhancedSkillData skillData)
    {
        return sendWebhook("ENHANCED_LEVEL", skillData.getPlayerName(), createEnhancedSkillExtra(skillData));
    }
    
    public CompletableFuture<Boolean> updateEnhancedXp(gimpanel.tracker.models.EnhancedSkillData skillData)
    {
        return sendWebhook("ENHANCED_XP_GAIN", skillData.getPlayerName(), createEnhancedXpExtra(skillData));
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
    
    public CompletableFuture<Boolean> updateEnhancedQuest(gimpanel.tracker.models.EnhancedQuestData questData)
    {
        return sendWebhook("ENHANCED_QUEST", questData.getPlayerName(), createEnhancedQuestExtra(questData));
    }

    public CompletableFuture<Boolean> syncPlayerData(PlayerData playerData)
    {
        return sendWebhook("PLAYER_SYNC", playerData.getUsername(), createPlayerSyncExtra(playerData));
    }

    public CompletableFuture<Boolean> heartbeat(String playerName)
    {
        return sendWebhook("HEARTBEAT", playerName, createHeartbeatExtra(playerName));
    }
    
    public CompletableFuture<Boolean> updateEnhancedInventory(gimpanel.tracker.models.EnhancedInventoryData inventoryData)
    {
        return sendWebhook("ENHANCED_INVENTORY", inventoryData.getPlayerName(), createEnhancedInventoryExtra(inventoryData));
    }
    
    public CompletableFuture<Boolean> updateAchievementDiary(gimpanel.tracker.models.AchievementDiaryData diaryData)
    {
        return sendWebhook("ACHIEVEMENT_DIARY", diaryData.getPlayerName(), createAchievementDiaryExtra(diaryData));
    }
    
    public CompletableFuture<Boolean> updateCollectionLog(gimpanel.tracker.models.CollectionLogData logData)
    {
        return sendWebhook("COLLECTION_LOG", logData.getPlayerName(), createCollectionLogExtra(logData));
    }
    
    public CompletableFuture<Boolean> updateCombatAchievement(gimpanel.tracker.models.CombatAchievementData caData)
    {
        return sendWebhook("COMBAT_ACHIEVEMENT", caData.getPlayerName(), createCombatAchievementExtra(caData));
    }

    private CompletableFuture<Boolean> sendWebhook(String type, String playerName, Map<String, Object> extra)
    {
        if (baseUrl == null)
        {
            log.warn("ApiClient not properly configured - baseUrl is null");
            return CompletableFuture.completedFuture(false);
        }
        
        if (authToken == null)
        {
            log.warn("ApiClient not properly configured - authToken is null");
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
            
            // Use consolidated webhook endpoint (handles both with and without file upload)
            String webhookUrl;
            if (baseUrl.endsWith("/api/webhook")) {
                webhookUrl = baseUrl; // Use main endpoint
            } else {
                webhookUrl = baseUrl + "/api/webhook";
            }
            
            log.info("Using consolidated webhook endpoint: {}", webhookUrl);
            
            Request request = new Request.Builder()
                .url(webhookUrl)
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                // Add headers that ngrok might need
                .header("ngrok-skip-browser-warning", "true")
                .post(formBody)
                .build();

            log.info("Sending {} webhook for {} to {}", type, playerName, webhookUrl);
            return CompletableFuture.supplyAsync(() -> {
                try (Response response = httpClient.newCall(request).execute())
                {
                    if (response.isSuccessful())
                    {
                        log.info("Successfully sent {} webhook for {} - HTTP {}", type, playerName, response.code());
                        return true;
                    }
                    else
                    {
                        String responseBody = "";
                        String responseHeaders = "";
                        try {
                            responseBody = response.body() != null ? response.body().string() : "No body";
                            responseHeaders = response.headers().toString();
                        } catch (Exception e) {
                            responseBody = "Error reading response: " + e.getMessage();
                        }
                        log.warn("Failed to send {} webhook for {}: HTTP {} - Headers: {} - Body: {}", 
                            type, playerName, response.code(), responseHeaders, responseBody);
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
    
    private Map<String, Object> createEnhancedSkillExtra(gimpanel.tracker.models.EnhancedSkillData skillData)
    {
        return Map.of(
            "skill", skillData.getSkillName(),
            "level", skillData.getLevel(),
            "xp", skillData.getXp(),
            "xpGained", skillData.getXpGained(),
            "totalLevel", skillData.getTotalLevel(),
            "combatLevel", skillData.getCombatLevel(),
            "xpPerHour", skillData.getXpPerHour(),
            "efficiency", skillData.getEfficiency(),
            "rank", skillData.getRank()
        );
    }
    
    private Map<String, Object> createEnhancedXpExtra(gimpanel.tracker.models.EnhancedSkillData skillData)
    {
        return Map.of(
            "skill", skillData.getSkillName(),
            "level", skillData.getLevel(),
            "xp", skillData.getXp(),
            "xpGained", skillData.getXpGained(),
            "totalLevel", skillData.getTotalLevel(),
            "combatLevel", skillData.getCombatLevel(),
            "xpPerHour", skillData.getXpPerHour(),
            "efficiency", skillData.getEfficiency(),
            "timeSinceLastUpdate", skillData.getTimeSinceLastUpdate()
        );
    }
    
    private Map<String, Object> createEnhancedQuestExtra(gimpanel.tracker.models.EnhancedQuestData questData)
    {
        return Map.of(
            "questName", questData.getQuestName(),
            "status", questData.getStatus(),
            "questPoints", questData.getQuestPoints(),
            "progress", questData.getProgress(),
            "requirements", questData.getRequirements() != null ? questData.getRequirements() : Map.of(),
            "rewards", questData.getRewards() != null ? questData.getRewards() : Map.of(),
            "difficulty", questData.getDifficulty() != null ? questData.getDifficulty() : "Unknown",
            "series", questData.getSeries() != null ? questData.getSeries() : "None"
        );
    }
    
    private Map<String, Object> createEnhancedInventoryExtra(gimpanel.tracker.models.EnhancedInventoryData inventoryData)
    {
        return Map.of(
            "containerName", inventoryData.getContainerName(),
            "totalValue", inventoryData.getTotalValue(),
            "totalItems", inventoryData.getTotalItems(),
            "uniqueItems", inventoryData.getUniqueItems(),
            "items", inventoryData.getItems().stream().map(item -> Map.of(
                "itemId", item.getItemId(),
                "itemName", item.getItemName(),
                "quantity", item.getQuantity(),
                "itemValue", item.getItemValue(),
                "totalValue", item.getTotalValue(),
                "category", item.getCategory(),
                "noted", item.isNoted()
            )).collect(java.util.stream.Collectors.toList()),
            "categories", inventoryData.getCategories() != null ? 
                inventoryData.getCategories().entrySet().stream().collect(
                    java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                            "category", entry.getValue().getCategory(),
                            "count", entry.getValue().getCount(),
                            "value", entry.getValue().getValue()
                        )
                    )
                ) : Map.of(),
            "valueRanges", inventoryData.getValueRanges() != null ?
                inventoryData.getValueRanges().entrySet().stream().collect(
                    java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                            "range", entry.getValue().getRange(),
                            "count", entry.getValue().getCount(),
                            "value", entry.getValue().getValue()
                        )
                    )
                ) : Map.of()
        );
    }
    
    private Map<String, Object> createAchievementDiaryExtra(gimpanel.tracker.models.AchievementDiaryData diaryData)
    {
        return Map.of(
            "area", diaryData.getArea(),
            "difficulty", diaryData.getDifficulty(),
            "completed", diaryData.isCompleted(),
            "completedTasks", diaryData.getCompletedTasks(),
            "totalTasks", diaryData.getTotalTasks(),
            "completionPercentage", diaryData.getCompletionPercentage(),
            "rewards", diaryData.getRewards() != null ? diaryData.getRewards() : java.util.List.of(),
            "taskProgress", diaryData.getTaskProgress() != null ? diaryData.getTaskProgress() : Map.of()
        );
    }
    
    private Map<String, Object> createCollectionLogExtra(gimpanel.tracker.models.CollectionLogData logData)
    {
        return Map.of(
            "itemName", logData.getItemName(),
            "category", logData.getCategory(),
            "collectedItems", logData.getCollectedItems(),
            "totalCollectionItems", logData.getTotalCollectionItems(),
            "completionPercentage", logData.getCompletionPercentage(),
            "categoryProgress", logData.getCategoryProgress() != null ? logData.getCategoryProgress() : Map.of()
        );
    }
    
    private Map<String, Object> createCombatAchievementExtra(gimpanel.tracker.models.CombatAchievementData caData)
    {
        return Map.of(
            "achievementName", caData.getAchievementName(),
            "tier", caData.getTier(),
            "category", caData.getCategory(),
            "completed", caData.isCompleted(),
            "points", caData.getPoints(),
            "description", caData.getDescription() != null ? caData.getDescription() : "",
            "tierProgress", caData.getTierProgress() != null ? caData.getTierProgress() : Map.of()
        );
    }
}