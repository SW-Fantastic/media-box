package org.swdc.recorder.core;

import org.bytedeco.ffmpeg.avutil.LogCallback;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swdc.recorder.core.ffmpeg.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FFLogCallback extends LogCallback {

    private static FFLogCallback callback = new FFLogCallback();

    private Pattern dshowLogPattern = Pattern.compile("\\[dshow @ [a-z|0-9]+]");
    private Pattern avFoundationPattern = Pattern.compile("\\[AVFoundation [a-z|A-Z]+ @ [a-z|0-9]+]");

    private MediaType mediaType;
    private List<String> avfoundationAudioDevice = new ArrayList<>();
    private List<String> avfoundationVideoDevice = new ArrayList<>();

    private List<String> dshowDevices = new ArrayList<>();

    private boolean parseDevice;

    private FFLogCallback() {

    }

    private static Logger logger = LoggerFactory.getLogger(avutil.class);

    private List<String> buffer = new ArrayList<>();

    public void parseDeviceList(boolean parseDevice) {
        this.parseDevice = parseDevice;
    }

    @Override
    public void call(int level, BytePointer msg) {

        String log = msg.getString();

        buffer.addLast(log);

        if (log.endsWith("\n")) {

            log = buffer.stream().collect(Collectors.joining()).replace("\n", "");

            if (log.isBlank()) {
                return;
            }

            if (parseDevice) {
                if (dshowLogPattern.matcher(log).find()) {
                    // Windows DShow
                    if (!log.contains("Alternative name")) {
                        // 在Windows上有时需要解析Dshow设备。
                        int devNameBegin = log.indexOf("\"");
                        dshowDevices.add(log.substring(devNameBegin));
                    }
                } else if (avFoundationPattern.matcher(log).find()) {
                    // MacOS AVFoundation
                    int infoBegin = log.indexOf(']');
                    String info = log.substring(infoBegin + 1);
                    if (info.strip().endsWith("video devices:")) {
                        mediaType = MediaType.MediaTypeVideo;
                        return;
                    } else if (info.strip().endsWith("audio devices:")) {
                        mediaType = MediaType.MediaTypeAudio;
                        return;
                    }
                    if (mediaType == MediaType.MediaTypeAudio) {
                        avfoundationAudioDevice.add(info);
                    } else if (mediaType == MediaType.MediaTypeVideo) {
                        avfoundationVideoDevice.add(info);
                    }
                }
                buffer.clear();
                return;
            }

            if (level == avutil.AV_LOG_INFO) {
                logger.info(log);
            } else if (level == avutil.AV_LOG_WARNING) {
                logger.warn(log);
            } else if (level == avutil.AV_LOG_ERROR) {
                logger.error(log);
            }
            buffer.clear();
        }

    }

    public static FFLogCallback getLogger() {
        return callback;
    }

    public List<String> getDshowDevices() {
        return dshowDevices;
    }

    public List<String> getAvfoundationAudioDevice() {
        return avfoundationAudioDevice;
    }

    public List<String> getAvfoundationVideoDevice() {
        return avfoundationVideoDevice;
    }

    public void clearDshowList() {
        dshowDevices.clear();
    }

    public void clearAVFoundationList() {
        avfoundationVideoDevice.clear();
        avfoundationAudioDevice.clear();
    }

}
