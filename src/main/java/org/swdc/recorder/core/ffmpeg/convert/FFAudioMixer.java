package org.swdc.recorder.core.ffmpeg.convert;


import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avfilter;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.core.ffmpeg.filters.*;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 混音器。
 *
 * 音频的滤镜系统，可以用于混音，对音频进行一定的调整，
 * 使用AudioMixer时，本类的对象将会代理音频数据的写入工作。
 *
 */
public class FFAudioMixer implements Closeable {

    private Logger logger = LoggerFactory.getLogger(FFAudioMixer.class);

    private Map<Object, Map<Class, FFAudioFilter>> typedFilters = new ConcurrentHashMap<>();

    private FFAudioMixFilter mixFilter;

    private FFAudioFormatFilter formatFilter;

    private FFAudioSinkFilter sinkFilter;

    private AVFilterGraph graph;

    private AVCodecParameters parameters;

    private AVFrame filteredFrame;

    private boolean configured;

    public boolean configure(AVCodecParameters parameters) {

        if (parameters == null) {
            logger.error("audio mixer does not allow empty parameter.");
            return false;
        }

        close();

        this.parameters = parameters;

        this.graph = avfilter.avfilter_graph_alloc();

        this.mixFilter = new FFAudioMixFilter();
        this.formatFilter = new FFAudioFormatFilter();
        this.sinkFilter = new FFAudioSinkFilter();

        this.mixFilter.connectNext(this.formatFilter,0,0);
        this.formatFilter.connectNext(this.sinkFilter,0,0);

        this.filteredFrame = avutil.av_frame_alloc();

        return true;

    }

    public void transform(Object key, AVFrame frame, Consumer<AVFrame> filtered) {

        if (!this.configured) {
            logger.error("failed to transform a frame because this filter is not configured.");
            return;
        }

        filteredFrame.sample_rate(frame.sample_rate());
        filteredFrame.nb_samples(frame.nb_samples());
        filteredFrame.format(frame.format());
        filteredFrame.ch_layout(frame.ch_layout());

        FFAudioBufferFilter bufferFilter = refInputFilter(key);
        if (bufferFilter == null) {
            return;
        }
        avfilter.av_buffersrc_add_frame(bufferFilter.context(), frame);
        while (avfilter.av_buffersink_get_frame(sinkFilter.context(),filteredFrame) >= 0)  {
            filtered.accept(filteredFrame);
        }

    }

    public FFAudioVolumeFilter refVolumeFilter(Object key) {

        Map<Class,FFAudioFilter> typedFilterMap = typedFilters.computeIfAbsent(key, k -> new HashMap<>());
        if (typedFilterMap.containsKey(FFAudioVolumeFilter.class)) {
            return (FFAudioVolumeFilter) typedFilterMap.get(FFAudioVolumeFilter.class);
        }

        if (configured) {
            logger.error("failed to create input filter with context is configured.");
            return null;
        }

        FFAudioBufferFilter sourceFilter = refInputFilter(key);
        FFAudioVolumeFilter volumeFilter = new FFAudioVolumeFilter();

        sourceFilter.connectNext(volumeFilter,0,0);
        volumeFilter.connectNext(mixFilter,0,mixFilter.getIntputs());
        typedFilterMap.put(FFAudioVolumeFilter.class,volumeFilter);

        return volumeFilter;

    }

    public FFAudioBufferFilter refInputFilter(Object key) {

        Map<Class,FFAudioFilter> typedFilterMap = typedFilters.computeIfAbsent(key, k -> new HashMap<>());
        if (typedFilterMap.containsKey(FFAudioBufferFilter.class)) {
            return (FFAudioBufferFilter) typedFilterMap.get(FFAudioBufferFilter.class);
        }

        if (configured) {
            logger.error("failed to create input filter with context is configured.");
            return null;
        }

        FFAudioBufferFilter sourceFilter = new FFAudioBufferFilter();
        sourceFilter.connectNext(this.mixFilter,0,this.mixFilter.getIntputs());
        typedFilterMap.put(FFAudioBufferFilter.class,sourceFilter);

        return sourceFilter;

    }

    public synchronized boolean fullyConnect() {

        if (configured) {
            return true;
        }

        for (Map<Class,FFAudioFilter> sourceEntryFilter : typedFilters.values()) {

            for (FFAudioFilter sourceFilter : sourceEntryFilter.values()) {
                if (!sourceFilter.configure(graph,parameters)) {
                    logger.error("failed to config filter of type : " + sourceFilter.getClass() );
                    return false;
                }
            }

        }

        if (!this.mixFilter.configure(graph,parameters)) {
            logger.error("failed to config mixer filter.");
            return false;
        }

        if (!this.formatFilter.configure(graph,parameters)) {
            logger.error("failed to config format filter.");
            return false;
        }

        if (!this.sinkFilter.configure(graph,parameters)) {
            logger.error("failed to config sink filter.");
            return false;
        }

        for (Map<Class,FFAudioFilter> sourceEntryFilter : typedFilters.values()) {

            for (FFAudioFilter sourceFilter : sourceEntryFilter.values()) {

                if(!sourceFilter.filterConnect()) {
                    logger.error("failed to connect the filter : " + sourceFilter.getClass());
                    return false;
                }

            }

        }

        if(!this.mixFilter.filterConnect()){
            logger.error("failed to connect the mixer filter.");
            return false;
        }

        if(!this.formatFilter.filterConnect()) {
            logger.error("failed to connect the format filter.");
            return false;
        }

        int state = avfilter.avfilter_graph_config(graph,null);
        if (state < 0) {
            logger.error("failed to config graph : ", FFMpegUtils.createException(state));
            return false;
        }

        this.configured = true;

        return true;
    }


    public boolean isConfigured() {
        return configured;
    }

    public FFAudioSinkFilter getSinkFilter() {
        if (!configured) {
            return null;
        }
        return sinkFilter;
    }

    @Override
    public synchronized void close() {

        for (Map<Class,FFAudioFilter> sourceEntryFilter : typedFilters.values()) {

            for (FFAudioFilter sourceFilter : sourceEntryFilter.values()) {
                sourceFilter.close();
            }

        }

        typedFilters.clear();
        if (this.mixFilter != null) {
            mixFilter.close();
            mixFilter = null;
        }

        if (this.formatFilter != null) {
            formatFilter.close();
            formatFilter = null;
        }

        if (this.sinkFilter != null) {
            sinkFilter.close();
            sinkFilter = null;
        }

        if (filteredFrame != null && !filteredFrame.isNull()) {
            avutil.av_frame_unref(filteredFrame);
            avutil.av_frame_free(filteredFrame);
            filteredFrame = null;
        }

        configured = false;

    }
}
