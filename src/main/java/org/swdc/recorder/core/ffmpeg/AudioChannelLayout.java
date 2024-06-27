package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avutil;

/**
 * 音频的Layout，目前的不全，以后有时间再补充。
 */
public enum AudioChannelLayout {

    layoutStereo(avutil.AV_CH_LAYOUT_STEREO),
    layoutMono(avutil.AV_CH_LAYOUT_MONO),

    layout2Point1(avutil.AV_CH_LAYOUT_2POINT1),
    layout3Point1(avutil.AV_CH_LAYOUT_3POINT1),
    layout4Point0(avutil.AV_CH_LAYOUT_4POINT0),
    layout4Point1(avutil.AV_CH_LAYOUT_4POINT1),
    layout5Point0(avutil.AV_CH_LAYOUT_5POINT0),
    layout5Point1(avutil.AV_CH_LAYOUT_5POINT1),


    layout2_1(avutil.AV_CH_LAYOUT_2_1),
    layout2_2(avutil.AV_CH_LAYOUT_2_2),
    layoutSurround(avutil.AV_CH_LAYOUT_SURROUND),
    layoutQuad(avutil.AV_CH_LAYOUT_QUAD),

    ;

    private long ffmpegChannelLayout;

    AudioChannelLayout(long layoutid) {
        this.ffmpegChannelLayout = layoutid;
    }

    public long getFfmpegChannelLayout() {
        return ffmpegChannelLayout;
    }
}
