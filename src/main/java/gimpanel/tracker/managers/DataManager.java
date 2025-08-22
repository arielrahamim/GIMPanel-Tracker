package gimpanel.tracker.managers;

import gimpanel.tracker.collectors.InventoryCollector;
import gimpanel.tracker.config.GIMPanelConfig;
import gimpanel.tracker.models.*;
import gimpanel.tracker.util.ApiClient;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.GameState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

@Slf4j
@Singleton
public class DataManager
{
    private final Client client;
    private final GIMPanelConfig config;
    private final ApiClient apiClient;
    
    private final BlockingQueue<SkillData> skillQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<SkillData> xpQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<DropData> dropQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ActivityData> activityQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<QuestData> questQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<InventoryCollector.InventoryData> inventoryQueue = new LinkedBlockingQueue<>();
    
    private volatile ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private volatile ScheduledFuture<?> periodicSyncTask;
    private volatile ScheduledFuture<?> heartbeatTask;
    
    // OPTIMIZATION: Add state tracking for differential updates
    private PlayerData lastPlayerData;
    private final Map<String, Object> lastStates = new ConcurrentHashMap<>();
    private int skipNextNAttempts = 0;
    private static final int SECONDS_BETWEEN_UPLOADS = 1; // Match group-ironmen-tracker frequency
    private static final int SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES = 60;

    @Inject
    public DataManager(Client client, GIMPanelConfig config, ApiClient apiClient)
    {
        this.client = client;
        this.config = config;
        this.apiClient = apiClient;
    }

    public void initialize()
    {
        if (isRunning.get())
        {
            return;
        }

        log.info("Initializing DataManager...");
        
        // Create new scheduler with optimized thread pool
        scheduler = Executors.newScheduledThreadPool(2); // Reduced from 3 to 2
        
        apiClient.configure(config.gimpanelUrl(), config.authToken());
        
        isRunning.set(true);
        
        // Start background workers
        startSkillProcessor();
        startXpProcessor();
        startDropProcessor();
        startActivityProcessor();
        startQuestProcessor();
        startInventoryProcessor();
        
        // OPTIMIZATION: Use more frequent updates for real-time data
        startPeriodicSync();
        startHeartbeat();
        
        log.info("DataManager initialized successfully");
    }

    public void shutdown()
    {
        log.info("Shutting down DataManager...");
        
        isRunning.set(false);
        
        if (periodicSyncTask != null)
        {
            periodicSyncTask.cancel(true);
        }
        
        if (heartbeatTask != null)
        {
            heartbeatTask.cancel(true);
        }
        
        if (scheduler != null)
        {
            scheduler.shutdown();
            try
            {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                {
                    scheduler.shutdownNow();
                }
            }
            catch (InterruptedException e)
            {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        
        log.info("DataManager shutdown complete");
    }

    public void queueSkillUpdate(SkillData skillData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        skillQueue.offer(skillData);
    }

    public void queueXpUpdate(SkillData skillData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        xpQueue.offer(skillData);
    }

    public void queueDropUpdate(DropData dropData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        dropQueue.offer(dropData);
    }

    public void queueActivityUpdate(ActivityData activityData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        activityQueue.offer(activityData);
    }

    public void queueQuestUpdate(QuestData questData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        questQueue.offer(questData);
    }

    public void queueInventoryUpdate(InventoryCollector.InventoryData inventoryData)
    {
        if (!isRunning.get())
        {
            return;
        }
        
        inventoryQueue.offer(inventoryData);
    }

    private void startSkillProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start skill processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    SkillData skillData = skillQueue.poll(1, TimeUnit.SECONDS);
                    if (skillData != null)
                    {
                        apiClient.updateSkill(skillData).exceptionally(throwable -> {
                            log.warn("Failed to send skill update: {}", throwable.getMessage());
                            return false;
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing skill update: {}", e.getMessage());
                }
            }
        });
    }

    private void startXpProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start XP processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    SkillData skillData = xpQueue.poll(1, TimeUnit.SECONDS);
                    if (skillData != null)
                    {
                        apiClient.updateXp(skillData).exceptionally(throwable -> {
                            log.warn("Failed to send XP update: {}", throwable.getMessage());
                            return false;
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing XP update: {}", e.getMessage());
                }
            }
        });
    }

    private void startDropProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start drop processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    DropData dropData = dropQueue.poll(1, TimeUnit.SECONDS);
                    if (dropData != null)
                    {
                        apiClient.updateDrop(dropData).exceptionally(throwable -> {
                            log.warn("Failed to send drop report: {}", throwable.getMessage());
                            return false;
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing drop update: {}", e.getMessage());
                }
            }
        });
    }

    private void startActivityProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start activity processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    ActivityData activityData = activityQueue.poll(1, TimeUnit.SECONDS);
                    if (activityData != null)
                    {
                        apiClient.updateActivity(activityData).exceptionally(throwable -> {
                            log.warn("Failed to send activity update: {}", throwable.getMessage());
                            return false;
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing activity update: {}", e.getMessage());
                }
            }
        });
    }

    private void startQuestProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start quest processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    QuestData questData = questQueue.poll(1, TimeUnit.SECONDS);
                    if (questData != null)
                    {
                        apiClient.updateQuest(questData).exceptionally(throwable -> {
                            log.warn("Failed to send quest update: {}", throwable.getMessage());
                            return false;
                        });
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing quest update: {}", e.getMessage());
                }
            }
        });
    }

    private void startInventoryProcessor()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start inventory processor - scheduler is not available");
            return;
        }
        
        scheduler.submit(() -> {
            while (isRunning.get())
            {
                try
                {
                    InventoryCollector.InventoryData inventoryData = inventoryQueue.poll(1, TimeUnit.SECONDS);
                    if (inventoryData != null)
                    {
                        // For now, just log inventory updates since the API endpoint may not exist yet
                        log.debug("Inventory update processed for {}: {} items in {}", 
                            inventoryData.getPlayerName(), 
                            inventoryData.getItems().size(), 
                            inventoryData.getContainerName());
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                catch (Exception e)
                {
                    log.error("Error processing inventory update: {}", e.getMessage());
                }
            }
        });
    }

    private void startPeriodicSync()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start periodic sync - scheduler is not available");
            return;
        }
        
        int intervalSeconds = Math.max(5, config.updateInterval()); // Minimum 5 seconds
        
        periodicSyncTask = scheduler.scheduleAtFixedRate(() -> {
            try
            {
                if (client.getLocalPlayer() == null || client.getGameState() != GameState.LOGGED_IN)
                {
                    return;
                }

                // OPTIMIZATION: Skip if too many failed attempts
                if (skipNextNAttempts-- > 0) return;

                String playerName = client.getLocalPlayer().getName();
                if (playerName == null)
                {
                    return;
                }

                // OPTIMIZATION: Create differential player data
                PlayerData currentPlayerData = createCurrentPlayerData();
                if (currentPlayerData != null && hasSignificantChanges(currentPlayerData))
                {
                    apiClient.syncPlayerData(currentPlayerData).exceptionally(throwable -> {
                        log.warn("Failed to sync player data: {}", throwable.getMessage());
                        skipNextNAttempts = 10; // Skip next 10 attempts on failure
                        return false;
                    });
                    
                    lastPlayerData = currentPlayerData;
                }
            }
            catch (Exception e)
            {
                log.error("Error during periodic sync: {}", e.getMessage());
                skipNextNAttempts = 5; // Skip next 5 attempts on error
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void startHeartbeat()
    {
        if (scheduler == null || scheduler.isShutdown())
        {
            log.warn("Cannot start heartbeat - scheduler is not available");
            return;
        }
        
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            try
            {
                if (client.getLocalPlayer() == null)
                {
                    return;
                }

                String playerName = client.getLocalPlayer().getName();
                if (playerName != null)
                {
                    apiClient.heartbeat(playerName).exceptionally(throwable -> {
                        log.debug("Heartbeat failed: {}", throwable.getMessage());
                        return false;
                    });
                }
            }
            catch (Exception e)
            {
                log.error("Error during heartbeat: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // Heartbeat every 30 seconds
    }

    private PlayerData createCurrentPlayerData()
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return null;
        }

        PlayerData playerData = new PlayerData();
        playerData.setUsername(playerName);
        playerData.setDisplayName(playerName);
        playerData.setTotalLevel(client.getTotalLevel());
        playerData.setCombatLevel(client.getLocalPlayer().getCombatLevel());
        
        // Calculate total XP
        long totalXp = 0;
        for (int i = 0; i < 23; i++) // All skills except overall
        {
            totalXp += client.getSkillExperience(net.runelite.api.Skill.values()[i]);
        }
        playerData.setTotalXp(totalXp);
        
        playerData.setOnline(true);
        playerData.setCurrentWorld(String.valueOf(client.getWorld()));
        
        // Fix location handling with proper instance support
        WorldPoint location = client.getLocalPlayer().getWorldLocation();
        if (location != null) {
            // Handle instances properly like other plugins
            LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
            location = WorldPoint.fromLocalInstance(client, localPoint);
        }
        playerData.setLocation(location);
        playerData.setLastSeen(System.currentTimeMillis());

        // Set resource state
        PlayerData.ResourceState resources = new PlayerData.ResourceState();
        resources.setHealth(client.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS));
        resources.setMaxHealth(client.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS));
        resources.setPrayer(client.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER));
        resources.setMaxPrayer(client.getRealSkillLevel(net.runelite.api.Skill.PRAYER));
        resources.setEnergy(client.getEnergy());
        resources.setMaxEnergy(100);
        resources.setSpecialAttack(client.getVar(net.runelite.api.VarPlayer.SPECIAL_ATTACK_PERCENT) / 10);
        playerData.setResources(resources);

        return playerData;
    }

    // OPTIMIZATION: Add differential update checking
    private boolean hasSignificantChanges(PlayerData currentData)
    {
        if (lastPlayerData == null) return true;
        
        // Check for significant changes that warrant an update
        return !Objects.equals(currentData.getLocation(), lastPlayerData.getLocation()) ||
               currentData.getTotalLevel() != lastPlayerData.getTotalLevel() ||
               currentData.getCombatLevel() != lastPlayerData.getCombatLevel() ||
               Math.abs(currentData.getTotalXp() - lastPlayerData.getTotalXp()) > 1000 ||
               !Objects.equals(currentData.getCurrentWorld(), lastPlayerData.getCurrentWorld()) ||
               !Objects.equals(currentData.getCurrentActivity(), lastPlayerData.getCurrentActivity());
    }

    // OPTIMIZATION: Add state-based update methods similar to group-ironmen-tracker
    public void updatePlayerLocation(WorldPoint location, String activity)
    {
        if (!isRunning.get()) return;
        
        String stateKey = "location_" + location.getX() + "_" + location.getY() + "_" + location.getPlane();
        if (!lastStates.containsKey(stateKey))
        {
            lastStates.put(stateKey, System.currentTimeMillis());
            
            ActivityData activityData = new ActivityData(
                client.getLocalPlayer().getName(), 
                activity, 
                getLocationName(location)
            );
            activityData.setWorldPoint(location);
            activityData.setWorldId(client.getWorld());
            
            queueActivityUpdate(activityData);
        }
    }

    public void updatePlayerResources(int health, int maxHealth, int prayer, int maxPrayer, int energy, int special)
    {
        if (!isRunning.get()) return;
        
        String stateKey = "resources_" + health + "_" + prayer + "_" + energy + "_" + special;
        if (!lastStates.containsKey(stateKey))
        {
            lastStates.put(stateKey, System.currentTimeMillis());
            
            // Create resource update activity
            ActivityData activityData = new ActivityData(
                client.getLocalPlayer().getName(),
                "Resource Update",
                "Health: " + health + "/" + maxHealth + ", Prayer: " + prayer + "/" + maxPrayer
            );
            
            queueActivityUpdate(activityData);
        }
    }

    private String getLocationName(WorldPoint location)
    {
        // Simplified location naming - can be expanded
        int regionId = location.getRegionID();
        switch (regionId)
        {
            case 12850: return "Lumbridge";
            case 12597: return "Varrock";
            case 12342: return "Falador";
            case 11828: return "Draynor Village";
            case 12954: return "Al Kharid";
            default: return "Region " + regionId;
        }
    }
}