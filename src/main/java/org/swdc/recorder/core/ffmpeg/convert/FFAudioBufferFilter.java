package org.swdc.recorder.core.ffmpeg.convert;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avfilter.AVFilter;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import org.bytedeco.ffmpeg.global.avfilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.AudioChannelLayout;
import org.swdc.recorder.core.ffmpeg.AudioSampleFormat;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;

import java.io.IOException;

/**
 * 音频输入滤镜，是一个非常特殊的滤镜，
 * 一个音频滤镜的滤镜链条中，本滤镜为固定的第一个滤镜，
 * 数据需要从此滤镜的上下文输入到整个滤镜的处理链条中。
 */
public class FFAudioBufferFilter implements FFAudioFilter {

    private static final String FILTER_NAME = "abuffer";

    private Logger logger = LoggerFactory.getLogger(FFAudioBufferFilter.class);

    private AVFilter filter;

    private AVFilterContext context;

    private AudioFilterConnect connect;

    @Override
    public boolean configure(AVFilterGraph graph, AVCodecParameters parameters) {

        // 获取滤镜的FFmpeg对象。
        filter = avfilter.avfilter_get_by_name(FILTER_NAME);
        // 初始化，为滤镜分配内存。
        context = avfilter.avfilter_graph_alloc_filter(graph,filter,FILTER_NAME + "_Ref_" + this.hashCode());

        if (context != null) {

            // 使用指定的参数初始化滤镜。
            int state = avfilter.avfilter_init_str(context,generateParameterStr(parameters));
            if (state < 0) {
                logger.error("failed to init a filter: " + FILTER_NAME + " caused by : ", FFMpegUtils.createException(state));
                return false;
            }

            return true;
        } else {
            logger.error("failed to alloc the filter context.");
        }

        return false;
    }

    /**
     * 从AVCodecParameter生成滤镜参数。
     * @param parameters 参数对象，通常是用于Encoder的参数，这是因为数据被重采样后
     *                   格式比较统一，方便滤镜处理。
     * @return 生成的滤镜参数。
     */
    private String generateParameterStr(AVCodecParameters parameters) {
        AudioChannelLayout layout = AudioChannelLayout.valueOf(
                parameters.ch_layout()
        );
        if (layout == null) {
            throw new RuntimeException("unknown channel layout: " + parameters.ch_layout().u_mask());
        }
        AudioSampleFormat sampleFormat = AudioSampleFormat.valueOf(
                parameters.format()
        );
        if (sampleFormat == null) {
            throw new RuntimeException("unknown sample format: " + parameters.format());
        }
        String channels = String.valueOf(
                parameters.ch_layout().nb_channels()
        );
        String sampleRate = String.valueOf(
                parameters.sample_rate()
        );

        StringBuilder abufferParam = new StringBuilder()
                .append("channel_layout=").append(layout.getName()).append(":")
                .append("channels=").append(channels).append(":")
                .append("sample_rate=").append(sampleRate).append(":")
                .append("sample_fmt=").append(sampleFormat.name());

        logger.error("params : " + abufferParam);

        return abufferParam.toString();
    }

    @Override
    public void connectNext(FFAudioFilter nextFilter, int inputPad, int outputPad) {
        this.connect = new AudioFilterConnect(
                this,
                nextFilter,
                inputPad,
                outputPad
        );

        nextFilter.connected();
    }

    @Override
    public AVFilterContext context() {
        return context;
    }

    @Override
    public boolean filterConnect() {
        if (this.filter != null && this.context != null && !context.isNull() && connect != null) {
            int state = avfilter.avfilter_link(
                    this.context,
                    connect.getInputPad(),
                    this.connect.getOutFilter().context(),
                    this.connect.getOutputPad()
            );
            if (state < 0) {
                logger.error("failed to connect filter: " , FFMpegUtils.createException(state));
                return false;
            }
            return true;
        } else {
            logger.warn("can not do connect");
            return false;
        }
    }

    @Override
    public void close()  {
        if(context != null && !context.isNull()) {

            avfilter.avfilter_free(context);
            context = null;
            connect = null;

        }
    }
}
