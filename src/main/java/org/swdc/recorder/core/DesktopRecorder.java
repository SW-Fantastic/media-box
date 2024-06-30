package org.swdc.recorder.core;

import jakarta.inject.Singleton;
import org.swdc.recorder.core.ffmpeg.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class DesktopRecorder {

    private FFAudioRecorder audioRecorder;

    private FFVideoRecorder videoRecorder;

    private FFOutputTarget target;

    private File recordFolder;

    private String fileName;

    private boolean isRecording = false;

    private Map<MediaType,RecordOutputFormat> outputFormats = new HashMap<>();

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(3,3,0, TimeUnit.MINUTES,new LinkedBlockingDeque<>());

    public DesktopRecorder() {

        audioRecorder = new FFAudioRecorder();
        videoRecorder = new FFVideoRecorder();

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
        audioRecorder.setSampleRate(quality.getSampleRate());
    }

    public boolean start() {

        if (recordFolder == null || fileName == null || fileName.isBlank()) {
            return false;
        }

        if (isRecording) {
            return true;
        }

        boolean doRecAudio = canRecordAudio();
        boolean doRecVideo = canRecordVideo();

        String extension = getOutputFileExtension();
        target = new FFOutputTarget(new File(recordFolder.getAbsolutePath() + File.separator + fileName + "." + extension));

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
