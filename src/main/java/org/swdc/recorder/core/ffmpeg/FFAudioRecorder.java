package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avfilter;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.convert.*;
import org.swdc.recorder.core.ffmpeg.filters.FFAudioVolumeFilter;
import org.swdc.recorder.core.ffmpeg.source.FFAudioSourceContext;
import org.swdc.recorder.core.ffmpeg.source.FFRecordSource;

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

    private FFAudioSourceContext sourceContext;

    private FFMpegEncoder encoder;

    private FFMpegDecoder decoder;

    private FFSwrContext swrContext;

    private FFOutputTarget target;

    private RecorderState state = RecorderState.STOPPED;

    private AudioCodecs audioCodecs;

    private FFRecordSource source;

    private CountDownLatch stateLock;

    // Filter处理

    private FFAudioMixer mixer;

    private FFAudioVolumeFilter volumeFilter;

    private double volume = 1;

    /*private FFAudioBufferFilter sourceFilter;

    private FFAudioSinkFilter sinkFilter;*/

    // Filter结束

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

    public void setMixer(FFAudioMixer mixer) {
        this.mixer = mixer;
    }

    public FFAudioMixer getMixer() {
        return mixer;
    }

    public boolean openRecorderDevice() {

        if (state != RecorderState.STOPPED) {
            return true;
        }

        close();

        sourceContext = new FFAudioSourceContext(source);
        if (!sourceContext.open()) {
            logger.error("failed to open input source");
            return false;
        }

        AVStream sourceAudioSteam = sourceContext.getStream();

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

        if (this.mixer != null) {
            // 创建输入Filter，用于混音和其他调整。
            this.mixer.refInputFilter(this);
            this.volumeFilter = this.mixer.refVolumeFilter(this);
            this.volumeFilter.setVolume(volume);
        }

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

        if (mixer != null && !mixer.isConfigured()) {
            mixer.fullyConnect();
        }

        AVStream sourceAudioSteam = sourceContext.getStream();

        // 初始化一个音视频数据包
        AVPacket packet = avcodec.av_packet_alloc();
        // 开始从视频流读取数据
        while (avformat.av_read_frame(sourceContext.getFormatCtx(), packet) == 0) {
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

                        if (mixer != null) {
                            mixer.transform(this,out, transformFrame -> {

                                transformFrame.pts(out.pts());
                                transformFrame.pkt_dts(out.pkt_dts());

                                encoder.encodeFrame(transformFrame, encoded -> {
                                    if (state == RecorderState.STOPPED) {
                                        avcodec.av_packet_unref(packet);
                                        return;
                                    }
                                    target.writeMediaPacket(encoded,MediaType.MediaTypeAudio);
                                });
                            });
                        } else {
                            encoder.encodeFrame(out, encoded -> {
                                if (state == RecorderState.STOPPED) {
                                    avcodec.av_packet_unref(packet);
                                    return;
                                }
                                target.writeMediaPacket(encoded,MediaType.MediaTypeAudio);

                            });
                        }

                    });

                });

            }

            // 及时释放，避免内存泄漏。
            avcodec.av_packet_unref(packet);
        }

        avcodec.av_packet_free(packet);

    }

    public double getVolume() {
        return volumeFilter.getVolume();
    }

    public void setVolume(double volume) {
        this.volume = volume;
        if (this.volumeFilter != null) {
            this.volumeFilter.setVolume(volume);
        }
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

        if (sourceContext != null ) {
            sourceContext.close();
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
