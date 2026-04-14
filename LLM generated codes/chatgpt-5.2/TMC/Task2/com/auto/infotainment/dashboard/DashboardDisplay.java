package com.auto.infotainment.dashboard;

import com.auto.infotainment.nativebridge.NativeInfotainment;

public class DashboardDisplay {

    public float getFuelLevel() {
        return NativeInfotainment.readFuelLevel();
    }

    public float getEngineTemperature() {
        return NativeInfotainment.readEngineTemperature();
    }

    public void refresh() {
        float fuel = getFuelLevel();
        float temp = getEngineTemperature();

        // Render values on dashboard UI
        System.out.println("Fuel: " + fuel + "%, Engine Temp: " + temp + "°C");
    }
}
