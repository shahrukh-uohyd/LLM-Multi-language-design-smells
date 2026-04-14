package com.wearable.health;

import com.wearable.nativebridge.NativeWearableBridge;

public class HeartRateMonitor {

    public int measureBpm() {
        int[] ppgSamples = NativeWearableBridge.readPpgSensor();

        if (ppgSamples == null || ppgSamples.length == 0) {
            throw new IllegalStateException("PPG sensor returned no data");
        }

        return NativeWearableBridge.calculateBpm(ppgSamples);
    }
}
