package com.auto.infotainment.media;

import com.auto.infotainment.nativebridge.NativeInfotainment;

public class MediaCenter {

    public void play(String mp3Uri) {
        NativeInfotainment.playMp3Stream(mp3Uri);
    }

    public void adjustEqualizer(int bass, int mid, int treble) {
        NativeInfotainment.setEqualizer(bass, mid, treble);
    }
}
