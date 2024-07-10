package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.*;
import org.bytedeco.ffmpeg.global.*;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.swdc.recorder.core.ffmpeg.convert.FFSwrContext;
import org.swdc.recorder.core.ffmpeg.convert.FFSwsContext;

import java.nio.DoubleBuffer;
import java.util.function.Consumer;

public class FFMpegEncoder implements AutoCloseable {

    private AVCodec codec;

    private AVCodecContext context;

    private AVPacket packet;

    private boolean opened = false;

    public FFMpegEncoder(int codecId) {
        if (codecId < 0) {
            throw new RuntimeException("invalid codec id");
        }
        this.codec = avcodec.avcodec_find_encoder(codecId);
        if (codec != null) {
            context = avcodec.avcodec_alloc_context3(codec);
        }

        if (codecId == avcodec.AV_CODEC_ID_H264) {
            avutil.av_opt_set(context.priv_data(),"b-pyramid", "none",0);
            avutil.av_opt_set(context.priv_data(),"preset", "slower",0);
            avutil.av_opt_set(context.priv_data(),"tune", "zerolatency",0);
        }

        this.packet = avcodec.av_packet_alloc();

        if (changeable()) {
            context.codec_id(codec.id());
        }
    }

    private boolean changeable() {
        // Open后不允许修改参数。
        return !opened && ready();
    }

    public boolean ready() {
        return codec != null && !codec.isNull() &&
                context != null && !context.isNull() &&
                packet != null && !packet.isNull();
    }



    public FFMpegEncoder timeBase(AVRational timeBase) {

        if (changeable()) {
            context.time_base(timeBase);
            return this;
        }

        throw new RuntimeException("failed to init encoder");

    }

    /**
     * 设定时间基础
     * @param num
     * @param den
     * @return
     */
    public FFMpegEncoder timeBase(int num, int den) {

        if (changeable()) {

            context.time_base(avutil.av_make_q(num,den));

            return this;
        }

        throw new RuntimeException("failed to init encoder");
    }

    /**
     * 设置编码的媒体类型
     * @param type 媒体类型
     * @return 本对象
     */
    public FFMpegEncoder mediaType(MediaType type) {

        if (changeable()) {
            context.codec_type(type.getFFMpegType());
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    public MediaType mediaType() {
        if (ready()) {
            int codecType = context.codec_type();
            return MediaType.valueOf(codecType);
        }
        return null;
    }


    //Encoder Parameters For Video Begin

    /**
     * 视频编码参数：
     * 设置视频的宽度和高度
     * @param width 宽度
     * @param height 高度
     * @return 本对象
     */
    public FFMpegEncoder size(int width, int height) {

        if (changeable()) {
            context.width(width);
            context.height(height);
            return this;
        }

        throw new RuntimeException("failed to init encoder");

    }

    /**
     * 视频编码参数：
     * 最多能有几个双向预测帧（B帧）
     * @param maxBFrameSize 最大的双向预测帧数量
     * @return 本对象
     */
    public FFMpegEncoder maxBFrameSize(int maxBFrameSize) {
        if (changeable()) {
            context.max_b_frames(maxBFrameSize);
            return this;
        } else {
            throw new RuntimeException("failed to init encoder.");
        }
    }


    /**
     * 视频编码参数：
     * 设置像素类型
     * @param format 像素类型
     * @return 本对象
     */
    public FFMpegEncoder pixFormat(VideoPixFormat format) {

        if (changeable()) {
            context.pix_fmt(format.getPixFmtId());
            return this;
        }

        throw new RuntimeException("failed to init encoder");

    }

    public FFMpegEncoder frameRate(int num, int den) {
        if (changeable()) {

            AVRational rational = new AVRational();
            rational.num(num);
            rational.den(den);

            context.framerate(rational);

            return this;

        }
        throw new RuntimeException("failed to init encoder");
    }

    public FFMpegEncoder frameRate(AVRational frameRate) {
        if (changeable()) {
            context.framerate(frameRate);
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    /**
     * 视频编码参数：
     * 设置比特率
     * @param bitRate bitRate
     * @return 本对象
     */
    public FFMpegEncoder bitRate(long bitRate) {

        if (changeable()) {
            context.bit_rate(bitRate);
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    // Encoder Parameters For Video End

    /**
     * 音频编码参数：
     * 设置采样格式
     * @param audioSampleFormat 采样格式。
     * @return
     */
    public FFMpegEncoder sampleFormat(AudioSampleFormat audioSampleFormat) {

        if (changeable()) {
            context.sample_fmt(audioSampleFormat.getFfmpegFormatId());
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    public AudioSampleFormat sampleFormat() {

        if (!ready()) {
            return null;
        }

        return AudioSampleFormat.valueOf(context.sample_fmt());

    }

    public AVChannelLayout channelLayout() {
        if (ready()) {
            return context.ch_layout();
        }
        return null;
    }

    /**
     * 音频编码参数：
     * 音频通道布局
     * @param channelLayout 布局。
     * @return
     */
    public FFMpegEncoder channelLayout(AudioChannelLayout channelLayout) {

        if (changeable()) {
            context.channel_layout(channelLayout.getFfmpegChannelLayout());
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    /**
     * 音频编码参数：
     * 音频通道数。
     * @param channels 通道数量
     * @return
     */
    public FFMpegEncoder channels(int channels) {

        if (changeable()) {
            context.channels(channels);
            return this;
        }
        throw new RuntimeException("failed to init encoder");

    }

    public int channels() {

        if (ready()) {
            return context.ch_layout().nb_channels();
        }
        return 0;
    }

    /**
     * 音频编码参数：
     * 采样率
     * @param sampleRate 采样率
     * @return
     */
    public FFMpegEncoder sampleRate(int sampleRate) {

        if (changeable()) {
            context.sample_rate(sampleRate);
            return this;
        }
        throw new RuntimeException("failed to init encoder");
    }

    public int frameSize() {
        if (ready()) {
            return context.frame_size();
        }
        return 0;
    }

    public int sampleRate() {
        if (ready()) {
            return context.sample_rate();
        }
        return 0;
    }


    /**
     * 每批编码多少帧
     * @param gopSize 批量大小
     * @return 本对象
     */
    public FFMpegEncoder groupSize(int gopSize) {

        if (changeable()) {
            context.gop_size(gopSize);
            return this;
        }

        throw new RuntimeException("failed to init encoder");
    }

    /**
     * 基于本编码器创建流。
     * @param context 格式上下文
     * @return 创建的音视频流
     */
    public AVStream createSteam(AVFormatContext context) {
        if (!ready()) {
            return null;
        }

        int state = 0;
        if (!opened) {
            if (avcodec.avcodec_is_open(this.context) > 0) {
                opened = true;
            } else {
                state = avcodec.avcodec_open2(this.context,codec,(AVDictionary) null);
                if (state < 0) {
                    throw FFMpegUtils.createException(state);
                }
            }
        }

        return avformat.avformat_new_stream(context,this.codec);
    }

    /**
     * 将本编码器的配置复制到指定对象
     * @param parameters 参数对象
     */
    public void transferParameterTo(AVCodecParameters parameters) {

        if (ready()) {

            int state = avcodec.avcodec_parameters_from_context(parameters,context);
            if (state < 0) {
                throw FFMpegUtils.createException(state);
            }
        }

    }

    /**
     * 编码一帧
     * @param frame 音视频帧
     * @param encodedCallback 编码回调，编码完成后调用
     * @return 是否顺利完成。
     */
    public boolean encodeFrame(AVFrame frame, Consumer<AVPacket> encodedCallback) {

        int state = 0;
        if (ready()) {

            if (!opened) {
                state = avcodec.avcodec_open2(context,codec,(AVDictionary) null);
                if (state < 0) {
                    throw FFMpegUtils.createException(state);
                }
                opened = true;
            }
            state = avcodec.avcodec_send_frame(context,frame);
            if (state < 0) {
                throw FFMpegUtils.createException(state);
            }

            while (avcodec.avcodec_receive_packet(context,packet) == 0) {
                encodedCallback.accept(packet);
                avcodec.av_packet_unref(packet);
            }

        }

        return true;
    }

    public AVRational timeBase() {
        if (!ready()) {
            return null;
        }
        return context.time_base();
    }

    public FFSwrContext createAudioSwrContext(FFMpegDecoder input) {

        if (!ready()) {
            return null;
        }

        if (!input.ready()) {
            return null;
        }

        if (!input.isOpened()) {
            input.open();
        }

        int codecType = context.codec_type();
        MediaType mediaType = MediaType.valueOf(codecType);
        if (mediaType != MediaType.MediaTypeAudio) {
            return null;
        }

        SwrContext swrContext = new SwrContext(null);
        int state = swresample.swr_alloc_set_opts2(
                swrContext,
                context.ch_layout(),
                context.sample_fmt(),
                context.sample_rate(),
                input.channelLayout(),
                input.sampleFormat().getFfmpegFormatId(),
                input.sampleRate(),
                0,
                null
        );

        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }

        state = swresample.swr_init(swrContext);
        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }

        AVAudioFifo fifo = avutil.av_audio_fifo_alloc(
                context.sample_fmt(),
                context.ch_layout().nb_channels(),
                context.frame_size()
        );

        if (fifo == null || fifo.isNull()) {
            return null;
        }

        AVCodecParameters parameters = avcodec.avcodec_parameters_alloc();
        avcodec.avcodec_parameters_from_context(parameters,context);

        return new FFSwrContext(swrContext,parameters,fifo,this.frameSize());
    }

    public FFSwsContext createVideoSwsContext(FFMpegDecoder input) {

        if (!ready()) {
            return null;
        }

        if (!input.ready()) {
            return null;
        }

        if (!input.isOpened()) {
            input.open();
        }

        int codecType = context.codec_type();
        MediaType type = MediaType.valueOf(codecType);
        if (type != MediaType.MediaTypeVideo) {
            return null;
        }

        AVFrame swsFrame = avutil.av_frame_alloc();
        if (swsFrame == null) {
            return null;
        }

        swsFrame.width(context.width());
        swsFrame.height(context.height());
        swsFrame.format(context.pix_fmt());

        int state = avutil.av_frame_get_buffer(swsFrame,1);
        if (state < 0) {
            return null;
        }

        VideoPixFormat inputFormat = input.pixFormat();
        if (inputFormat == VideoPixFormat.none) {
            inputFormat = VideoPixFormat.bgra;
        }

        SwsContext swsContext = swscale.sws_getContext(
                input.width(),
                input.height(),
                inputFormat.getPixFmtId(),
                context.width(),
                context.height(),
                context.pix_fmt(),
                swscale.SWS_BICUBIC,null,null,(DoubleBuffer) null
        );


        if (swsContext == null) {
            return null;
        }
        return new FFSwsContext(this,swsContext,swsFrame);

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


    /**
     * 关闭对象，释放资源。
     */
    @Override
    public void close() {

        if (opened) {
            avcodec.avcodec_close(context);
        }

        if (context != null && !context.isNull()) {
            avcodec.avcodec_free_context(context);
            context = null;
        }

        if (codec != null && !codec.isNull()) {
            codec.close();
            this.codec = null;
        }

        if (packet != null && !packet.isNull()) {
            avcodec.av_packet_free(packet);
            packet = null;
        }

    }
}
