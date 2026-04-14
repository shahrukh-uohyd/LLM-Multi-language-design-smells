package com.iotcontroller.cloud;

import com.iotcontroller.nativebridge.NativeController;

public class CloudService {

    public boolean connect(String relayEndpoint) {
        return NativeController.connectToRemoteRelay(relayEndpoint);
    }

    public byte[] fetchLatestConfiguration() {
        return NativeController.fetchConfigUpdate();
    }

    public void logEvent(String eventJson) {
        NativeController.logEventToCloud(eventJson);
    }
}
