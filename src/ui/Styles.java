package ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.scene.paint.Color;

/** 控件外观统一走 Monet 色板：MaterialFX 负责交互手感，颜色和圆角在这里定。 */
public final class Styles {

    /** 普通操作按钮：填充次要容器色。 */
    public static void button(MFXButton button, Theme theme) {
        fill(button, theme.primaryContainer(), theme.onPrimaryContainer());
    }

    /** 主行动按钮：填充主色。 */
    public static void primary(MFXButton button, Theme theme) {
        fill(button, theme.primary(), theme.onPrimary());
    }

    private static void fill(MFXButton button, Color background, Color text) {
        button.setStyle("-fx-background-color: " + Theme.hex(background) + ";"
                + "-fx-text-fill: " + Theme.hex(text) + ";"
                + "-fx-background-radius: 16;"
                + "-fx-padding: 10 20 10 20;"
                + "-fx-font-size: 14px;"
                + "-fx-cursor: hand;");
    }

    /** 主题变了重刷按钮颜色。 */
    public static void refresh(Iterable<MFXButton> buttons, Theme theme) {
        for (MFXButton button : buttons) fill(button, theme.primaryContainer(), theme.onPrimaryContainer());
    }

    private Styles() {
    }
}
