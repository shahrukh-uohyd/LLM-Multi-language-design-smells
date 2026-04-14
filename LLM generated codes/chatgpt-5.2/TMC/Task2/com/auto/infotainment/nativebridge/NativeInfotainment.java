package com.auto.infotainment.nativebridge;

public final class NativeInfotainment {

    static {
        // Loads libinfotainment.so / infotainment.dll
        System.loadLibrary("infotainment");
    }

    private NativeInfotainment() {
        // Prevent instantiation
    }

    /* =======================
       Dashboard Display
       ======================= */

    public static native float readFuelLevel();          // percentage (0.0 – 100.0)
    public static native float readEngineTemperature();  // degrees Celsius


    /* =======================
       Media Center
       ======================= */

    public static native void playMp3Stream(String sourceUri);
    public static native void setEqualizer(int bass, int mid, int treble);


    /* =======================
       Navigation Module
       ======================= */

    public static native byte[] calculateRoute(
            double startLat, double startLon,
            double endLat, double endLon
    );

    public static native byte[] renderMapTile(int zoom, int x, int y);


    /* =======================
       Connectivity Suite
       ======================= */

    public static native boolean pairBluetoothDevice(String deviceAddress);
    public static native String[] searchPointsOfInterest(String keyword);
}
