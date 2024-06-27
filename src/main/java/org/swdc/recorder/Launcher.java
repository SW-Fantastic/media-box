package org.swdc.recorder;


import org.bytedeco.ffmpeg.global.avutil;
import org.swdc.recorder.core.FFLogCallback;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;

/**
 * 启动类。
 * 应用程序从这里启动，就是一个Main方法。
 */
public class Launcher {



    public static void main(String[] args)  {

        avutil.setLogCallback(FFLogCallback.getLogger());
        RecorderApplication application = new RecorderApplication();
        application.applicationLaunch(args);

    }

}
