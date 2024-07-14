package org.swdc.recorder.views.controllers;

import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;
import org.swdc.platforms.WindowsBinaryUtils;
import org.swdc.recorder.RecorderConfiguration;
import org.swdc.recorder.core.FFLogCallback;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.views.ConfigView;
import org.swdc.recorder.views.MainView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ConfigController extends ViewController<ConfigView> {

    @Inject
    private Logger logger;

    @Inject
    private FXResources resources;

    @Inject
    private RecorderConfiguration config;

    @Inject
    private MainView mainView;

    @FXML
    public void saveConfigs() {

        try {
            config.save();
            getView().hide();
        } catch (Exception e) {
            Alert alert = getView().alert("失败","无法存储配置文件。", Alert.AlertType.ERROR);
            alert.showAndWait();
            logger.error("failed to save config file", e);
        }

    }


    @FXML
    public void registerDshow() {

        List<String> arch64 = Arrays.asList(
                "amd64","x64","x86_64"
        );

        String videoRecorderName = "screen-capture-recorder";
        String audioRecorderName = "audio_sniffer";

        String osName = System.getProperty("os.name").trim().toLowerCase();
        if (!osName.contains("windows")) {
            Alert alert = getView().alert("提示", "DShow设备仅适用用于Windows，你的操作系统无法安装和使用此设备。", Alert.AlertType.WARNING);
            alert.showAndWait();
            return;
        }

        Alert tips = getView().alert("提示", "正在准备注册《video-capture-recorder》和《virtual-audio-recorder》" +
                "你可以使用它们录制屏幕以及系统声音，这个操作需要Windows管理员权限，" +
                "如果失败，请尝试以管理员身份运行本应用。",
                Alert.AlertType.INFORMATION
        );

        tips.showAndWait();

        String osArch = System.getProperty("os.arch");
        if (arch64.contains(osArch.toLowerCase())) {
            osArch = "-x64";
        } else {
            osArch = "";
        }

        File assetFolder = resources.getAssetsFolder();
        File dshowLocation = new File(assetFolder.getAbsolutePath() + File.separator + "platform/dshow");

        try {

            if (!dshowLocation.exists()) {
                dshowLocation.mkdirs();
            }

            String recorderName =  videoRecorderName + osArch + ".dll";
            File videoRecDevice = new File(dshowLocation.getAbsolutePath() + File.separator + recorderName);
            if (!videoRecDevice.exists()) {
                InputStream inputStream = getClass().getModule().getResourceAsStream("dshow/" + recorderName);
                FileOutputStream fos = new FileOutputStream(videoRecDevice);
                inputStream.transferTo(fos);
                fos.close();
                inputStream.close();
            }

            if(!WindowsBinaryUtils.dllRegister(videoRecDevice.getAbsoluteFile())) {
                Alert fail = getView().alert("失败", "无法注册DShow设备，请关闭本应用并以管理员权限运行。", Alert.AlertType.ERROR);
                fail.showAndWait();
                return;
            }

            String audioRecName = audioRecorderName + osArch + ".dll";
            File audioRecDevice = new File(dshowLocation.getAbsolutePath() + File.separator + audioRecName);
            if (!audioRecDevice.exists()) {
                InputStream inputStream = getClass().getModule().getResourceAsStream("dshow/" + audioRecName);
                FileOutputStream fos = new FileOutputStream(audioRecDevice);
                inputStream.transferTo(fos);
                fos.close();
                inputStream.close();
            }

            if (!WindowsBinaryUtils.dllRegister(audioRecDevice)) {
                Alert fail = getView().alert("失败", "无法注册DShow设备，请关闭本应用并以管理员权限运行。", Alert.AlertType.ERROR);
                fail.showAndWait();
                return;
            }

            FFLogCallback callback = FFLogCallback.getLogger();
            callback.clearDshowList();
            callback.parseDeviceList(true);
            FFMpegUtils.listDshows();

            mainView.refreshDevices();
            Alert fail = getView().alert("提示", "设备已经注册完成，如果你找不到它们，请重新启动本应用。", Alert.AlertType.INFORMATION);
            fail.showAndWait();

        } catch (Exception e) {
            logger.error("failed to register dshow devices", e);
            Alert fail = getView().alert("失败", "无法注册DShow设备，未知错误。", Alert.AlertType.ERROR);
            fail.showAndWait();
        }



    }


}
