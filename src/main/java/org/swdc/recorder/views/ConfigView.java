package org.swdc.recorder.views;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.PropertySheet;
import org.swdc.fx.FXResources;
import org.swdc.fx.config.ConfigViews;
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

    @PostConstruct
    public void initView() {

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
