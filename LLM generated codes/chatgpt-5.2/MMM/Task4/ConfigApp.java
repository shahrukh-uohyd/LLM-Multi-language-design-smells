import java.util.Arrays;

public class ConfigApp {

    public static void main(String[] args) {

        String config = "mode=2;retry=5;timeout=300";
        String[] keys = { "mode", "retry", "timeout" };

        int[] values =
            NativeConfigParser.parseConfig(config, keys);

        System.out.println("Config string: " + config);
        System.out.println("Keys: " + Arrays.toString(keys));
        System.out.println("Parsed values: " + Arrays.toString(values));
    }
}
