package kraken.stub;

import kraken.plugin.api.ConVar;
import kraken.plugin.api.PluginContext;

public abstract class AbstractPlugin {

    /**
     * Called when the bot is loaded.
     *
     * @return If the plugin will run or not.
     */
    public boolean onLoaded(PluginContext pluginContext) {
        return true;
    }

    /**
     * Called when the client is ticking, and it's time for us
     * to loop again. The client will wait for the amount of milliseconds
     * you return before calling this function again.
     *
     * @return The amount of time to wait before invoking this function again.
     */
    public int onLoop() {
        return 60000;
    }

    /**
     * Called when the plugin's window is being painted.
     */
    public void onPaint() {

    }

    /**
     * Called when the client's 3d overlay is being painted.
     */
    public void onPaintOverlay() {

    }

    /**
     * Called when the value of a convar changes. Has up to 500ms delay.
     */
    public void onConVarChanged(ConVar conv, int oldValue, int newValue) {

    }

    /**
     * Called when a widget is opened or closed. Has up to 500ms delay. A
     * widget object is not accepted because the widget may be destroyed at this point.
     */
    public void onWidgetVisibilityChanged(int id, boolean visible) {

    }

}
