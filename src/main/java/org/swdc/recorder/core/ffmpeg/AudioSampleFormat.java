package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avutil;

public enum AudioSampleFormat {

    u8(avutil.AV_SAMPLE_FMT_U8),
    u8p(avutil.AV_SAMPLE_FMT_U8P),
    s16(avutil.AV_SAMPLE_FMT_S16),
    s16p(avutil.AV_SAMPLE_FMT_S16P),
    s32(avutil.AV_SAMPLE_FMT_S32),
    s32p(avutil.AV_SAMPLE_FMT_S32P),
    s64(avutil.AV_SAMPLE_FMT_S64),
    s64p(avutil.AV_SAMPLE_FMT_S64P),
    dbl(avutil.AV_SAMPLE_FMT_DBL),
    dblp(avutil.AV_SAMPLE_FMT_DBLP),
    fltp(avutil.AV_SAMPLE_FMT_FLTP);

    private int ffmpegFormatId;

    AudioSampleFormat(int id) {
        this.ffmpegFormatId = id;
    }

    public int getFfmpegFormatId() {
        return ffmpegFormatId;
    }

    public static AudioSampleFormat valueOf(int ffmpegFormatId) {
        for (AudioSampleFormat format: values()) {
            if (format.ffmpegFormatId == ffmpegFormatId) {
                return format;
            }
        }
        return null;
    }

}
