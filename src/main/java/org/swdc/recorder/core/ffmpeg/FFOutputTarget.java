package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FFOutputTarget implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FFOutputTarget.class);

    private File targetFile;

    private AVOutputFormat outputFormat;

    private AVFormatContext outputContext;

    private AVStream videoOutputSteam;

    private AVStream audioOutputSteam;

    private boolean opend = false;

    private transient boolean targetHasClosed = false;

    public FFOutputTarget(File targetFile) {

        this.targetFile = targetFile.getAbsoluteFile();

    }


    private boolean contextOpen() {
        return outputContext != null && !outputContext.isNull() &&
                outputFormat != null && !outputFormat.isNull();
    }

    public synchronized boolean openOutput(FFMpegEncoder encoder) {

        int state = 0;

        if (opend || targetHasClosed) {
            // Header已经写入，不能创建新的输出了，或者本对象已经关闭。
            return false;
        }

        if (!contextOpen()) {
            // Output过程中必要的Context没有打开，首先需要打开对应的Context
            outputFormat = avformat.av_guess_format(null,targetFile.getAbsolutePath(),null);
            if (outputFormat == null) {
                logger.error("can not get output format");
                return false;
            }

            outputContext = avformat.avformat_alloc_context();
            if (outputContext == null) {
                logger.error("failed to create output context");
                return false;
            }

            state = avformat.avformat_alloc_output_context2(outputContext,outputFormat,null, (String) null);
            if (state < 0) {
                logger.error("failed to alloc output context", FFMpegUtils.createException(state));
                return false;
            }

            if ((outputContext.flags() & avformat.AVFMT_NOFILE) == 0) {
                if (!this.targetFile.exists()) {
                    try {
                        this.targetFile.createNewFile();
                    } catch (Exception e) {
                        logger.error("can not create a new file: " + targetFile.getAbsolutePath(), e);
                        return false;
                    }
                }
                AVIOContext context = new AVIOContext();
                state = avformat.avio_open(context,targetFile.getAbsolutePath(),avformat.AVIO_FLAG_WRITE);
                if (state < 0) {
                    logger.error("failed to open output file", FFMpegUtils.createException(state));
                    return false;
                }
                outputContext.pb(context);
            }
        }

        if (encoder.mediaType() == MediaType.MediaTypeVideo) {

            videoOutputSteam = encoder.createSteam(outputContext);
            if (videoOutputSteam == null) {
                logger.error("failed to create out stream");
                return false;
            }
            encoder.transferParameterTo(videoOutputSteam.codecpar());

            return true;

        } else if (encoder.mediaType() == MediaType.MediaTypeAudio){

            audioOutputSteam = encoder.createSteam(outputContext);
            if (audioOutputSteam == null) {
                logger.error("failed to create out stream");
                return false;
            }
            encoder.transferParameterTo(audioOutputSteam.codecpar());

            return true;
        }

        return false;
    }

    public AVStream getVideoOutputSteam() {
        return videoOutputSteam;
    }

    public AVStream getAudioOutputSteam() {
        return audioOutputSteam;
    }

    public synchronized boolean writeMediaPacket(AVPacket packet, MediaType mediaType) {

        if (targetHasClosed) {
            return false;
        }

        int state = 0;

        if (contextOpen()) {

            if (!opend) {

                state = avformat.avformat_write_header(outputContext,(AVDictionary) null);
                if (state < 0) {
                    logger.error("failed to write header", FFMpegUtils.createException(state));
                    return false;
                }
                opend = true;

            }

            if (mediaType == MediaType.MediaTypeVideo && videoOutputSteam != null && !videoOutputSteam.isNull()) {

                packet.stream_index(videoOutputSteam.index());
                state = avformat.av_write_frame(outputContext,packet);
                if (state < 0) {
                    logger.error("failed to write a frame : ", FFMpegUtils.createException(state));
                    return false;
                }
                return true;

            } else if (mediaType == MediaType.MediaTypeAudio && audioOutputSteam != null && !audioOutputSteam.isNull() ){

                packet.stream_index(audioOutputSteam.index());
                state = avformat.av_write_frame(outputContext,packet);
                if (state < 0) {
                    logger.error("failed to write a frame : ", FFMpegUtils.createException(state));
                    return false;
                }
                return true;
            }

        }

        return false;
    }


    @Override
    public void close() {

        if (opend) {
            avformat.av_write_trailer(outputContext);
            opend = false;
            targetHasClosed = true;
        }

        if (videoOutputSteam != null && !videoOutputSteam.isNull()) {
            videoOutputSteam = null;

        }
        if (outputContext != null && !outputContext.isNull()) {
            avformat.avformat_free_context(outputContext);
            AVIOContext context = outputContext.pb();
            avformat.avio_close(context);
            outputContext = null;

        }
        if (outputFormat != null && !outputFormat.isNull()) {

            outputFormat.close();
            outputFormat = null;

        }
    }
}
