package kraken.plugin;

import kraken.plugin.api.*;
import kraken.stub.ExamplePluginEntry;
import kraken.stub.Plugins;

import java.nio.file.Paths;

/**
 * This acts as a simple wrapper for plugins, so that everything doesn't
 * have to be static.
 */
public class Entry {

    public static boolean onLoaded(PluginContext pluginContext) {
        pluginContext.setName("Local Plugins");

        // load example plugin first
        // we use this to test API functionality
        Kraken.loadNewPlugin(ExamplePluginEntry.class);

        Debug.log("Loading jars from " + Kraken.getPluginDir());
        try {
            Plugins.loadJars(Paths.get(Kraken.getPluginDir()));
            Debug.log("Stub loaded!");
        } catch (Throwable e) {
            Debug.printStackTrace("Failed to load jars", e);
        }
        return true;
    }

}
