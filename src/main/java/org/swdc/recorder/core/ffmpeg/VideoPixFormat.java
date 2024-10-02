package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.global.avutil;

/**
 * 视频像素格式，目前还不全，以后有时间再补充。
 */
public enum VideoPixFormat {

    none(avutil.AV_PIX_FMT_NONE),
    bgra(avutil.AV_PIX_FMT_BGRA),
    bgr0(avutil.AV_PIX_FMT_BGR0),
    rgb24(avutil.AV_PIX_FMT_RGB24),
    bgr24(avutil.AV_PIX_FMT_BGR24),
    yuv422p(avutil.AV_PIX_FMT_YUV422P),
    yuav422p(avutil.AV_PIX_FMT_YUVA422P),
    yuv444p(avutil.AV_PIX_FMT_YUV444P),
    yuv410p(avutil.AV_PIX_FMT_YUV410P),
    yuv411p(avutil.AV_PIX_FMT_YUV411P),
    gray8(avutil.AV_PIX_FMT_GRAY8),
    yuv420p(avutil.AV_PIX_FMT_YUV420P),
    yuyv422(avutil.AV_PIX_FMT_YUYV422);

    private int pixFmtId;

    VideoPixFormat(int val) {
        this.pixFmtId = val;
    }

    public int getPixFmtId() {
        return pixFmtId;
    }

    public static VideoPixFormat valueOf(int val) {
        for (VideoPixFormat format : values()) {
            if (format.pixFmtId == val) {
                return format;
            }
        }
        return null;
    }

}
