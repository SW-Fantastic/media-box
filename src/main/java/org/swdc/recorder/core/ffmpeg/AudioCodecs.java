package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avcodec;

public enum AudioCodecs {

    mp3(avcodec.AV_CODEC_ID_MP3),
    aac(avcodec.AV_CODEC_ID_AAC),

    ;

    private int codecId;

    AudioCodecs(int codecId) {
        this.codecId = codecId;
    }

    public int getCodecId() {
        return codecId;
    }

    public static AudioCodecs valueOf(int value) {
        for (AudioCodecs codecs: values()) {
            if (codecs.getCodecId() == value) {
                return codecs;
            }
        }
        return null;
    }

}
