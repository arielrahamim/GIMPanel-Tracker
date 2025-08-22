package gimpanel.tracker;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Integration test class for GIMPanel Tracker Plugin
 * Tests the overall plugin functionality and integration
 */
public class GIMPanelIntegrationTest
{
    @Test
    public void testPluginIntegration() {
        // Test that the plugin can be loaded and initialized
        GIMPanelTrackerPlugin plugin = new GIMPanelTrackerPlugin();
        assertNotNull("Plugin should be created", plugin);
        
        // Test that the plugin has the correct class name
        assertEquals("Plugin class name should match", 
            "gimpanel.tracker.GIMPanelTrackerPlugin", 
            plugin.getClass().getName());
    }
    
    @Test
    public void testPluginDescriptor() {
        // Test that the plugin has the required descriptor annotation
        GIMPanelTrackerPlugin plugin = new GIMPanelTrackerPlugin();
        assertNotNull("Plugin should have PluginDescriptor annotation",
            plugin.getClass().getAnnotation(net.runelite.client.plugins.PluginDescriptor.class));
    }
    
    @Test
    public void testPluginPackage() {
        // Test that the plugin is in the correct package
        GIMPanelTrackerPlugin plugin = new GIMPanelTrackerPlugin();
        assertEquals("Plugin should be in gimpanel.tracker package",
            "gimpanel.tracker", plugin.getClass().getPackage().getName());
    }
}
