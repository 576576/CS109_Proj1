package spike;

import javafx.application.Application;

/**
 * 单独的启动类：main 不放在 Application 子类里，否则 JavaFX 在非模块化 classpath 上
 * 会报 "JavaFX runtime components are missing"。
 */
public class FxSpike {
    public static void main(String[] args) {
        Application.launch(FxSpikeApp.class, args);
    }
}
