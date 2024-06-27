package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avcodec;

public enum VideoCodecs {

    mpeg4(avcodec.AV_CODEC_ID_MPEG4),
    mpeg2ts(avcodec.AV_CODEC_ID_MPEG2TS),
    mpeg2video(avcodec.AV_CODEC_ID_MPEG2VIDEO),
    h264(avcodec.AV_CODEC_ID_H264),
    h265(avcodec.AV_CODEC_ID_H265),
    h266(avcodec.AV_CODEC_ID_H266);

    private int codecId;

    VideoCodecs(int codecId) {
        this.codecId = codecId;
    }

    public int getCodecId() {
        return codecId;
    }

    public static VideoCodecs valueOf(int value) {
        for (VideoCodecs codecs: values()) {
            if (codecs.getCodecId() == value) {
                return codecs;
            }
        }
        return null;
    }
}
