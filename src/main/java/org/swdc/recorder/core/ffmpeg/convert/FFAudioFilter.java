package org.swdc.recorder.core.ffmpeg.convert;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avfilter.AVFilterContext;
import org.bytedeco.ffmpeg.avfilter.AVFilterGraph;

import java.io.Closeable;
import java.io.IOException;

public interface FFAudioFilter extends Closeable {

    /**
     * 配置并启动Filter。
     *
     * 这个在connectNext完成后进行。
     *
     * @param graph Filter管道上下文
     * @param parameters Filter参数
     * @return 是否成功。
     */
    boolean configure(AVFilterGraph graph, AVCodecParameters parameters);

    /**
     *
     * 创建Filter之间的链接关系。
     *
     * 此时不会立即链接，而是会记录这个链接关系。
     *
     * @param nextFilter 下一个Filter
     * @param inputPad  来自本Filter的输出index
     * @param outputPad 输出到下一个Filter的index。
     */
    void connectNext(FFAudioFilter nextFilter, int inputPad, int outputPad);

    /**
     * 返回Filter的Context，用于链接Filter。
     * @return FFmpeg额度AVFilterContext
     */
    AVFilterContext context();

    /**
     * 回调，connectNext后应当调用本方法。
     */
    default void connected() {
    }

    /**
     * 真正完成Filter的链接。
     * @return 是否成功。
     */
    boolean filterConnect();

    @Override
    void close();
}
