package org.swdc.recorder.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.GridView;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;
import org.swdc.recorder.views.cells.FileGridCell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@View(
        viewLocation = "views/main/RecordsView.fxml",
        title = "已录制的视频"
)
public class RecordsView extends AbstractView {

    private GridView<File> fileGridView = new GridView<>();

    @Inject
    private Fontawsome5Service fontawsome5Service;

    private ObservableList<File> fileSelected = FXCollections.observableArrayList();


    @PostConstruct
    public void initView() {

        fileGridView.setCellHeight(82);
        fileGridView.setCellWidth(82);
        fileGridView.setPadding(new Insets(12));
        fileGridView.setHorizontalCellSpacing(4);
        fileGridView.setVerticalCellSpacing(4);
        fileGridView.getStyleClass().add("gridView");
        fileGridView.setCellFactory(gv -> new FileGridCell(fontawsome5Service, fileSelected));


        BorderPane pane = (BorderPane) getView();
        pane.setCenter(fileGridView);

        setupButtonIcon(findById("back"),"chevron-left");
        setupButtonIcon(findById("forward"),"chevron-right");
        setupButtonIcon(findById("refresh"), "redo-alt");
        setupButtonIcon(findById("addFolder"), "folder-plus");
        setupButtonIcon(findById("trashFile"), "trash-alt");

    }

    private void setupButtonIcon(ButtonBase button, String iconName) {

        button.setPadding(new Insets(4));
        button.setFont(fontawsome5Service.getSolidFont(FontSize.MIDDLE_SMALL));
        button.setText(fontawsome5Service.getFontIcon(iconName));

    }

    public GridView<File> getFileGridView() {
        return fileGridView;
    }

    public ObservableList<File> getFileSelected() {
        return fileSelected;
    }
}
