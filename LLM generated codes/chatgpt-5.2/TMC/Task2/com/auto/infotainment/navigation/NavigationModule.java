package com.auto.infotainment.navigation;

import com.auto.infotainment.nativebridge.NativeInfotainment;

public class NavigationModule {

    public byte[] calculateRoute(
            double startLat, double startLon,
            double endLat, double endLon
    ) {
        return NativeInfotainment.calculateRoute(
                startLat, startLon, endLat, endLon
        );
    }

    public byte[] getMapTile(int zoom, int x, int y) {
        return NativeInfotainment.renderMapTile(zoom, x, y);
    }
}
