package org.swdc.recorder.views.controllers;

import jakarta.inject.Inject;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.ViewController;
import org.swdc.recorder.RecorderConfiguration;
import org.swdc.recorder.core.*;
import org.swdc.recorder.core.ffmpeg.FFMpegUtils;
import org.swdc.recorder.core.ffmpeg.source.FFRecordSource;
import org.swdc.recorder.core.ffmpeg.MediaType;
import org.swdc.recorder.views.ConfigView;
import org.swdc.recorder.views.MainView;
import org.swdc.recorder.views.RecordsView;

import java.io.File;
import java.net.URL;
import java.util.List;
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
    private ComboBox<FFRecordSource> cbxAudioSecondSource;

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

    @FXML
    private TextField txtFileName;

    @FXML
    private Slider slVol;

    @FXML
    private Slider slVolSecond;

    @Inject
    private RecordsView recordsView;

    @Inject
    private ConfigView configView;

    @Inject
    private RecorderConfiguration configuration;

    @Inject
    private DesktopRecorder recorder = null;

    private List<FFRecordSource> audioSources;

    private List<FFRecordSource> videoSources;

    @Override
    protected void viewReady(URL url, ResourceBundle resourceBundle) {


        slVol.setMax(1);
        slVol.setMin(0);
        slVol.setValue(0.5);
        slVol.valueProperty().addListener(this::volMainChanged);

        slVolSecond.setMax(1);
        slVolSecond.setMin(0);
        slVolSecond.setValue(0.5);
        slVolSecond.valueProperty().addListener(this::volSecondChanged);

        txtFileName.setText("录制-" + System.currentTimeMillis());
        setRecIcon("play");

        refreshDevices();

        cbxAudioSource.getSelectionModel().selectedItemProperty()
                .addListener(this::sourceChanged);

    }



    private void sourceChanged(Observable value) {

        FFRecordSource source = cbxAudioSource.getSelectionModel().getSelectedItem();
        List<FFRecordSource> sources = audioSources.stream()
                .filter(s -> s != source || s.getFormat() == null)
                .toList();

        ObservableList<FFRecordSource> secondSources = cbxAudioSecondSource.getItems();
        FFRecordSource secondSelected = cbxAudioSecondSource.getSelectionModel().getSelectedItem();
        secondSources.clear();
        secondSources.addAll(sources);
        if (sources.contains(secondSelected)) {
            cbxAudioSecondSource.getSelectionModel().select(secondSelected);
        }

    }


    private void volSecondChanged(Observable observable) {

        recorder.setVolumeSecondary(slVolSecond.getValue());

    }

    private void volMainChanged(Observable observable) {

        recorder.setVolumeMain(slVol.getValue());

    }

    private void setRecIcon(String icon) {
        recButton.setFont(fontawsome5Service.getSolidFont(FontSize.MIDDLE));
        recButton.setPadding(new Insets(4));
        recButton.setText(fontawsome5Service.getFontIcon(icon));
    }

    @FXML
    public void showConfigView() {

        Stage stage = configView.getStage();
        if (stage.isShowing()) {
            stage.toFront();
        } else {
            stage.show();
        }

    }

    @FXML
    public void showRecords() {

        Stage stage = recordsView.getStage();
        if (stage.isShowing()) {
            stage.toFront();
        } else {
            stage.show();
        }

    }

    @FXML
    public void onRecClicked() {

        if (recorder.isRecording()) {
            recorder.stop();
            setRecIcon("play");
            txtFileName.setEditable(true);
        } else {

            RecordVideoQuality videoQuality = cbxBitrate.getSelectionModel().getSelectedItem();
            RecordOutputFormat videoOutFormat = cbxVideoOut.getSelectionModel().getSelectedItem();

            RecordAudioQuality audioQuality = cbxSamplerate.getSelectionModel().getSelectedItem();
            RecordOutputFormat audioOutFormat = cbxAudioOut.getSelectionModel().getSelectedItem();

            FFLogCallback callback = FFLogCallback.getLogger();
            callback.parseDeviceList(false);

            FFRecordSource audioMain = cbxAudioSource.getSelectionModel().getSelectedItem();
            FFRecordSource audioSecond = cbxAudioSecondSource.getSelectionModel().getSelectedItem();

            FFRecordSource video = cbxVideoSource.getSelectionModel().getSelectedItem();

            if (audioQuality != null && audioOutFormat != null) {
                recorder.setAudioQuality(audioQuality);
                recorder.setAudioOutput(audioOutFormat);
            }

            recorder.setTarget(new File(configuration.getVideoFolder()).getAbsoluteFile(), txtFileName.getText());
            if (audioMain != null && audioMain.getFormat() != null) {
                recorder.setAudioSource(audioMain);
            }
            if (audioSecond != null && audioSecond.getFormat() != null) {
                recorder.setAudioSecondarySource(audioSecond);
            }

            if (video != null && videoQuality != null && videoOutFormat != null) {
                recorder.setVideoOutput(videoOutFormat);
                recorder.setVideoQuality(videoQuality);
                recorder.setVideoSource(video);
            }

            if (recorder.canRecordAudioMain() || recorder.canRecordVideo()) {
                if (!recorder.start()) {
                    // failed to start
                    Alert alert = getView().alert("失败", "视频录制启动失败，未知错误。", Alert.AlertType.ERROR);
                    alert.showAndWait();
                } else {
                    setRecIcon("stop");
                    txtFileName.setEditable(false);
                }
            }

        }

    }

    public void refreshDevices() {

        FFRecordSource emptySource = new FFRecordSource();
        emptySource.setDeviceName("不录制音频");
        emptySource.setType(MediaType.MediaTypeAudio);

        List<FFRecordSource> audioSources = FFMpegUtils.getAudioSources();
        audioSources.add(emptySource);
        this.audioSources = audioSources;

        ObservableList<FFRecordSource> audioSourceItems = cbxAudioSource.getItems();
        audioSourceItems.clear();
        audioSourceItems.addAll(audioSources);
        cbxAudioSource.getSelectionModel().select(0);

        sourceChanged(null);

        List<FFRecordSource> videoSources = FFMpegUtils.getVideoSources();
        this.videoSources = videoSources;

        ObservableList<FFRecordSource> videoSourceItems = cbxVideoSource.getItems();
        videoSourceItems.clear();
        videoSourceItems.addAll(videoSources);
        cbxVideoSource.getSelectionModel().select(0);

        ObservableList<RecordVideoQuality> videoQualities = cbxBitrate.getItems();
        videoQualities.clear();
        videoQualities.addAll(RecordVideoQuality.videoQualities());
        cbxBitrate.getSelectionModel().select(0);

        ObservableList<RecordAudioQuality> audioQualities = cbxSamplerate.getItems();
        audioQualities.clear();
        audioQualities.addAll(
                RecordAudioQuality.audioQualities()
        );
        cbxSamplerate.getSelectionModel().select(0);

        ObservableList<RecordOutputFormat> audioOuts = cbxAudioOut.getItems();
        audioOuts.clear();
        audioOuts.addAll(
                RecordOutputFormat.outputFormats(MediaType.MediaTypeAudio)
        );
        cbxAudioOut.getSelectionModel().select(0);

        ObservableList<RecordOutputFormat> videoOuts = cbxVideoOut.getItems();
        videoOuts.clear();
        videoOuts.addAll(
                RecordOutputFormat.outputFormats(MediaType.MediaTypeVideo)
        );
        cbxVideoOut.getSelectionModel().select(0);
    }

}
