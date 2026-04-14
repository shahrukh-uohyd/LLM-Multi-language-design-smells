// AutomotiveInfotainmentSystem.java
public class AutomotiveInfotainmentSystem {
    static {
        System.loadLibrary("auto_infotainment_native");
    }

    // Dashboard display methods
    private native double readFuelLevel();
    private native double readEngineTemperature();

    // Media center methods
    private native boolean playMp3Stream(String filePath);
    private native void adjustEqualizer(int bass, int treble, int mid);

    // Navigation module methods
    private native String calculateRoute(String origin, String destination);
    private native byte[] renderMapTile(double latitude, double longitude, int zoom);

    // Connectivity suite methods
    private native boolean pairBluetoothDevice(String deviceAddress);
    private native String[] searchPointsOfInterest(String category, double radius);

    private static final AutomotiveInfotainmentSystem INSTANCE = new AutomotiveInfotainmentSystem();
    
    private AutomotiveInfotainmentSystem() {}
    
    public static AutomotiveInfotainmentSystem getInstance() {
        return INSTANCE;
    }
}

class DashboardDisplay {
    private AutomotiveInfotainmentSystem system = AutomotiveInfotainmentSystem.getInstance();

    public void updateDashboard() {
        double fuelLevel = system.readFuelLevel();
        double engineTemp = system.readEngineTemperature();
        
        System.out.println("Fuel Level: " + fuelLevel + "%");
        System.out.println("Engine Temp: " + engineTemp + "°C");
    }
}

class MediaCenter {
    private AutomotiveInfotainmentSystem system = AutomotiveInfotainmentSystem.getInstance();

    public boolean playMusic(String file) {
        return system.playMp3Stream(file);
    }

    public void setEqualizer(int bass, int treble, int mid) {
        system.adjustEqualizer(bass, treble, mid);
    }
}

class NavigationModule {
    private AutomotiveInfotainmentSystem system = AutomotiveInfotainmentSystem.getInstance();

    public String getDirections(String from, String to) {
        return system.calculateRoute(from, to);
    }

    public byte[] getMapImage(double lat, double lon, int zoom) {
        return system.renderMapTile(lat, lon, zoom);
    }
}

class ConnectivitySuite {
    private AutomotiveInfotainmentSystem system = AutomotiveInfotainmentSystem.getInstance();

    public boolean connectToDevice(String address) {
        return system.pairBluetoothDevice(address);
    }

    public String[] findNearby(String type, double range) {
        return system.searchPointsOfInterest(type, range);
    }
}