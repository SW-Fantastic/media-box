package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avutil.AVAudioFifo;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.swresample.SwrContext;

import java.util.function.Consumer;

public class FFSwrContext implements AutoCloseable {

    private SwrContext context;

    private AVAudioFifo audioFifo;

    private int frameSize;

    private AVFrame swrFrame;

    private AVFrame resultFrame;

    private AVCodecParameters parameters;

    public FFSwrContext(SwrContext context, AVCodecParameters parameters, AVAudioFifo fifo, int frameSize) {

        this.context = context;
        this.audioFifo = fifo;
        this.frameSize = frameSize;
        this.parameters = parameters;
        this.swrFrame = avutil.av_frame_alloc();
        this.resultFrame = avutil.av_frame_alloc();

        resetFrame(resultFrame, frameSize);

    }

    public void resetFrame(AVFrame frame, int size) {

        avutil.av_frame_unref(frame);
        frame.format(parameters.format());
        frame.ch_layout(parameters.ch_layout());
        frame.sample_rate(parameters.sample_rate());
        frame.nb_samples(size);

        int state = avutil.av_frame_get_buffer(frame,0);
        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }

    }

    public boolean convert(AVFrame input, Consumer<AVFrame> callback) {

        if (!ready()) {
            return false;
        }

        resetFrame(swrFrame,input.nb_samples());

        int state = swresample.swr_convert(
                context,
                swrFrame.data(),
                swrFrame.nb_samples(),
                input.data(),
                input.nb_samples()
        );

        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }

        if (avutil.av_audio_fifo_size(audioFifo) < frameSize) {

            state = avutil.av_audio_fifo_realloc(audioFifo,avutil.av_audio_fifo_size(audioFifo) + input.nb_samples());
            if (state < 0) {
                throw FFMpegUtils.createException(state);
            }
            state = avutil.av_audio_fifo_write(audioFifo,swrFrame.data(),swrFrame.nb_samples());
            if (state < 0) {
                throw FFMpegUtils.createException(state);
            }

        }

        while (avutil.av_audio_fifo_size(audioFifo) >= frameSize) {

            resetFrame(resultFrame, frameSize);
            state = avutil.av_audio_fifo_read(
                    audioFifo,
                    resultFrame.data(),
                    frameSize
            );

            if (state < 0) {
                throw FFMpegUtils.createException(state);
            }

            callback.accept(resultFrame);

        }

        return true;
    }


    public boolean ready() {
        return context != null && !context.isNull() &&
                swrFrame != null && !swrFrame.isNull();
    }

    @Override
    public void close() {
        if (context != null && !context.isNull()) {
            swresample.swr_free(context);
            context = null;
        }
        if (swrFrame != null && !swrFrame.isNull()) {
            avutil.av_frame_free(swrFrame);
            swrFrame = null;
        }
        if (resultFrame != null && !resultFrame.isNull()) {
            avutil.av_frame_free(resultFrame);
            resultFrame = null;
        }
        if (audioFifo != null && !audioFifo.isNull()) {
            avutil.av_audio_fifo_free(audioFifo);
            audioFifo = null;
        }
        if (parameters != null && !parameters.isNull()) {
            avcodec.avcodec_parameters_free(parameters);
            parameters = null;
        }
    }
}
