package com.auto.infotainment.connectivity;

import com.auto.infotainment.nativebridge.NativeInfotainment;

public class ConnectivitySuite {

    public boolean pairDevice(String macAddress) {
        return NativeInfotainment.pairBluetoothDevice(macAddress);
    }

    public String[] searchPOI(String keyword) {
        return NativeInfotainment.searchPointsOfInterest(keyword);
    }
}
