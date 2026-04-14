package com.hardware.sensors;

/**
 * Central controller for managing all hardware sensors.
 * Handles initialization and coordinated data reads.
 */
public class SensorController {

    private final TemperatureSensor temperatureSensor;
    private final PressureSensor pressureSensor;
    private final HumiditySensor humiditySensor;

    public SensorController() {
        this.temperatureSensor = new TemperatureSensor();
        this.pressureSensor    = new PressureSensor();
        this.humiditySensor    = new HumiditySensor();
    }

    /**
     * Initializes all sensors in sequence.
     * Should be called once at application startup.
     */
    public void initializeAll() {
        temperatureSensor.initialize();
        pressureSensor.initialize();
        humiditySensor.initialize();
        System.out.println("All sensors initialized successfully.");
    }

    /**
     * Reads and prints the current values from all sensors.
     */
    public void readAll() {
        double temperature = temperatureSensor.readValue();
        double pressure    = pressureSensor.readValue();
        double humidity    = humiditySensor.readValue();

        System.out.printf("Temperature : %.2f °C%n", temperature);
        System.out.printf("Pressure    : %.2f Pa%n",  pressure);
        System.out.printf("Humidity    : %.2f %%%n",  humidity);
    }

    public static void main(String[] args) {
        SensorController controller = new SensorController();
        controller.initializeAll();
        controller.readAll();
    }
}