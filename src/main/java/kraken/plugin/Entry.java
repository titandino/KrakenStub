package kraken.plugin;

import java.nio.file.Paths;

import kraken.plugin.api.Debug;
import kraken.plugin.api.Kraken;
import kraken.plugin.api.Plugin;
import kraken.plugin.api.PluginContext;
import kraken.stub.ExamplePlugin;
import kraken.stub.Plugins;

public class Entry extends Plugin {

    @Override
    public boolean onLoaded(PluginContext pluginContext) {
        pluginContext.setName("Local Plugins");

        // load example plugin first
        // we use this to test API functionality
        Kraken.loadNewPlugin(ExamplePlugin.class);

        Debug.log("Loading jars from " + Kraken.getPluginDir());
        try {
            Plugins.loadJars(Paths.get(Kraken.getPluginDir()));
            Debug.log("Stub loaded!");
        } catch (Throwable e) {
            Debug.printStackTrace("plugin load", e);
        }
        return true;
    }

}
