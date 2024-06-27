package org.swdc.recorder.core;

import org.swdc.recorder.core.ffmpeg.VideoPixFormat;

import java.util.Arrays;
import java.util.List;

public class RecordVideoQuality {

    private int bitRate;

    private String name;

    RecordVideoQuality(int bitRate, String name) {
        this.bitRate = bitRate;
        this.name = name;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<RecordVideoQuality> videoQualities() {
        return Arrays.asList(
                new RecordVideoQuality(8000000,"高质量"),
                new RecordVideoQuality(6000000,"标准质量"),
                new RecordVideoQuality(4000000,"低质量")
        );
    }

}
