import com.example.plugin.Plugin;
import com.example.plugin.LoggingPlugin;

public class PluginNativeBridge {

    static {
        System.loadLibrary("pluginbridge");
    }

    // Native method that triggers plugin execution
    public native void invokePlugin(Plugin plugin);

    public static void main(String[] args) {

        Plugin plugin = new LoggingPlugin();

        PluginNativeBridge bridge = new PluginNativeBridge();
        bridge.invokePlugin(plugin);
    }
}
