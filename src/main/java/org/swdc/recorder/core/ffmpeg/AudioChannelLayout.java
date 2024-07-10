package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.global.avutil;

/**
 * 音频的Layout，目前的不全，以后有时间再补充。
 */
public enum AudioChannelLayout {

    layoutStereo(avutil.AV_CH_LAYOUT_STEREO, "stereo"),
    layoutMono(avutil.AV_CH_LAYOUT_MONO, "mono"),

    layout2Point1(avutil.AV_CH_LAYOUT_2POINT1, "2point1"),
    layout3Point1(avutil.AV_CH_LAYOUT_3POINT1, "3point1"),
    layout4Point0(avutil.AV_CH_LAYOUT_4POINT0, "4point0"),
    layout4Point1(avutil.AV_CH_LAYOUT_4POINT1, "4point1"),
    layout5Point0(avutil.AV_CH_LAYOUT_5POINT0, "5point0"),
    layout5Point1(avutil.AV_CH_LAYOUT_5POINT1, "5point1"),


    layout2_1(avutil.AV_CH_LAYOUT_2_1,"2_1"),
    layout2_2(avutil.AV_CH_LAYOUT_2_2, "2_2"),
    layoutSurround(avutil.AV_CH_LAYOUT_SURROUND, "surround"),
    layoutQuad(avutil.AV_CH_LAYOUT_QUAD, "quad"),

    ;

    private long ffmpegChannelLayout;

    private String name;

    AudioChannelLayout(long layoutid, String name) {
        this.ffmpegChannelLayout = layoutid;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getFfmpegChannelLayout() {
        return ffmpegChannelLayout;
    }

    public static AudioChannelLayout valueOf(AVChannelLayout input) {
        long umask = input.u_mask();
        if (umask == 0) {
            AVChannelLayout layout = new AVChannelLayout();
            avutil.av_channel_layout_default(layout,input.nb_channels());
            umask = layout.u_mask();
            layout.close();
        }
        for (AudioChannelLayout layout: values()) {
            if (layout.getFfmpegChannelLayout() == umask) {
                return layout;
            }
        }
        return null;
    }

}
