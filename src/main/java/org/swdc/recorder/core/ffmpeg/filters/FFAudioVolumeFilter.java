package org.swdc.recorder.core.ffmpeg.filters;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avfilter.AVFilter;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import org.bytedeco.ffmpeg.avutil.AVOption;
import org.bytedeco.ffmpeg.global.avfilter;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;

public class FFAudioVolumeFilter implements FFAudioFilter {

    private static final String FILTER_NAME = "volume";

    private static Logger logger = LoggerFactory.getLogger(FFAudioVolumeFilter.class);;

    private AVFilter filter;

    private AVFilterContext context;

    private AudioFilterConnect connect;

    private double volume = 1.0;

    @Override
    public boolean configure(AVFilterGraph graph, AVCodecParameters parameters) {

        this.filter = avfilter.avfilter_get_by_name(FILTER_NAME);
        this.context = avfilter.avfilter_graph_alloc_filter(graph,filter,FILTER_NAME + "_Ref_" + this.hashCode());
        if (context != null) {

            int state = avfilter.avfilter_init_str(context,generateParameters());
            if (state < 0) {
                logger.error("failed to init volume filter ", FFMpegUtils.createException(state));
                return false;
            }

            return true;
        }

        return false;
    }


    private String generateParameters() {

        return "volume=" + volume;

    }

    @Override
    public void connectNext(FFAudioFilter nextFilter, int inputPad, int outputPad) {
        if (this.connect != null && connect.getOutFilter() instanceof AudioFilterListener) {
            AudioFilterListener trigger = (AudioFilterListener) connect.getOutFilter();
            trigger.onDisconnect(this);
        }

        this.connect = new AudioFilterConnect(
                this,
                nextFilter,
                inputPad,
                outputPad
        );

        if (nextFilter instanceof AudioFilterListener) {
            AudioFilterListener trigger = (AudioFilterListener) nextFilter;
            trigger.onConnected(this);
        }
    }

    public void setVolume(double volume) {

        if (context == null || context.graph() == null || context.graph().isNull()) {
            this.volume = volume;
            return;
        }

        int state = avfilter.avfilter_graph_send_command(
                context.graph(),
                FILTER_NAME + "_Ref_" + this.hashCode(),
                "volume",
                String.valueOf(volume),
                (BytePointer) null,
                0,
                1
        );

        if (state < 0) {
            logger.error("failed to set volume ", FFMpegUtils.createException(state));
        } else {
            this.volume = volume;
        }


    }

    public double getVolume() {
        return volume;
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
    public void close() {
        if(context != null && !context.isNull()) {

            avfilter.avfilter_free(context);
            context = null;
            connect = null;

        }
    }
}
