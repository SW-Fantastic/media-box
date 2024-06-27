package org.swdc.recorder.core;

import org.swdc.recorder.core.ffmpeg.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecordOutputFormat {

    private MediaType mediaType;

    private VideoPixFormat pixFormat;

    private AudioSampleFormat sampleFormat;

    private AudioCodecs audioCodecs;

    private VideoCodecs videoCodecs;

    private String name;

    RecordOutputFormat(String name, VideoPixFormat format, VideoCodecs codecs) {
        this.name = name;
        this.pixFormat = format;
        this.mediaType = MediaType.MediaTypeVideo;
        this.videoCodecs = codecs;
    }

    RecordOutputFormat(String name, AudioSampleFormat sampleFormat, AudioCodecs codecs) {
        this.name = name;
        this.sampleFormat = sampleFormat;
        this.mediaType = MediaType.MediaTypeAudio;
        this.audioCodecs = codecs;
    }

    public AudioSampleFormat getSampleFormat() {
        return sampleFormat;
    }

    public void setSampleFormat(AudioSampleFormat sampleFormat) {
        this.sampleFormat = sampleFormat;
    }

    public VideoPixFormat getPixFormat() {
        return pixFormat;
    }

    public void setPixFormat(VideoPixFormat pixFormat) {
        this.pixFormat = pixFormat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public VideoCodecs getVideoCodecs() {
        return videoCodecs;
    }

    public void setVideoCodecs(VideoCodecs videoCodecs) {
        this.videoCodecs = videoCodecs;
    }

    public AudioCodecs getAudioCodecs() {
        return audioCodecs;
    }

    public void setAudioCodecs(AudioCodecs audioCodecs) {
        this.audioCodecs = audioCodecs;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<RecordOutputFormat> outputFormats(MediaType type) {
        return switch (type) {
            case MediaTypeVideo -> Arrays.asList(
                    new RecordOutputFormat("Mp4 H.264", VideoPixFormat.yuv420p, VideoCodecs.h264)
            );
            case MediaTypeAudio -> Arrays.asList(
                    new RecordOutputFormat("AAC", AudioSampleFormat.fltp, AudioCodecs.aac),
                    new RecordOutputFormat("MP3", AudioSampleFormat.s16p, AudioCodecs.mp3)
            );
            default -> Collections.emptyList();
        };
    }

}
