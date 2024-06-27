package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

import java.util.function.Consumer;

public class FFMpegDecoder implements AutoCloseable {


    private AVCodec codec;

    private AVCodecContext context;

    private AVFrame frame;

    private boolean opened;

    public FFMpegDecoder(int codecId) {

        if (codecId < 0) {
            throw new RuntimeException("invalid codec id");
        }

        // 初始化一个Frame
        this.frame = avutil.av_frame_alloc();
        // 查找解码对象
        this.codec = avcodec.avcodec_find_decoder(codecId);
        if (codec != null) {
            // 创建解码上下文
            context = avcodec.avcodec_alloc_context3(codec);
        }

    }

    /**
     * 为解码器设置参数
     * @param parameter 编码参数对象
     */
    public void loadParameterFrom(AVCodecParameters parameter) {
        if (!ready()) {
            return;
        }
        int state = avcodec.avcodec_parameters_to_context(context,parameter);
        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }
    }


    public int width() {
        if (!ready()) {
            return 0;
        }
        return context.width();
    }


    public int height() {
        if (!ready()) {
            return 0;
        }
        return context.height();
    }


    public VideoPixFormat pixFormat() {

        if (!ready()) {
            return null;
        }

        return VideoPixFormat.valueOf(context.pix_fmt());

    }


    public AVChannelLayout channelLayout() {
        if (!ready()) {
            return null;
        }
        return context.ch_layout();
    }

    public AudioSampleFormat sampleFormat() {

        if (!ready()) {
            return null;
        }

        return AudioSampleFormat.valueOf(context.sample_fmt());

    }

    public int sampleRate() {

        if (!ready()) {
            return 0;
        }

        return context.sample_rate();
    }

    public int channels() {

        if (!ready()) {
            return 0;
        }

        return context.ch_layout().nb_channels();
    }


    void open() {
        if (!opened) {
            // 解码器没有打开，需要首先启动解码器
            int state = avcodec.avcodec_open2(context,codec,(AVDictionary)null);
            if (state < 0) {
                // 启动失败
                throw FFMpegUtils.createException(state);
            }
            opened = true;
        }
    }

    /**
     * 执行解码
     * @param packet 音视频数据包
     * @param decoded 解码回调函数，解码后的Frame将会传入这里。
     * @return 是否顺利完成。
     */
    public boolean decodePacket(AVPacket packet, Consumer<AVFrame> decoded) {

        if (!ready()) {
            return false;
        }

        if (packet == null || packet.isNull()) {
            return false;
        }


        int state = 0;

        if (!opened) {
            open();
        }

        state = avcodec.avcodec_send_packet(context,packet);
        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }


        while (avcodec.avcodec_receive_frame(context,frame) == 0) {
            decoded.accept(frame);
            avutil.av_frame_unref(frame);
        }

        return true;
    }

    public AVRational timeBase() {
        if (!ready()) {
            return null;
        }
        return context.time_base();
    }


    /**
     * 判读FFMpeg对象是否初始化完毕
     * @return yes or no
     */
    public boolean ready() {
        return codec != null && !codec.isNull() &&
                context != null && !context.isNull() &&
                frame != null && !frame.isNull();
    }

    public boolean isOpened() {
        return opened;
    }

    @Override
    public void close() {
        if (context != null && !context.isNull()) {
            if (opened) {
                avcodec.avcodec_close(context);
            }
            avcodec.avcodec_free_context(context);
            context = null;
        }
        if (codec != null && !codec.isNull()) {
            codec.close();
            codec = null;
        }
        if (frame != null && !frame.isNull()) {
            avutil.av_frame_free(frame);
            frame = null;
        }
    }
}
