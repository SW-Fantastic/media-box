package org.swdc.recorder.core.ffmpeg;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.Callback_Pointer_int_String_Pointer;
import org.bytedeco.ffmpeg.avutil.LogCallback;
import org.bytedeco.ffmpeg.global.avdevice;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;
import org.swdc.recorder.core.FFLogCallback;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FFMpegUtils {


    public static List<FFRecordSource> listAVFoundation() {

        FFLogCallback callback = FFLogCallback.getLogger();
        callback.clearAVFoundationList();

        avdevice.avdevice_register_all();
        AVInputFormat avfoundation = avformat.av_find_input_format("avfoundation");

        if (avfoundation != null && !avfoundation.isNull()) {
            callback.parseDeviceList(true);
            // 打开AVFoundation设备，用以输出AVFoundation的可用设备列表到Logger。
            AVFormatContext context =  avformat.avformat_alloc_context();
            AVDictionary dict = new AVDictionary();
            avutil.av_dict_set(dict,"list_devices", "true",0);
            avformat.avformat_open_input(context,"",avfoundation,dict);
            avformat.avformat_close_input(context);
            avutil.av_dict_free(dict);

            List<FFRecordSource> sources = new ArrayList<>();
            sources.addAll(callback.getAvfoundationAudioDevice().stream().map(d -> {
                String index = d.substring(d.indexOf("["), d.indexOf("]"));
                FFRecordSource source = new FFRecordSource();
                source.setDeviceName(d);
                source.setUrl(index);
                source.setFormat(avfoundation);
                source.setType(MediaType.MediaTypeAudio);
                return source;
            }).toList());

            sources.addAll(callback.getAvfoundationVideoDevice().stream().map(d -> {
                String index = d.substring(d.indexOf("["), d.indexOf("]"));
                FFRecordSource source = new FFRecordSource();
                source.setDeviceName(d);
                source.setUrl(index);
                source.setFormat(avfoundation);
                source.setType(MediaType.MediaTypeVideo);
                return source;
            }).toList());

            return sources;
        }

        return Collections.emptyList();
    }

    public static List<FFRecordSource> listDshows() {

        avdevice.avdevice_register_all();
        // 清除Dshow列表
        FFLogCallback callback = FFLogCallback.getLogger();
        callback.clearDshowList();

        AVInputFormat dshow = avformat.av_find_input_format("dshow");
        if (dshow != null && !dshow.isNull()) {
            callback.parseDeviceList(true);
            // 打开Dshow设备，用以输出Dshow的可用设备列表到Logger。
            AVFormatContext context =  avformat.avformat_alloc_context();
            AVDictionary dict = new AVDictionary();
            avutil.av_dict_set(dict,"list_devices", "true",0);
            avformat.avformat_open_input(context,"video=dummy",dshow,dict);
            avformat.avformat_close_input(context);
            avutil.av_dict_free(dict);
        }

        return callback.getDshowDevices().stream().map( s -> {
            int begin = s.indexOf("\"");
            int end = s.lastIndexOf("\"");
            String name = s.substring(begin,end + 1);
            MediaType mediaType = null;
            String url = "";
            if (s.contains("audio")) {
                url = "audio=" + name.replace("\"","");
                mediaType = MediaType.MediaTypeAudio;
            } else {
                url = "video=" + name.replace("\"","");
                mediaType = MediaType.MediaTypeVideo;
            }

            FFRecordSource source = new FFRecordSource();
            source.setDeviceName(name);
            source.setType(mediaType);
            source.setFormat(dshow);
            source.setUrl(url);

            return source;
        }).collect(Collectors.toList());

    }


    public static List<FFRecordSource> getVideoSources() {

        List<FFRecordSource> sources = new ArrayList<>();

        avdevice.avdevice_register_all();
        AVInputFormat format = null;
        // av_input_video_device_next是用于遍历AVInputFormat的函数，功能和对应c语言函数一致。
        while ((format = avdevice.av_input_video_device_next(format)) != null) {
            // 读取Format的name。
            String name = format.name().getString();
            if (name.equals("dshow")) {
                // windows dshow
                avutil.setLogCallback(FFLogCallback.getLogger());
                sources.addAll(listDshows()
                        .stream()
                        .filter(ds -> ds.getType() == MediaType.MediaTypeVideo)
                        .toList()
                );
            } else if (name.equals("gdigrab")) {
                // windows gdigrab
                FFRecordSource source = new FFRecordSource();
                source.setUrl("desktop");
                source.setFormat(format);
                source.setType(MediaType.MediaTypeVideo);
                source.setDeviceName("gdigrab");
                sources.add(source);
            } else if (name.equals("avfoundation")) {
                // macos avfoundation
                avutil.setLogCallback(FFLogCallback.getLogger());
                sources.addAll(listAVFoundation()
                        .stream()
                        .filter(ds -> ds.getType() == MediaType.MediaTypeVideo)
                        .toList()
                );
            } else if (name.equals("x11grab")) {
                // linux x11grab
                // TODO 暂时不会写这个。
            }
        }

        return sources;
    }

    public static List<FFRecordSource> getAudioSources() {

        List<FFRecordSource> sources = new ArrayList<>();

        avdevice.avdevice_register_all();

        // 尝试打开Windows的DShow
        AVInputFormat dshow = avformat.av_find_input_format("dshow");
        if (dshow != null && !dshow.isNull()) {
            avutil.setLogCallback(FFLogCallback.getLogger());
            sources.addAll(listDshows()
                    .stream()
                    .filter(ds -> ds.getType() == MediaType.MediaTypeAudio)
                    .toList()
            );
            dshow.close();
        }

        // 尝试打开MacOS的AVFoundation
        AVInputFormat avFoundation = avformat.av_find_input_format("avfoundation");
        if(avFoundation != null && !avFoundation.isNull()) {
            avutil.setLogCallback(FFLogCallback.getLogger());
            sources.addAll(listAVFoundation()
                    .stream()
                    .filter(ds -> ds.getType() == MediaType.MediaTypeAudio)
                    .toList()
            );
        }

        return sources;
    }


    /**
     * 从Context中检索一个视频数据流。
     * @param context 音视频格式上下文
     * @return 视频数据流，如果没有视频流，则会返回空
     */
    public static AVStream findInputAVSteam(AVFormatContext context, MediaType type) {
        int steams = context.nb_streams();
        for (int idx = 0; idx < steams; idx ++ ) {
            AVStream stream = context.streams(idx);
            if (stream.codecpar().codec_type() == type.getFFMpegType()) {
                return stream;
            }
        }
        return null;
    }


    /**
     * 将FFMpeg的异常转换为JavaException
     * @param errCode 异常编码
     * @return 运行时异常
     */
    public static RuntimeException createException(int errCode) {
        byte[] err = new byte[128];
        avutil.av_make_error_string(err,err.length - 1,errCode);
        for (int idx = err.length - 1; idx > 0; idx --) {
            if (err[idx] != 0) {
                return new RuntimeException(new String(err,0,idx + 1));
            }
        }
        return new RuntimeException(new String(err, StandardCharsets.UTF_8));
    }

}
