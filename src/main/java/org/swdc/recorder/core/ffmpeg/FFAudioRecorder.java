package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Audio录制器。
 * 用于录制音频
 */
public class FFAudioRecorder implements AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(FFAudioRecorder.class);

    private int sampleRate = 48000;

    private AudioSampleFormat sampleFormat;
    private AudioChannelLayout channelLayout = AudioChannelLayout.layoutStereo;

    private AVInputFormat sourceFormat;
    private AVFormatContext sourceFormatCtx;
    private AVStream sourceAudioSteam;

    private FFMpegEncoder encoder;
    private FFMpegDecoder decoder;
    private FFSwrContext swrContext;

    private FFOutputTarget target;

    private RecorderState state = RecorderState.STOPPED;

    private AudioCodecs audioCodecs;

    private FFRecordSource source;

    private CountDownLatch stateLock;

    public FFAudioRecorder() {
    }

    public void setAudioCodecs(AudioCodecs audioCodecs) {
        this.audioCodecs = audioCodecs;
    }

    public AudioCodecs getAudioCodecs() {
        return audioCodecs;
    }

    public void setTarget(FFOutputTarget target) {
        this.target = target;
    }

    public FFOutputTarget getTarget() {
        return target;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public AudioSampleFormat getSampleFormat() {
        return sampleFormat;
    }

    public void setSampleFormat(AudioSampleFormat sampleFormat) {
        this.sampleFormat = sampleFormat;
    }

    public AudioChannelLayout getChannelLayout() {
        return channelLayout;
    }

    public void setChannelLayout(AudioChannelLayout channelLayout) {
        this.channelLayout = channelLayout;
    }

    public void setSource(FFRecordSource source) {
        this.source = source;
    }

    public FFRecordSource getSource() {
        return source;
    }

    public boolean openRecorderDevice() {

        if (state != RecorderState.STOPPED) {
            return true;
        }

        close();

        sourceFormat = source.getFormat();
        if (sourceFormat == null) {
            logger.error("provide source does not have input format.");
            return false;
        }

        // 配置录音选项
        AVDictionary dictionary = new AVDictionary();
        int state = 0;
        // 创建Context
        sourceFormatCtx = avformat.avformat_alloc_context();
        // 打开输入设备
        state = avformat.avformat_open_input(
                sourceFormatCtx,
                source.getUrl(),
                sourceFormat,
                dictionary
        );

        if (state < 0) {
            logger.error("failed to open input format : ", FFMpegUtils.createException(state));
            return false;
        }

        state = avformat.avformat_find_stream_info(sourceFormatCtx,(AVDictionary) null);
        if (state < 0) {
            logger.error("failed to open input format : ", FFMpegUtils.createException(state));
            return false;
        }

        // 查找音频流
        sourceAudioSteam = FFMpegUtils.findInputAVSteam(sourceFormatCtx,MediaType.MediaTypeAudio);
        if (sourceAudioSteam == null) {
            // 视频流不存在
            logger.error("Can not found video source");
            return false;
        }

        // 创建音频解码器
        decoder = new FFMpegDecoder(sourceAudioSteam.codecpar().codec_id());
        // 加载音频解码器参数
        decoder.loadParameterFrom(sourceAudioSteam.codecpar());

        AVChannelLayout layout = new AVChannelLayout();
        avutil.av_channel_layout_from_mask(layout,getChannelLayout().getFfmpegChannelLayout());

        encoder = new FFMpegEncoder(audioCodecs.getCodecId());
        encoder.mediaType(MediaType.MediaTypeAudio)
                .sampleFormat(getSampleFormat() == null ? decoder.sampleFormat() : getSampleFormat())
                .channelLayout(getChannelLayout())
                .channels(layout.nb_channels())
                .sampleRate(getSampleRate());

        if (!this.target.openOutput(encoder)) {
            logger.error("failed to open target file.");
            return false;
        }

        // 重采样上下文
        swrContext = encoder.createAudioSwrContext(decoder);
        this.state = RecorderState.READY;

        return true;
    }

    public void record() {

        if (target == null) {
            logger.error("no output target");
            close();
            return;
        }

        if (!this.openRecorderDevice()) {
            close();
            return;
        }

        AtomicLong count = new AtomicLong(0);

        // 初始化一个音视频数据包
        AVPacket packet = avcodec.av_packet_alloc();
        // 开始从视频流读取数据
        while (avformat.av_read_frame(sourceFormatCtx, packet) == 0) {
            if (state == RecorderState.READY) {
                state = RecorderState.RECORDING;
            } else if (state == RecorderState.PAUSED) {
                avcodec.av_packet_unref(packet);
                try {
                    stateLock = new CountDownLatch(1);
                    stateLock.await();
                    stateLock = null;
                } catch (Exception e) {
                }
                continue;
            }
            if (state == RecorderState.STOPPED) {
                avcodec.av_packet_unref(packet);
                break;
            }
            if (packet.stream_index() == sourceAudioSteam.index()) {
                // 读取到的是音频流的数据
                // 将这个数据包发送到解码器中（解码器是异步运行的）
                decoder.decodePacket(packet, frame -> {
                    // 解码完毕，开始转码，这一步的目的是将输入格式转码为输出的格式
                    swrContext.convert(frame, out -> {
                        // 转码完毕，开始编码，这一步的目的是将转码后的数据进行编码以便输出到文件。
                        encoder.encodeFrame(out, encoded -> {
                            if (state == RecorderState.STOPPED) {
                                avcodec.av_packet_unref(packet);
                                return;
                            }
                            target.writeMediaPacket(encoded,MediaType.MediaTypeAudio);

                        });
                        count.set(count.get() + 1);
                    });
                });

            }

            // 及时释放，避免内存泄漏。
            avcodec.av_packet_unref(packet);
        }

        avcodec.av_packet_free(packet);

    }

    public void stop() {
        if (this.state == RecorderState.RECORDING) {
            this.state = RecorderState.STOPPED;
        } else if (this.state == RecorderState.PAUSED) {
            this.state = RecorderState.STOPPED;
            if (stateLock != null) {
                stateLock.countDown();
            }
        } else if (this.state == RecorderState.READY) {
            this.state = RecorderState.STOPPED;
        }
    }

    public void pause() {
        if (this.state == RecorderState.RECORDING) {
            this.state = RecorderState.PAUSED;
        } else if (state == RecorderState.READY) {
            this.state = RecorderState.PAUSED;
        }
    }

    public void resume() {
        if (this.state == RecorderState.PAUSED) {
            this.state = RecorderState.RECORDING;
            if (stateLock != null) {
                stateLock.countDown();
            }
        } else if (this.state == RecorderState.STOPPED) {
            throw new RuntimeException("can not resume from stopped State");
        }
    }

    @Override
    public void close() {

        if (sourceFormatCtx != null && !sourceFormatCtx.isNull()) {
            avformat.avformat_close_input(sourceFormatCtx);
            avformat.avformat_free_context(sourceFormatCtx);
            sourceFormat = null;
        }

        if (swrContext != null) {
            swrContext.close();
            swrContext = null;
        }

        if (encoder != null) {
            encoder.close();
            encoder = null;
        }

        if (decoder != null) {
            decoder.close();
            decoder = null;
        }

    }
}
