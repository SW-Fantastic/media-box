package org.swdc.recorder.core.ffmpeg.source;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.core.ffmpeg.MediaType;
import org.swdc.recorder.core.ffmpeg.VideoPixFormat;

import java.io.Closeable;
import java.io.IOException;

public class FFVideoSourceContext implements FFSourceContext, Closeable {

    private static Logger logger = LoggerFactory.getLogger(FFVideoSourceContext.class);

    private static final String pixelFormatKey = "pixel_format";

    private VideoPixFormat captureFormat = VideoPixFormat.yuv420p;


    /**
     * 视频输入格式
     */
    private AVInputFormat sourceFormat;

    /**
     * 视频输入格式上下文
     */
    private AVFormatContext sourceFormatCtx;

    /**
     * 视频输入流
     */
    private AVStream sourceVideoSteam;


    private FFRecordSource source;

    public FFVideoSourceContext(FFRecordSource source) {
        this.source = source;
    }


    public VideoPixFormat getCaptureFormat() {
        return captureFormat;
    }

    public void setCaptureFormat(VideoPixFormat captureFormat) {
        this.captureFormat = captureFormat;
    }

    @Override
    public boolean ready() {

        return sourceFormat != null && !sourceFormat.isNull() &&
                sourceFormatCtx != null && !sourceFormatCtx.isNull() &&
                sourceVideoSteam != null && !sourceVideoSteam.isNull();

    }

    @Override
    public boolean open() {

        if (ready()) {
            return true;
        }

        if (source.getType() != MediaType.MediaTypeVideo) {
            return false;
        }

        // 搜索输入设备
        sourceFormat = source.getFormat();
        if (sourceFormat == null) {
            logger.error("provided source does not have a input format");
            return false;
        }
        // 配置录屏选项
        AVDictionary dictionary = new AVDictionary();
        int state = 0;
        // 设定像素格式
        state = avutil.av_dict_set(dictionary,pixelFormatKey, getCaptureFormat().name(), 1);
        if (state < 0) {
            logger.error("failed to setup options ", FFMpegUtils.createException(state));
            return false;
        }
        // 创建Context
        sourceFormatCtx = avformat.avformat_alloc_context();
        // 打开输入设备
        state = avformat.avformat_open_input(
                sourceFormatCtx,
                source.getUrl(),
                sourceFormat,
                dictionary
        );

        if (state < 0) {
            logger.warn("failed to open with special pixel format, try to open directly.");
            state = avformat.avformat_open_input(
                    sourceFormatCtx,
                    source.getUrl(),
                    sourceFormat,
                    null
            );
            if (state < 0) {
                logger.error("failed to open input format : ", FFMpegUtils.createException(state));
                return false;
            }
        }

        state = avformat.avformat_find_stream_info(sourceFormatCtx,(AVDictionary) null);
        if (state < 0) {
            logger.error("failed to fetch steam info");
            return false;
        }
        // 查找视频流
        sourceVideoSteam = FFMpegUtils.findInputAVSteam(sourceFormatCtx, MediaType.MediaTypeVideo);
        if (sourceVideoSteam == null) {
            // 视频流不存在
            logger.error("Can not found video source");
            return false;
        }

        return ready();
    }

    @Override
    public AVStream getStream() {
        return sourceVideoSteam;
    }

    @Override
    public AVFormatContext getFormatCtx() {
        return sourceFormatCtx;
    }

    @Override
    public void close()  {
        if (sourceFormatCtx != null && ! sourceFormatCtx.isNull()) {
            avformat.avformat_close_input(sourceFormatCtx);
            avformat.avformat_free_context(sourceFormatCtx);
            sourceFormatCtx = null;
        }
    }
}
