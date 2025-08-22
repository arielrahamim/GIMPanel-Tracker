package gimpanel.tracker.managers;

import gimpanel.tracker.collectors.QuestCollector;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class StateTracker
{
    private final Client client;
    private final QuestCollector questCollector;
    
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private GameState previousGameState;
    private boolean hasLoggedIn = false;
    private int ticksSinceLogin = 0;
    private static final int INITIALIZATION_DELAY_TICKS = 10; // Wait 10 ticks after login before full initialization

    @Inject
    public StateTracker(Client client, QuestCollector questCollector)
    {
        this.client = client;
        this.questCollector = questCollector;
    }

    public void initialize()
    {
        log.info("StateTracker initialized");
        isInitialized.set(true);
        previousGameState = client.getGameState();
    }

    public void shutdown()
    {
        log.info("StateTracker shutdown");
        isInitialized.set(false);
        hasLoggedIn = false;
        ticksSinceLogin = 0;
    }

    public void onGameStateChanged(GameStateChanged event)
    {
        if (!isInitialized.get())
        {
            return;
        }

        GameState newState = event.getGameState();
        GameState oldState = previousGameState;
        
        log.debug("Game state changed from {} to {}", oldState, newState);

        switch (newState)
        {
            case LOGGED_IN:
                handleLogin();
                break;
            case LOGIN_SCREEN:
            case HOPPING:
                handleLogout();
                break;
            case LOADING:
                log.debug("Loading new area...");
                break;
            default:
                break;
        }

        previousGameState = newState;
    }

    public void onGameTick(GameTick event)
    {
        if (!isInitialized.get())
        {
            return;
        }

        if (hasLoggedIn && ticksSinceLogin < INITIALIZATION_DELAY_TICKS)
        {
            ticksSinceLogin++;
            
            if (ticksSinceLogin == INITIALIZATION_DELAY_TICKS)
            {
                performPostLoginInitialization();
            }
        }
    }

    private void handleLogin()
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        log.info("Player logged in: {}", playerName);
        hasLoggedIn = true;
        ticksSinceLogin = 0;
    }

    private void handleLogout()
    {
        if (hasLoggedIn)
        {
            log.info("Player logged out");
            hasLoggedIn = false;
            ticksSinceLogin = 0;
        }
    }

    private void performPostLoginInitialization()
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        if (playerName == null)
        {
            return;
        }

        log.info("Performing post-login initialization for {}", playerName);

        try
        {
            // Refresh quest states after login to ensure we have current data
            questCollector.refreshAllQuests();
            
            log.info("Post-login initialization complete for {}", playerName);
        }
        catch (Exception e)
        {
            log.error("Error during post-login initialization: {}", e.getMessage());
        }
    }

    public boolean isPlayerLoggedIn()
    {
        return hasLoggedIn && client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
    }

    public boolean isInitializationComplete()
    {
        return hasLoggedIn && ticksSinceLogin >= INITIALIZATION_DELAY_TICKS;
    }

    public String getCurrentPlayerName()
    {
        if (!isPlayerLoggedIn())
        {
            return null;
        }

        return client.getLocalPlayer().getName();
    }

    public GameState getCurrentGameState()
    {
        return client.getGameState();
    }
}