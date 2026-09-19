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

    /**
     * 开关按钮：开=实心主色（醒目），关=描边幽灵态（透明底 + outline 描边 + 弱化文字），
     * 状态全靠颜色差异表达，按钮文案固定不变。
     */
    public static void toggle(MFXButton button, Theme theme, boolean on) {
        button.setMaxWidth(Double.MAX_VALUE);
        String base = "-fx-background-radius: 16; -fx-border-radius: 16;"
                + " -fx-padding: 9 12 9 12; -fx-font-size: 13px; -fx-cursor: hand;";
        if (on) {
            button.setStyle("-fx-background-color: " + Theme.hex(theme.primary()) + ";"
                    + "-fx-text-fill: " + Theme.hex(theme.onPrimary()) + ";"
                    + "-fx-border-width: 0;" + base);
        } else {
            button.setStyle("-fx-background-color: transparent;"
                    + "-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant()) + ";"
                    + "-fx-border-color: " + Theme.hex(theme.outline()) + ";"
                    + "-fx-border-width: 1.5;" + base);
        }
    }

    private static void fill(MFXButton button, Color background, Color text) {
        // maxWidth=MAX 配合菜单的 setFillWidth(true)，让所有按钮等宽铺满菜单
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: " + Theme.hex(background) + ";"
                + "-fx-text-fill: " + Theme.hex(text) + ";"
                + "-fx-background-radius: 16;"
                + "-fx-padding: 9 12 9 12;"
                + "-fx-font-size: 13px;"
                + "-fx-cursor: hand;");
    }

    /** 主题变了重刷按钮颜色。 */
    public static void refresh(Iterable<MFXButton> buttons, Theme theme) {
        for (MFXButton button : buttons) fill(button, theme.primaryContainer(), theme.onPrimaryContainer());
    }

    /** 半透明 scrim 底卡：盖在壁纸上，保住页面内容的可读性。 */
    public static String scrimCard(Theme theme) {
        return "-fx-background-color: " + Theme.rgba(theme.scrim()) + ";"
                + "-fx-background-radius: 24;"
                + "-fx-padding: 36 48;";
    }

    private Styles() {
    }
}
