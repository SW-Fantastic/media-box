package org.swdc.recorder;

import org.swdc.dependency.DependencyContext;
import org.swdc.dependency.EnvironmentLoader;
import org.swdc.fx.FXApplication;
import org.swdc.fx.FXResources;
import org.swdc.fx.SWFXApplication;
import org.swdc.platforms.NativePlatform;
import org.swdc.recorder.core.DesktopRecorder;
import org.swdc.recorder.views.MainView;

/**
 * 应用启动和依赖控制的类
 * <br><br>
 * 应用程序类需要使用SWFXApplication类，
 * assetFolder用于指定一个资源目录，它需要一个相对路径，相对于应用根目录。
 * splash是闪屏界面，你需要自行实现一个，这里有一个简单的闪屏界面，仅仅用于展示一个图片
 * configs指的是配置类，这里目前就一个主配置，写在这里的配置会被预加载。
 * icons指的是应用程序的图标，通常会覆盖整个应用程序的所有窗口。
 * <br><br>
 * 应用程序的图标存放位置是resource目录的icons，这里需要填写icons的文件名称，建议使用png格式。
 * 此外你还需要准备一个ico图标用于Windows的打包或者icns图标用于macos的打包。
 * <br><br>
 * 本类不接受依赖注入，请使用提供的依赖环境直接获取对象。
 * @author SW-Fantastic
 */
@SWFXApplication(assetsFolder = "./assets",
        splash = SplashScene.class,
        configs = { RecorderConfiguration.class },
        icons = { "rec16.png","rec24.png","rec32.png","rec64.png","rec128.png","rec256.png","rec512.png" })
public class RecorderApplication extends FXApplication {


    /**
     * 系统正在退出的时候会调用的方法。
     * @param context 依赖环境，如果你想要做一些资源释放，可以在这里进行。
     *                但是，你必须通过Platform的exit方法退出，此方法才会触发。
     */
    @Override
    public void onShutdown(DependencyContext context) {
        context.getByClass(DesktopRecorder.class).dispose();
    }

    /**
     * 应用程序的预加载方法，如果你需要预先注入一些对象或者注册来自其他
     * 类库的东西，那么你可以在这里进行。
     * @param loader 依赖环境加载器
     */
    @Override
    public void onConfig(EnvironmentLoader loader) {

    }

    /**
     * 应用程序已经准备就绪，可以开始处理应用程序的逻辑了。
     * @param dependencyContext 依赖环境，Application类不进行注入，
     *                          需要啥直接从这里获取。
     */
    @Override
    public void onStarted(DependencyContext dependencyContext) {
        FXResources resources = dependencyContext.getByClass(FXResources.class);
        NativePlatform.initializePlatform(resources.getAssetsFolder());
        dependencyContext.getByClass(MainView.class).show();
    }

}
