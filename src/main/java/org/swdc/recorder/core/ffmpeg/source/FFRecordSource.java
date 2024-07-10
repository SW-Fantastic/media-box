package org.swdc.recorder.core.ffmpeg.source;

import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.swdc.recorder.core.ffmpeg.MediaType;

public class FFRecordSource {

    private String deviceName;

    private MediaType type;

    private AVInputFormat format;

    private String url;

    public FFRecordSource() {

    }

    public void setFormat(AVInputFormat format) {
        this.format = format;
    }

    public AVInputFormat getFormat() {
        return format;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return deviceName.replace(
                "\"", ""
        );
    }
}

