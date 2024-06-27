package org.swdc.recorder.views.controllers;

import jakarta.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.slf4j.Logger;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.ViewController;
import org.swdc.recorder.core.*;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.core.ffmpeg.FFOutputTarget;
import org.swdc.recorder.core.ffmpeg.FFRecordSource;
import org.swdc.recorder.core.ffmpeg.MediaType;
import org.swdc.recorder.views.MainView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * 这是View的一个Controller。
 * 请不要向Controller注入自己的View，如果需要得到view请使用getView方法。
 * 你需要在javaFX的fxml里面填写本Class的全限定名，这样Controller就会在加载View
 * 的时候得以创建并且附加到View里面。
 *
 * 此外，如果你的View的注解中的multiple被指定为TRUE，
 * 你应该为Controller标注Prototype注解。
 *
 * 请务必不要重写javafx.fxml.Initializable的initialize方法，需要初始化的话，
 * 请使用viewReady。
 *
 */
public class MainController extends ViewController<MainView> {

    @Inject
    private Logger logger;

    @Inject
    private Fontawsome5Service fontawsome5Service;

    @FXML
    private ComboBox<FFRecordSource> cbxAudioSource;

    @FXML
    private ComboBox<FFRecordSource> cbxVideoSource;

    @FXML
    private ComboBox<RecordVideoQuality> cbxBitrate;

    @FXML
    private ComboBox<RecordAudioQuality> cbxSamplerate;

    @FXML
    private ComboBox<RecordOutputFormat> cbxAudioOut;

    @FXML
    private ComboBox<RecordOutputFormat> cbxVideoOut;

    @FXML
    private Button recButton;

    @Inject
    private DesktopRecorder recorder = null;

    @Override
    protected void viewReady(URL url, ResourceBundle resourceBundle) {

        cbxAudioSource.getItems().addAll(
                FFMpegUtils.getAudioSources()
        );

        cbxAudioSource.getSelectionModel().select(0);

        cbxVideoSource.getItems().addAll(
                FFMpegUtils.getVideoSources()
        );
        cbxVideoSource.getSelectionModel().select(0);

        cbxBitrate.getItems().addAll(
                RecordVideoQuality.videoQualities()
        );
        cbxBitrate.getSelectionModel().select(0);

        cbxSamplerate.getItems().addAll(
                RecordAudioQuality.audioQualities()
        );
        cbxSamplerate.getSelectionModel().select(0);

        cbxAudioOut.getItems().addAll(
                RecordOutputFormat.outputFormats(MediaType.MediaTypeAudio)
        );
        cbxAudioOut.getSelectionModel().select(0);

        cbxVideoOut.getItems().addAll(
                RecordOutputFormat.outputFormats(MediaType.MediaTypeVideo)
        );
        cbxVideoOut.getSelectionModel().select(0);

        setRecIcon("play");
    }

    private void setRecIcon(String icon) {
        recButton.setFont(fontawsome5Service.getSolidFont(FontSize.MIDDLE));
        recButton.setPadding(new Insets(4));
        recButton.setText(fontawsome5Service.getFontIcon(icon));
    }

    @FXML
    public void onRecClicked() {

        if (recorder.isRecording()) {
            recorder.stop();
            setRecIcon("play");
        } else {

            RecordVideoQuality videoQuality = cbxBitrate.getSelectionModel().getSelectedItem();
            RecordOutputFormat videoOutFormat = cbxVideoOut.getSelectionModel().getSelectedItem();
            if (videoQuality == null || videoOutFormat == null) {
                return;
            }

            RecordAudioQuality audioQuality = cbxSamplerate.getSelectionModel().getSelectedItem();
            RecordOutputFormat audioOutFormat = cbxAudioOut.getSelectionModel().getSelectedItem();
            if (audioQuality == null || audioOutFormat == null) {
                return;
            }

            FFLogCallback callback = FFLogCallback.getLogger();
            callback.parseDeviceList(false);

            FFRecordSource audio = cbxAudioSource.getSelectionModel().getSelectedItem();
            FFRecordSource video = cbxVideoSource.getSelectionModel().getSelectedItem();
            if (audio == null || video == null) {
                return;
            }

            FFOutputTarget target = new FFOutputTarget(new File("test.mp4"));
            recorder.setTarget(target);
            recorder.setAudioOutput(audioOutFormat);
            recorder.setAudioQuality(audioQuality);
            recorder.setVideoOutput(videoOutFormat);
            recorder.setVideoQuality(videoQuality);
            recorder.setAudioSource(audio);
            recorder.setVideoSource(video);

            if (!recorder.start()) {
                // failed to start
            } else {
                setRecIcon("stop");
            }
        }

    }

}
