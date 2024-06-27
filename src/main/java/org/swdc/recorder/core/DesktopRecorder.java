package org.swdc.recorder.core;

import jakarta.inject.Singleton;
import org.swdc.recorder.core.ffmpeg.*;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class DesktopRecorder {

    private FFAudioRecorder audioRecorder;

    private FFVideoRecorder videoRecorder;

    private FFOutputTarget target;

    private boolean isRecording = false;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0, TimeUnit.MINUTES,new LinkedBlockingDeque<>());

    public DesktopRecorder() {

        audioRecorder = new FFAudioRecorder();
        videoRecorder = new FFVideoRecorder();

    }

    public void setTarget(FFOutputTarget target) {
        this.target = target;
    }

    public FFOutputTarget getTarget() {
        return target;
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
    }

    public void setVideoOutput(RecordOutputFormat format) {
        videoRecorder.setVideoCodecs(format.getVideoCodecs());
        videoRecorder.setEncodePixFormat(format.getPixFormat());
    }

    public void setVideoQuality(RecordVideoQuality quality) {
        videoRecorder.setBitRate(quality.getBitRate());
    }

    public void setAudioQuality(RecordAudioQuality quality) {
        audioRecorder.setSampleRate(quality.getSampleRate());
    }

    public boolean start() {

        if (target == null) {
            return false;
        }

        if (isRecording) {
            return true;
        }

        boolean doRecAudio = canRecordAudio();
        boolean doRecVideo = canRecordVideo();

        int latchCount = 0;

        if (doRecAudio) {
            latchCount ++;
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


    public void dispose(){
        stop();
        executor.shutdown();
    }

}
