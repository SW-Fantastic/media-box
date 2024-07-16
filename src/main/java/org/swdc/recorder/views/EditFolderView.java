package org.swdc.recorder.views;


import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;
import org.swdc.recorder.views.controllers.RecordsController;

import java.io.File;

@View(
        viewLocation = "views/main/EditFolderView.fxml",
        title = "创建文件夹",
        dialog = true,
        resizeable = false,
        multiple = true
)
public class EditFolderView extends AbstractView {

    private File parentFolder;

    @Inject
    private RecordsView recordsView;

    @PostConstruct
    public void initView() {

        Button btnOk = findById("btnOk");
        Button btnCancel = findById("btnCancel");

        TextField field = findById("txtName");

        btnOk.setOnAction(e -> {

            if (parentFolder == null || field.getText().isBlank()) {
                return;
            }

            File targetFolder = parentFolder.toPath()
                    .resolve(field.getText())
                    .toFile();

            if (targetFolder.exists()) {
                return;
            }

            targetFolder.mkdirs();
            hide();

            RecordsController controller = recordsView.getController();
            controller.doRefreshFolder();
        });

        btnCancel.setOnAction(e -> {

            field.setText("");
            parentFolder = null;
            hide();

        });

    }

    public void show(File parent) {
        this.parentFolder = parent;
        this.show();
    }


}
