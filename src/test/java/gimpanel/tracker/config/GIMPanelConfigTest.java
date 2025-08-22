package gimpanel.tracker.config;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test class for GIMPanelConfig
 */
public class GIMPanelConfigTest
{
    private TestGIMPanelConfig config;
    
    @Before
    public void setUp() {
        config = new TestGIMPanelConfig();
    }
    
    @Test
    public void testDefaultValues() {
        assertEquals("Default URL should be empty", "", config.gimpanelUrl());
        assertEquals("Default auth token should be empty", "", config.authToken());
        assertFalse("Default share inventory should be false", config.shareInventory());
        assertTrue("Default share location should be true", config.shareLocation());
        assertTrue("Default share resources should be true", config.shareResources());
        assertEquals("Default update interval should be 30", 30, config.updateInterval());
        assertEquals("Default location update frequency should be 1", 1, config.locationUpdateFrequency());
        assertEquals("Default resource update frequency should be 10", 10, config.resourceUpdateFrequency());
        assertTrue("Default drop tracking should be true", config.enableDropTracking());
        assertTrue("Default skill tracking should be true", config.enableSkillTracking());
        assertTrue("Default quest tracking should be true", config.enableQuestTracking());
        assertTrue("Default differential updates should be true", config.enableDifferentialUpdates());
        assertTrue("Default heartbeat should be true", config.enableHeartbeat());
        assertEquals("Default heartbeat interval should be 30", 30, config.heartbeatInterval());
    }
    
    @Test
    public void testConfigRangeValidation() {
        // Test that range annotations are properly set
        assertTrue("Update interval should be at least 5", config.updateInterval() >= 5);
        assertTrue("Update interval should be at most 300", config.updateInterval() <= 300);
        assertTrue("Location update frequency should be at least 1", config.locationUpdateFrequency() >= 1);
        assertTrue("Location update frequency should be at most 50", config.locationUpdateFrequency() <= 50);
        assertTrue("Resource update frequency should be at least 5", config.resourceUpdateFrequency() >= 5);
        assertTrue("Resource update frequency should be at most 100", config.resourceUpdateFrequency() <= 100);
        assertTrue("Heartbeat interval should be at least 15", config.heartbeatInterval() >= 15);
        assertTrue("Heartbeat interval should be at most 300", config.heartbeatInterval() <= 300);
    }
    
    /**
     * Test implementation of GIMPanelConfig for testing
     */
    private static class TestGIMPanelConfig implements GIMPanelConfig {
        // This class provides default implementations for testing
    }
}
