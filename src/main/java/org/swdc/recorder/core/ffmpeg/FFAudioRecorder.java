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

        /*AVFilterGraph graph = avfilter.avfilter_graph_alloc();

        AVCodecParameters parameters = avcodec.avcodec_parameters_alloc();
        encoder.transferParameterTo(parameters);

        sourceFilter = new FFAudioBufferFilter();
        FFAudioMixFilter audioMixFilter = new FFAudioMixFilter();
        FFAudioFormatFilter formatsFilter = new FFAudioFormatFilter();
        sinkFilter = new FFAudioSinkFilter();

        sourceFilter.connectNext(audioMixFilter,0,0);
        audioMixFilter.connectNext(formatsFilter,0,0);
        formatsFilter.connectNext(sinkFilter,0,0);

        sourceFilter.configure(graph,parameters);
        audioMixFilter.configure(graph,parameters);
        formatsFilter.configure(graph,parameters);
        sinkFilter.configure(graph,parameters);

        sourceFilter.filterConnect();
        audioMixFilter.filterConnect();
        formatsFilter.filterConnect();

        avfilter.avfilter_graph_config(graph,null);*/

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

        // TODO 滤镜字段区

        // 上下文
        // 输入
        /*AVFilter inputFilter = avfilter.avfilter_get_by_name("abuffer");
        AVFilterContext inputCtx = avfilter.avfilter_graph_alloc_filter(graph,inputFilter,"src");

        StringBuilder abufferParam = new StringBuilder()
                .append("channel_layout=").append(AudioChannelLayout.valueOf(
                        encoder.channelLayout()
                ).getName()).append(":")
                .append("channels=").append(encoder.channels()).append(":")
                .append("sample_rate=").append(encoder.sampleRate()).append(":")
                .append("sample_fmt=").append(encoder.sampleFormat().name());

        avfilter.avfilter_init_str(inputCtx,abufferParam.toString());

        // AMix

        AVFilter mixFilter = avfilter.avfilter_get_by_name("amix");
        AVFilterContext mixContext = avfilter.avfilter_graph_alloc_filter(graph,mixFilter,"mixer");
        StringBuilder amixParams = new StringBuilder()
                .append("inputs=").append(1);

        avfilter.avfilter_init_str(mixContext,amixParams.toString());*/

        // 输出



        /*AVFilter formatFilter = avfilter.avfilter_get_by_name("aformat");
        AVFilterContext formatCtx = avfilter.avfilter_graph_alloc_filter(graph,formatFilter,"format");
        StringBuilder formatParam = new StringBuilder()
                .append("channel_layouts=").append(AudioChannelLayout.valueOf(
                        encoder.channelLayout()
                ).getName()).append(":")
                .append("sample_fmts=").append(encoder.sampleFormat().name()).append(":")
                .append("sample_rates=").append(encoder.sampleRate());

        avfilter.avfilter_init_str(formatCtx, formatParam.toString());

        AVFilter outFilter = avfilter.avfilter_get_by_name("abuffersink");
        AVFilterContext outCtx = avfilter.avfilter_graph_alloc_filter(graph,outFilter,"dst");
        avfilter.avfilter_init_dict(outCtx,(AVDictionary) null);

        avfilter.avfilter_link(inputCtx,0,mixContext,0);
        avfilter.avfilter_link(mixContext,0,formatCtx,0);
        avfilter.avfilter_link(formatCtx,0,outCtx,0);*/

        // TODO 滤镜字段区结束

        //FFAudioBufferFilter sourceFilter = mixer.refInputFilter(this);
        //FFAudioSinkFilter sinkFilter = mixer.getSinkFilter();

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

                        /*AVFrame frameTrans = avutil.av_frame_alloc();
                        frameTrans.sample_rate(out.sample_rate());
                        frameTrans.nb_samples(out.nb_samples());
                        frameTrans.format(out.format());
                        frameTrans.ch_layout(out.ch_layout());
                        avfilter.av_buffersrc_add_frame(sourceFilter.context(),out);*/

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

                        //avutil.av_frame_get_buffer(frameTrans,1);
                        // 转码完毕，开始编码，这一步的目的是将转码后的数据进行编码以便输出到文件。

                        /*while (avfilter.av_buffersink_get_frame(sinkFilter.context(),frameTrans) >= 0) {
                            frameTrans.pts(out.pts());
                            frameTrans.pkt_dts(out.pkt_dts());
                            // 开始编码
                            encoder.encodeFrame(frameTrans, encoded -> {
                                if (state == RecorderState.STOPPED) {
                                    avcodec.av_packet_unref(packet);
                                    return;
                                }
                                target.writeMediaPacket(encoded,MediaType.MediaTypeAudio);

                            });
                            //avutil.av_frame_unref(frameTrans);
                            count.set(count.get() + 1);
                        }*/


                    });

                    /*swrContext.convert(frame, out -> {
                        // 转码完毕，开始编码，这一步的目的是将转码后的数据进行编码以便输出到文件。

                        // 开始编码


                        encoder.encodeFrame(out, encoded -> {
                            if (state == RecorderState.STOPPED) {
                                avcodec.av_packet_unref(packet);
                                return;
                            }
                            target.writeMediaPacket(encoded,MediaType.MediaTypeAudio);

                        });
                        count.set(count.get() + 1);
                    });*/
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
