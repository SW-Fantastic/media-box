package org.swdc.recorder;

import org.swdc.config.annotations.ConfigureSource;
import org.swdc.config.configs.JsonConfigHandler;
import org.swdc.fx.config.ApplicationConfig;

/**
 * 这是应用程序的配置类。
 * <br><br>
 * 应用程序的主配置类需要继承自ApplicationConfig，
 * 其他配置需要继承org.swdc.config.AbstractConfig，
 * 并且标注注解ConfigureSource。
 * <br><br>
 * value需要指定相对于项目根路径的配置文件，handle用于指定
 * 处理此类型文件的Handle，目前我们支持这些格式：
 * <br><br>
 * json ：org.swdc.config.configs.JsonConfigHandler<br>
 * ini  ：org.swdc.config.configs.InitializeConfigHandler<br>
 * properties ： org.swdc.config.configs.PropertiesHandler<br>
 * yaml ： org.swdc.config.configs.YamlConfigHandler<br>
 * typesafe hocon ：org.swdc.config.configs.HOCONConfigHandler<br>
 * <br><br>
 * 每一项需要存储的配置字段，请为它标注此注解，注解的内容为存储时使用的key。<br>
 * org.swdc.config.annotations.Property
 * <br><br>
 * 支持的字段的类型多样，支持map，list，String等。
 * <br><br>
 * ApplicationConfig是一个配置类中的特例，本类提供额外的两个属性，它们是必须的
 * 一个是language，用于国际化，如果不需要，请设置为“unavailable”，另一个是theme，
 * 这关系到应用程序的主题样式，本框架通过此项查找和提供窗口的主题。
 * <br><br>
 * 主题存放的位置是SWFXApplication注解中，asset目录下的skin文件夹中，
 * 你需要为每一个主题创建一个子目录，其中必须含有一个stage.less，框架会自动编译
 * 它为css并且加载。
 *
 */
@ConfigureSource(value = "assets/config.json", handler = JsonConfigHandler.class)
public class RecorderConfiguration extends ApplicationConfig {
}
