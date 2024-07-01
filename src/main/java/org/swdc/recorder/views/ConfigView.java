package org.swdc.recorder.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PropertySheet;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;
import org.swdc.fx.font.FontSize;
import org.swdc.fx.font.Fontawsome5Service;
import org.swdc.fx.view.AbstractView;
import org.swdc.fx.view.View;
import org.swdc.recorder.RecorderConfiguration;

@View(
        viewLocation = "views/main/ConfigView.fxml",
        title = "设置",
        resizeable = false
)
public class ConfigView extends AbstractView {


    @Inject
    private FXResources resources;

    @Inject
    private RecorderConfiguration config;

    @Inject
    private Fontawsome5Service fontawsome5Service;

    @PostConstruct
    public void initView() {

        Button dshowConfig = findById("windowsRec");
        dshowConfig.setPadding(new Insets(4));
        dshowConfig.setFont(fontawsome5Service.getBrandFont(FontSize.MIDDLE_SMALL));
        dshowConfig.setText(fontawsome5Service.getFontIcon("windows"));

        ObservableList confGenerals = ConfigViews.parseConfigs(resources,config);
        PropertySheet generalConfSheet = new PropertySheet(confGenerals);
        generalConfSheet.setPropertyEditorFactory(ConfigViews.factory(resources));

        generalConfSheet.setModeSwitcherVisible(false);
        generalConfSheet.setSearchBoxVisible(false);
        generalConfSheet.getStyleClass().add("prop-sheets");

        BorderPane pane = (BorderPane) getView();
        pane.setCenter(generalConfSheet);

    }


}
