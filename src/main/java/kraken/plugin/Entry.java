package kraken.plugin;

import kraken.plugin.api.Client;
import kraken.plugin.api.ConVar;
import kraken.plugin.api.Debug;
import kraken.plugin.api.PluginContext;
import kraken.stub.Plugins;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;

/**
 * This acts as a simple wrapper for plugins, so that everything doesn't
 * have to be static.
 */
public class Entry {

    public static boolean onLoaded(PluginContext pluginContext) {
        pluginContext.setName("Local Plugins");

        Debug.log("Loading jars from " + Client.getPluginDir());
        try {
            Plugins.loadJars(Paths.get(Client.getPluginDir()));
            Debug.log("Stub loaded!");
        } catch (Throwable e) {
            Debug.printStackTrace("Failed to load jars", e);
        }
        return true;
    }

}
