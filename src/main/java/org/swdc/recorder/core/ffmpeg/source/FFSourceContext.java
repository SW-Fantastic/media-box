package org.swdc.recorder.core.ffmpeg.source;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.swdc.recorder.core.ffmpeg.MediaType;

import java.io.Closeable;

public interface FFSourceContext extends Closeable {

    boolean ready();

    boolean open();

    AVStream getStream();

    AVFormatContext getFormatCtx();

}
