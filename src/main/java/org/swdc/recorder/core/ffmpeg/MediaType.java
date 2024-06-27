package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avutil;

public enum MediaType {

    MediaTypeVideo(avutil.AVMEDIA_TYPE_VIDEO),
    MediaTypeAudio(avutil.AVMEDIA_TYPE_AUDIO)

    ;
    private int type;

    MediaType(int type) {
        this.type = type;
    }

    public int getFFMpegType() {
        return type;
    }

    public static MediaType valueOf(int val) {
        for (MediaType v : values()) {
            if (v.type == val) {
                return v;
            }
        }
        return null;
    }

}
