// 这里是模块声明，没见过的请自行了解JPMS相关知识。
module recorder {

    // framework 模块，这三个全部都要
    requires swdc.application.fx;
    requires swdc.application.dependency;
    requires swdc.application.configs;
    requires swdc.platform;

    // javaFX模块，按需要引入
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.controlsfx.controls;

    // Swing和Injection
    requires java.desktop;
    requires jakarta.annotation;
    requires jakarta.inject;

    requires org.bytedeco.ffmpeg;

    // Logger模块，这里使用SLF4J + Logback
    requires org.slf4j;

    // 开放package访问权限给必要的模块。
    // 如果你看到类似于cannot access class xxx (in module xxx) because module xxx does not export
    // 那么请使用opens语句开放访问权限。
    opens org.swdc.recorder to
            swdc.application.dependency,
            swdc.application.fx,
            swdc.application.configs,
            org.controlsfx.controls,
            javafx.graphics,
            javafx.controls;

    opens org.swdc.recorder.core to
            swdc.application.fx,
            swdc.application.dependency;

    opens org.swdc.recorder.views to
            swdc.application.dependency,
            swdc.application.fx,
            swdc.application.configs,
            javafx.graphics,
            javafx.controls;


    opens org.swdc.recorder.views.controllers to
            swdc.application.dependency,
            swdc.application.fx,
            javafx.fxml,
            javafx.controls;

    // 特别说明，这里不能直接使用views这个包名，因为我的框架占用了它。
    opens views.main;
    // 这里面放图标。
    opens icons;

}