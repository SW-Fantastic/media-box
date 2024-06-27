package org.swdc.recorder.core;

import org.swdc.recorder.core.ffmpeg.AudioSampleFormat;

import java.util.Arrays;
import java.util.List;

public class RecordAudioQuality {

    private String name;

    private int sampleRate;


    RecordAudioQuality(int sampleRate,String name) {
        this.name = name;
        this.sampleRate = sampleRate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<RecordAudioQuality> audioQualities() {
        return Arrays.asList(
                new RecordAudioQuality(48000,"标准质量"),
                new RecordAudioQuality(44100,"低质量")
        );
    }

}
