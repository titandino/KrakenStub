package kraken.plugin;

import kraken.plugin.api.*;
import kraken.stub.ExamplePlugin;
import kraken.stub.Plugins;

import java.nio.file.Paths;
import java.util.List;

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
