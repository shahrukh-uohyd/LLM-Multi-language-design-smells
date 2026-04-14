public class MultiSensorDevice {

    // The static block ensures the native C/C++ library is loaded into memory 
    // exactly once when the MultiSensorDevice class is first initialized.
    static {
        // "multisensor_hw" will load:
        // - multisensor_hw.dll on Windows
        // - libmultisensor_hw.so on Linux
        // - libmultisensor_hw.dylib on macOS
        System.loadLibrary("multisensor_hw");
    }

    // ---------------------------------------------------------
    // 1. Temperature Sensor Component
    // ---------------------------------------------------------
    public static class TemperatureSensor {
        /**
         * Initializes the temperature sensor hardware.
         * Sets up the necessary I2C/SPI buses or memory registers.
         */
        public native void initHardware();

        /**
         * Reads the current temperature.
         * @return The temperature value (e.g., in Celsius).
         */
        public native double readValue();
    }

    // ---------------------------------------------------------
    // 2. Pressure Sensor Component
    // ---------------------------------------------------------
    public static class PressureSensor {
        /**
         * Initializes the pressure sensor hardware.
         */
        public native void initHardware();

        /**
         * Reads the current atmospheric pressure.
         * @return The pressure value (e.g., in hPa or Pascals).
         */
        public native double readValue();
    }

    // ---------------------------------------------------------
    // 3. Humidity Sensor Component
    // ---------------------------------------------------------
    public static class HumiditySensor {
        /**
         * Initializes the humidity sensor hardware.
         */
        public native void initHardware();

        /**
         * Reads the current relative humidity.
         * @return The humidity percentage (0.0 to 100.0).
         */
        public native double readValue();
    }

    // ---------------------------------------------------------
    // Example Usage
    // ---------------------------------------------------------
    public static void main(String[] args) {
        // Instantiate the sensors
        TemperatureSensor tempSensor = new TemperatureSensor();
        PressureSensor pressureSensor = new PressureSensor();
        HumiditySensor humiditySensor = new HumiditySensor();

        // Initialize hardware
        tempSensor.initHardware();
        pressureSensor.initHardware();
        humiditySensor.initHardware();

        // Read and print values
        System.out.println("Temperature: " + tempSensor.readValue() + " °C");
        System.out.println("Pressure:    " + pressureSensor.readValue() + " hPa");
        System.out.println("Humidity:    " + humiditySensor.readValue() + " %");
    }
}