package org.swdc.recorder.core.ffmpeg.filters;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avfilter.AVFilter;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import org.bytedeco.ffmpeg.global.avfilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;

public class FFAudioMixFilter implements FFAudioFilter, AudioFilterListener {

    private static final String FILTER_NAME = "amix";

    private Logger logger = LoggerFactory.getLogger(FFAudioMixFilter.class);

    private AVFilter filter;

    private AVFilterContext context;

    private int intputs;

    private AudioFilterConnect connect;

    @Override
    public boolean configure(AVFilterGraph graph, AVCodecParameters parameters) {

        filter = avfilter.avfilter_get_by_name(FILTER_NAME);
        context = avfilter.avfilter_graph_alloc_filter(graph,filter,FILTER_NAME + "_Ref_" + this.hashCode());

        if (context != null) {

            int state = avfilter.avfilter_init_str(context,generateParameterStr());
            if (state < 0) {
                logger.error("failed to init this filter: " + FILTER_NAME , FFMpegUtils.createException(state));
                return false;
            }

            return true;
        }

        return false;
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

    private String generateParameterStr() {
        return "inputs=" + this.intputs;
    }

    public int getIntputs() {
        return intputs;
    }

    @Override
    public AVFilterContext context() {
        return context;
    }

    @Override
    public void close() {
        if(context != null && !context.isNull()) {

            avfilter.avfilter_free(context);
            context = null;
            connect = null;

        }
    }

    @Override
    public void onConnected(FFAudioFilter filter) {
        this.intputs = this.intputs + 1;
    }

    @Override
    public void onDisconnect(FFAudioFilter filter) {
        this.intputs = this.intputs - 1;
    }

}
