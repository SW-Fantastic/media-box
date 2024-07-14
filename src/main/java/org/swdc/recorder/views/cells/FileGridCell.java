package org.swdc.recorder.views.cells;

import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.controlsfx.control.GridCell;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileGridCell extends GridCell<File> {

    private Label icon;

    private Label text;

    private VBox root;

    private Fontawsome5Service fontawsome5Service;

    private ObservableList<File> selectContainer;

    private static List<String> audioExts = Arrays.asList("mp3","ogg","wav","aac");

    private static List<String> videoExts = Arrays.asList("mp4","mkv","flv","wmv");

    public FileGridCell(Fontawsome5Service fontawsome5Service, ObservableList<File> selectContainer) {
        this.fontawsome5Service = fontawsome5Service;
        this.selectContainer = selectContainer;
        this.selectContainer.addListener((ListChangeListener<? super File>)  c -> {
            if (root == null) {
                return;
            }
            while (c.next()) {
                if(c.getRemoved().contains(getItem())) {
                    root.getStyleClass().remove("selected");
                }
                if (c.getAddedSubList().contains(getItem())) {
                    root.getStyleClass().add("selected");
                }
            }
        });
    }

    @Override
    protected void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setGraphic(null);
            return;
        }
        if (root == null) {
            root = new VBox();
            text = new Label();
            icon = new Label();

            root.getChildren().addAll(
                    icon,text
            );
            root.setAlignment(Pos.CENTER);
            root.setSpacing(6);
            root.setPadding(new Insets(4));
            root.setOnMouseClicked(e -> {
                if (e.getClickCount() > 1) {
                    if (!selectContainer.contains(item)) {
                        if (!e.isControlDown()) {
                            selectContainer.clear();
                        }
                        selectContainer.add(item);
                    }
                } else {
                    if (!selectContainer.contains(item)) {
                        if (!e.isControlDown()) {
                            selectContainer.clear();
                        }
                        selectContainer.add(item);
                    } else {
                        selectContainer.remove(item);
                    }
                }

            });
        }


        String ext = "";
        if (item.getName().contains(".")) {
            ext = item.getName().substring(
                    item.getName().lastIndexOf(".") + 1
            );
        }

        icon.setFont(fontawsome5Service.getSolidFont(FontSize.MIDDLE_LARGE));
        icon.getStyleClass().add("icon");
        if (ext.isBlank()) {
            icon.setText(fontawsome5Service.getFontIcon("folder"));
        } else {
            ext = ext.toLowerCase();
            if (audioExts.contains(ext)) {
                icon.setText(fontawsome5Service.getFontIcon("volume-up"));
            } else if (videoExts.contains(ext)) {
                icon.setText(fontawsome5Service.getFontIcon("video"));
            } else {
                icon.setText(fontawsome5Service.getFontIcon("file"));
            }
        }
        text.setText(item.getName());
        setGraphic(root);
    }
}
