package org.swdc.recorder.views.controllers;

import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.GridView;
import org.slf4j.Logger;
import org.swdc.fx.FXResources;
import org.swdc.fx.view.ViewController;
import org.swdc.recorder.RecorderConfiguration;
import org.swdc.recorder.core.FileUtils;
import org.swdc.recorder.views.EditFolderView;
import org.swdc.recorder.views.RecordsView;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RecordsController extends ViewController<RecordsView> {

    @FXML
    private Button btnForward;

    @FXML
    private Button btnBack;

    @FXML
    private Button btnTrash;

    @Inject
    private RecorderConfiguration configuration;

    @Inject
    private FXResources resources;

    @Inject
    private Logger logger;

    private ArrayDeque<File> previousPath = new ArrayDeque<>();

    private ArrayDeque<File> nextPath = new ArrayDeque<>();

    private File currentPath;

    @Override
    protected void viewReady(URL url, ResourceBundle resourceBundle) {

        GridView<File> gridView = getView().getFileGridView();
        gridView.setOnMouseClicked(this::gridFileClicked);
        setRecordRootFolder(new File(configuration.getVideoFolder()));

    }

    @FXML
    public void folderBack() {

        if (previousPath.isEmpty()) {
            return;
        }

        File prev = previousPath.removeLast();
        nextPath.addLast(currentPath);
        currentPath = prev;
        refreshFolder();
    }

    @FXML
    public void folderForward() {

        if (nextPath.isEmpty()) {
            return;
        }

        File next = nextPath.removeLast();
        previousPath.addLast(currentPath);
        currentPath = next;
        refreshFolder();

    }

    @FXML
    public void addNewFolder() {
        getView().getView(EditFolderView.class)
                .show(currentPath);
    }

    @FXML
    public void doRefreshFolder() {
        refreshFolder();
    }

    @FXML
    public void onTrashFiles() {

        RecordsView view = getView();
        ObservableList<File> selected = view.getFileSelected();
        if (selected.isEmpty()) {
            return;
        }

        StringBuilder text = new StringBuilder("你的确要删除以下文件吗：\n").append(
                "-----------------------------\n"
        );

        for (File file : selected) {
            text.append(file.getName()).append("\n");
        }

        Alert alert = view.alert("删除", text.toString(), Alert.AlertType.CONFIRMATION);
        alert.showAndWait().ifPresent(t -> {
            if (t.getButtonData() == ButtonBar.ButtonData.OK_DONE) {

                List<File> deleted = new ArrayList<>();
                for (File file: selected) {
                    if(FileUtils.deleteAnyFile(file)) {
                        deleted.add(file);
                    }
                }

                nextPath.clear();

                btnBack.setDisable(previousPath.isEmpty());
                btnForward.setDisable(nextPath.isEmpty());
                btnTrash.setDisable(true);

                selected.removeAll(deleted);
                refreshFolder();
            }
        });
    }

    private void refreshFolder() {

        RecordsView view = getView();
        GridView<File> gridView = view.getFileGridView();

        File[] files = currentPath.listFiles();
        ObservableList<File> items = gridView.getItems();

        btnBack.setDisable(previousPath.isEmpty());
        btnForward.setDisable(nextPath.isEmpty());
        btnTrash.setDisable(true);

        items.clear();
        items.addAll(files);

        view.getFileSelected().clear();

    }

    public void setRecordRootFolder(File recordFolder) {

        currentPath = new File(recordFolder.getAbsolutePath());
        btnForward.setDisable(true);
        btnBack.setDisable(true);
        btnTrash.setDisable(true);
        nextPath.clear();
        previousPath.clear();

        refreshFolder();
    }

    private void gridFileClicked(MouseEvent event) {

        ObservableList<File> selected = getView().getFileSelected();

        if (event.getClickCount() > 1) {
            if (selected.size() == 1) {
                File selectedFile = selected.get(0);
                if (selectedFile.isDirectory()) {
                    nextPath.clear();
                    previousPath.addLast(currentPath);
                    currentPath = selectedFile;
                    refreshFolder();
                    return;
                }
                try {
                    Desktop.getDesktop()
                            .open(selectedFile);
                } catch (Exception e) {
                    logger.error("Failed to open file: " + selectedFile.getAbsolutePath(), e);
                    Alert alert = getView().alert("失败", "无法打开指定的文件 - " + e.getClass().getSimpleName(), Alert.AlertType.ERROR);
                    alert.showAndWait();
                }
            }
        } else {
            btnTrash.setDisable(selected.isEmpty());
        }

    }


}
