package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avdevice;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class FFVideoRecorder implements AutoCloseable {

    private static final String pixelFormatKey = "pixel_format";

    private static Logger logger = LoggerFactory.getLogger(FFVideoRecorder.class);

    private VideoPixFormat captureFormat = VideoPixFormat.yuv420p;

    private VideoPixFormat encodePixFormat = VideoPixFormat.yuv420p;

    private int bitRate = 6000000;

    /**
     * 视频输入格式
     */
    private AVInputFormat sourceFormat;

    /**
     * 视频输入格式上下文
     */
    private AVFormatContext sourceFormatCtx;

    /**
     * 视频输入流
     */
    private AVStream sourceVideoSteam;

    /**
     * 视频流解码器
     */
    private FFMpegDecoder decoder;

    /**
     * 视频流编码器
     */
    private FFMpegEncoder encoder;

    /**
     * 视频的格式转换器
     */
    private FFSwsContext swsContext;


    private FFOutputTarget target;

    private VideoCodecs videoCodecs;

    private CountDownLatch stateLock;

    private RecorderState state = RecorderState.STOPPED;

    private FFRecordSource source;

    public FFVideoRecorder() {
    }

    public void setVideoCodecs(VideoCodecs videoCodecs) {
        this.videoCodecs = videoCodecs;
    }

    public VideoCodecs getVideoCodecs() {
        return videoCodecs;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public FFOutputTarget getTarget() {
        return target;
    }

    public void setTarget(FFOutputTarget target) {
        this.target = target;
    }

    public void setCaptureFormat(VideoPixFormat captureFormat) {
        this.captureFormat = captureFormat;
    }

    public VideoPixFormat getCaptureFormat() {
        return captureFormat;
    }

    public String getCapturePixelFormatName() {
        return getCaptureFormat().name();
    }

    public VideoPixFormat getEncodePixFormat() {
        return encodePixFormat;
    }

    public void setEncodePixFormat(VideoPixFormat encodePixFormat) {
        this.encodePixFormat = encodePixFormat;
    }

    public FFRecordSource getSource() {
        return source;
    }

    public void setSource(FFRecordSource source) {
        this.source = source;
    }


    /**
     * 初始化FFMpeg对象。
     * @return 是否成功。
     */
    public boolean openRecorderDevice() {

        if (state != RecorderState.STOPPED) {
            return true;
        }

        avdevice.avdevice_register_all();

        this.close();

        // 搜索输入设备
        sourceFormat = source.getFormat();
        if (sourceFormat == null) {
            logger.error("provided source does not have a input format");
            return false;
        }
        // 配置录屏选项
        AVDictionary dictionary = new AVDictionary();
        int state = 0;
        // 设定像素格式
        state = avutil.av_dict_set(dictionary,pixelFormatKey, getCapturePixelFormatName(), 1);
        if (state < 0) {
            logger.error("failed to setup options ", FFMpegUtils.createException(state));
            return false;
        }
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
            logger.warn("failed to open with special pixel format, try to open directly.");
            if (state < 0) {
                logger.error("failed to setup options ", FFMpegUtils.createException(state));
                return false;
            }
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
        }

        state = avformat.avformat_find_stream_info(sourceFormatCtx,(AVDictionary) null);
        if (state < 0) {
            logger.error("failed to fetch steam info");
            return false;
        }
        // 查找视频流
        sourceVideoSteam = FFMpegUtils.findInputAVSteam(sourceFormatCtx,MediaType.MediaTypeVideo);
        if (sourceVideoSteam == null) {
            // 视频流不存在
            logger.error("Can not found video source");
            return false;
        }

        // 初始化解码器
        // 从视频流的编码参数中可以得到编码器的Id，通过这个id能够找到解码器对象
        decoder = new FFMpegDecoder(sourceVideoSteam.codecpar().codec_id());
        decoder.loadParameterFrom(sourceVideoSteam.codecpar());

        // 初始化编码器
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        encoder = new FFMpegEncoder(videoCodecs.getCodecId());

        encoder.bitRate(getBitRate())
                .size(dimension.width,dimension.height)
                .groupSize(12)
                .pixFormat(encodePixFormat)
                .mediaType(MediaType.MediaTypeVideo)
                .timeBase(1,24)
                .maxBFrameSize(3);

        if (!this.target.openOutput(encoder)) {
            logger.error("failed to open target file.");
            return false;
        }

        // 格式转换上下文
        swsContext = encoder.createVideoSwsContext(decoder);

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

        this.state = RecorderState.RECORDING;


        AtomicLong count = new AtomicLong(0);

        AtomicLong ptsLast = new AtomicLong(0);
        AtomicLong ptsDelta = new AtomicLong(0);

        // 初始化一个音视频数据包
        AVPacket packet = avcodec.av_packet_alloc();
        // 开始从视频流读取数据
        while (avformat.av_read_frame(sourceFormatCtx, packet) == 0) {
            if (state == RecorderState.PAUSED) {
                try {
                    stateLock = new CountDownLatch(1);
                    stateLock.await();
                    stateLock = null;
                } catch (Exception e) {
                }
                avcodec.av_packet_unref(packet);
                continue;
            }
            if (state == RecorderState.STOPPED) {
                avcodec.av_packet_unref(packet);
                break;
            }
            if (packet.stream_index() == sourceVideoSteam.index()) {

                // 读取到的是视频流的数据
                // 将这个数据包发送到解码器中（解码器是异步运行的）

                decoder.decodePacket(packet, frame -> {

                    // 计算时间差值
                    if (ptsLast.get() == 0) {
                        ptsLast.set(frame.pts());
                    } else {
                        ptsDelta.set(frame.pts() - ptsLast.get());
                        ptsLast.set(frame.pts());
                    }

                    // 解码完毕，开始转码，这一步的目的是将输入格式转码为输出的格式
                    AVFrame out = swsContext.scale(frame);

                    // 把视频帧的时间差从视频流的时间基转换为编码器的时间基。
                    long rescaledPts = avutil.av_rescale_q(
                            ptsDelta.get(),sourceVideoSteam.time_base(),encoder.timeBase()
                    );

                    // 使用累加器和本时间差作为PTS。
                    out.pts(count.get() + rescaledPts);
                    // 更新累加器。
                    count.set(count.get() + rescaledPts);
                    // 转码完毕，开始编码，这一步的目的是将转码后的数据进行编码以便输出到文件。
                    encoder.encodeFrame(out, encoded -> {
                        if (state == RecorderState.STOPPED) {
                            avcodec.av_packet_unref(packet);
                            return;
                        }
                        // 转换时间刻度（timeBase）
                        avcodec.av_packet_rescale_ts(
                                encoded,
                                encoder.timeBase(),
                                target.getVideoOutputSteam().time_base()
                        );
                        target.writeMediaPacket(encoded,MediaType.MediaTypeVideo);

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
            if (this.stateLock != null) {
                this.stateLock.countDown();
            }
            this.state = RecorderState.STOPPED;
        } else if (this.state == RecorderState.READY) {
            this.state = RecorderState.STOPPED;
        }
    }

    public void pause() {
        if (state == RecorderState.RECORDING) {
            this.state = RecorderState.PAUSED;
        } else if (state == RecorderState.READY) {
            this.state = RecorderState.PAUSED;
        }
    }

    public void resume() {
        if (this.state == RecorderState.PAUSED) {
            this.state = RecorderState.RECORDING;
            if (this.stateLock != null) {
                this.stateLock.countDown();
            }
        } else if (state == RecorderState.STOPPED) {
            throw new RuntimeException("can not resume from stopped");
        }
    }

    /**
     * 依次关闭各个本地对象。
     */
    @Override
    public void close() {
        if (sourceFormatCtx != null && !sourceFormatCtx.isNull()) {
            avformat.avformat_close_input(sourceFormatCtx);
            avformat.avformat_free_context(sourceFormatCtx);
            sourceFormat = null;
        }

        if (swsContext != null) {
            swsContext.close();
        }

        if (decoder != null) {
            decoder.close();
        }

        if (encoder != null) {
            encoder.close();
        }
    }
}
