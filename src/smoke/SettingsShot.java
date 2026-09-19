package smoke;

import config.GameSettings;
import ui.Match3App;

/** 临时探针：截设置页，验证 scrim 底卡与文字颜色。 */
public class SettingsShot {

    public static void main(String[] args) throws Exception {
        FxHarness.boot();
        Thread.sleep(1500);
        FxHarness.onFx(() -> Match3App.get().showSettings(new GameSettings()));
        Thread.sleep(1000);
        FxHarness.onFx(() -> UiShot.snapshot(Match3App.get().scene(), "build/shot-settings.png"));
        Thread.sleep(500);
        FxHarness.shutdown();
        System.exit(0);
    }
}
