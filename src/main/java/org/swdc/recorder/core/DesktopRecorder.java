package org.swdc.recorder.core;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.IntPointer;
import org.slf4j.Logger;
import org.swdc.recorder.core.ffmpeg.*;
import org.swdc.recorder.core.ffmpeg.convert.FFAudioMixer;
import org.swdc.recorder.core.ffmpeg.source.FFRecordSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class DesktopRecorder {

    @Inject
    private Logger logger;

    private FFAudioRecorder audioRecorder;

    private FFVideoRecorder videoRecorder;

    private AVCodecParameters mixParameter;

    private FFAudioMixer audioMixer;

    private FFOutputTarget target;

    private File recordFolder;

    private String fileName;

    private boolean isRecording = false;

    private Map<MediaType,RecordOutputFormat> outputFormats = new HashMap<>();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0, TimeUnit.MINUTES,new LinkedBlockingDeque<>());

    public DesktopRecorder() {

        audioMixer = new FFAudioMixer();
        audioRecorder = new FFAudioRecorder();
        videoRecorder = new FFVideoRecorder();
        mixParameter = avcodec.avcodec_parameters_alloc();

    }

    public void setTarget(File folder, String name) {
        this.recordFolder = folder;
        this.fileName = name;
    }


    public void setVideoSource(FFRecordSource source) {
        videoRecorder.setSource(source);
    }

    public void setAudioSource(FFRecordSource recordSource) {
        audioRecorder.setSource(recordSource);
    }

    public void setAudioOutput(RecordOutputFormat format) {

        audioRecorder.setAudioCodecs(format.getAudioCodecs());
        audioRecorder.setSampleFormat(format.getSampleFormat());

        AVChannelLayout layout = new AVChannelLayout();
        int state = avutil.av_channel_layout_from_mask(
                layout,AudioChannelLayout.layoutStereo.getFfmpegChannelLayout()
        );
        if (state < 0) {
            throw FFMpegUtils.createException(state);
        }
        mixParameter.format(format.getSampleFormat().getFfmpegFormatId());
        mixParameter.ch_layout(layout);

        outputFormats.put(MediaType.MediaTypeAudio,format);

    }

    public void setVideoOutput(RecordOutputFormat format) {
        videoRecorder.setVideoCodecs(format.getVideoCodecs());
        videoRecorder.setEncodePixFormat(format.getPixFormat());
        outputFormats.put(MediaType.MediaTypeVideo,format);
    }

    public void setVideoQuality(RecordVideoQuality quality) {
        videoRecorder.setBitRate(quality.getBitRate());
    }

    public void setAudioQuality(RecordAudioQuality quality) {
        mixParameter.sample_rate(quality.getSampleRate());
        audioRecorder.setSampleRate(quality.getSampleRate());
    }

    public boolean start() {

        if (recordFolder == null || fileName == null || fileName.isBlank()) {
            return false;
        }

        if (isRecording) {
            return true;
        }



        int numberOfSamples = avutil.av_samples_get_buffer_size(
                (IntPointer) null,
                mixParameter.ch_layout().nb_channels(),
                1,
                mixParameter.format(),
                1
        );
        mixParameter.frame_size(numberOfSamples);

        audioMixer.close();
        audioMixer.configure(mixParameter);

        boolean doRecAudio = canRecordAudio();
        boolean doRecVideo = canRecordVideo();

        String extension = getOutputFileExtension();
        target = new FFOutputTarget(new File(recordFolder.getAbsolutePath() + File.separator + fileName + "." + extension));

        int latchCount = 0;

        if (doRecAudio) {
            latchCount ++;
            audioRecorder.setMixer(audioMixer);
            audioRecorder.setTarget(target);
        }

        if (doRecVideo) {
            latchCount ++;
            videoRecorder.setTarget(target);
        }

        if (latchCount == 0) {
            // 这意味着不能录制任何东西。
            return false;
        }

        CountDownLatch latch = new CountDownLatch(latchCount);

        if(videoRecorder.openRecorderDevice() && audioRecorder.openRecorderDevice()) {


            executor.submit(() -> {

                try {

                    if (doRecAudio) {
                        executor.submit(() -> {
                            audioRecorder.record();
                            latch.countDown();
                        });
                    }
                    if (doRecVideo) {
                        executor.submit(() -> {
                            videoRecorder.record();
                            latch.countDown();
                        });
                    }

                    latch.await();

                } catch (Exception e) {
                }

                audioRecorder.close();
                videoRecorder.close();
                target.close();

                isRecording = false;

            });

            isRecording = true;
            return true;
        }

        return false;

    }

    public void stop() {

        if (isRecording) {

            audioRecorder.stop();
            videoRecorder.stop();
            outputFormats.clear();

        }

    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean canRecordAudio() {
        return audioRecorder.getAudioCodecs() != null &&
                audioRecorder.getSampleFormat() != null &&
                audioRecorder.getSource() != null;
    }

    public boolean canRecordVideo() {
        return videoRecorder.getVideoCodecs() != null &&
                videoRecorder.getEncodePixFormat() != null &&
                audioRecorder.getSource() != null;
    }


    public String getOutputFileExtension() {
        if (canRecordVideo()) {
            return outputFormats.get(MediaType.MediaTypeVideo).getExtension();
        } else if (canRecordAudio()) {
            return outputFormats.get(MediaType.MediaTypeAudio).getExtension();
        }
        return null;
    }

    public void dispose(){
        stop();
        executor.shutdown();
    }

}
