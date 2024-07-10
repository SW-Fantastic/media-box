package org.swdc.recorder.core.ffmpeg.source;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.core.ffmpeg.MediaType;

import java.io.Closeable;


/**
 * 音频录制上下文。
 *
 * 主要作用是处理音频源的打开和关闭。
 */
public class FFAudioSourceContext implements FFSourceContext, Closeable {

    private static Logger logger = LoggerFactory.getLogger(FFAudioSourceContext.class);

    private AVInputFormat sourceFormat;

    private AVFormatContext sourceFormatCtx;

    private FFRecordSource source;

    private AVStream sourceAudioSteam;

    public FFAudioSourceContext(FFRecordSource source) {
        this.source = source;
    }

    @Override
    public boolean ready() {

        return sourceFormat != null && !sourceFormat.isNull() &&
                sourceFormatCtx != null && !sourceFormatCtx.isNull() &&
                sourceAudioSteam != null && !sourceAudioSteam.isNull();

    }

    /**
     * 打开音频源
     * @return 是否成功
     */
    @Override
    public boolean open() {
        if (source.getType() != MediaType.MediaTypeAudio) {
            // 打开的不是音频源。
            logger.error("failed to open source caused by type mismatched.");
            return false;
        }

        if (ready()) {
            return true;
        }

        sourceFormat = source.getFormat();
        if (sourceFormat == null) {
            // 没有提供Format
            logger.error("provide source does not have input format.");
            return false;
        }

        // 配置录音选项
        AVDictionary dictionary = new AVDictionary();
        int state = 0;
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
            logger.error("failed to open input format : ", FFMpegUtils.createException(state));
            return false;
        }

        state = avformat.avformat_find_stream_info(sourceFormatCtx,(AVDictionary) null);
        if (state < 0) {
            logger.error("failed to open input format : ", FFMpegUtils.createException(state));
            return false;
        }

        this.sourceAudioSteam = FFMpegUtils.findInputAVSteam(sourceFormatCtx,MediaType.MediaTypeAudio);
        return ready();
    }

    @Override
    public AVStream getStream() {
        return sourceAudioSteam;
    }

    @Override
    public AVFormatContext getFormatCtx() {
        return sourceFormatCtx;
    }

    public FFRecordSource getSource() {
        return source;
    }

    @Override
    public void close() {

        if (this.sourceFormatCtx != null && !this.sourceFormatCtx.isNull()) {
            avformat.avformat_close_input(sourceFormatCtx);
            avformat.avformat_free_context(sourceFormatCtx);
            sourceFormat = null;
            sourceFormatCtx = null;
        }

    }
}
