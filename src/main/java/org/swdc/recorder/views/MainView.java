package org.swdc.recorder.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;
import org.swdc.recorder.views.controllers.MainController;


@View(
        viewLocation = "views/main/MainView.fxml",
        title = "映画",
        resizeable = false
)
public class MainView extends AbstractView {

    @Inject
    private Logger logger;

    @Inject
    private Fontawsome5Service fontawsome5Service;

    @PostConstruct
    public void onViewInit() {

        Stage stage = getStage();
        stage.setMinHeight(200);
        stage.setMinWidth(580);

        setButtonIcon(findById("btnSetting"), "cog");
        setButtonIcon(findById("btnList"), "video");

    }

    private void setButtonIcon(ButtonBase buttonBase, String icon) {
        buttonBase.setFont(fontawsome5Service.getSolidFont(FontSize.MIDDLE_SMALL));
        buttonBase.setPadding(new Insets(4));
        buttonBase.setText(fontawsome5Service.getFontIcon(icon));
    }


    public void refreshDevices() {

        MainController controller = getController();
        controller.refreshDevices();

    }

}
