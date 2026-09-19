package ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/** Material 图标：字体随 ikonli 的 jar 走，颜色一律取当前 Monet 色板。 */
public final class Icons {

    private static final int DEFAULT_SIZE = 18;
    private static final int GAP = 8;

    private Icons() {
    }

    public static FontIcon of(Ikon ikon, Color color) {
        return of(ikon, color, DEFAULT_SIZE);
    }

    public static FontIcon of(Ikon ikon, Color color, int size) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(size);
        icon.setIconColor(color);
        return icon;
    }

    /** checkbox / radio 这类控件本身放不了图标，就在左边并排一个。 */
    public static Node beside(Ikon ikon, Color color, Node node) {
        return beside(ikon, color, node, DEFAULT_SIZE);
    }

    public static Node beside(Ikon ikon, Color color, Node node, int size) {
        HBox row = new HBox(GAP, of(ikon, color, size), node);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
