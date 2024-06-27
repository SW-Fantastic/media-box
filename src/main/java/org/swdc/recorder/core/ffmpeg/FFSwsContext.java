package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;

public class FFSwsContext implements AutoCloseable {

    private BytePointer buffer;

    private SwsContext context;

    private AVFrame frame;

    private FFMpegEncoder encoder;

    public FFSwsContext(FFMpegEncoder encoder, BytePointer buffer, SwsContext context, AVFrame frame) {
        this.buffer = buffer;
        this.context = context;
        this.frame = frame;
        this.encoder = encoder;
    }


    public AVFrame scale(AVFrame input) {

        if (!ready()) {
            return null;
        }

        int state = swscale.sws_scale(
                context,
                input.data(),
                input.linesize(),
                0,
                input.height(),
                frame.data(),
                frame.linesize()
        );

        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }

        return frame;
    }

    public boolean ready() {
        return context != null && !context.isNull() &&
                buffer != null && !buffer.isNull() &&
                frame != null && !frame.isNull();
    }


    @Override
    public void close() {
        if (context != null && !context.isNull()) {
            swscale.sws_freeContext(context);
            context = null;
        }
        if (buffer != null && !buffer.isNull()) {
            avutil.av_free(buffer);
            buffer = null;
        }
        if (frame != null && !frame.isNull()) {
            avutil.av_frame_free(frame);
            frame = null;
        }
    }
}
