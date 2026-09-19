package ui;

import config.PlayMode;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

/** 主菜单。 */
public class MenuView extends VBox {

    public MenuView(Theme theme, Match3App app) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setFillWidth(false);
        // StackPane 会把子节点拉满整页；限成首选尺寸，scrim 卡才贴着内容而不是铺满窗口
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setStyle(Styles.scrimCard(theme));

        // 标题本身就是字标，不再配图标
        Label title = new Label(I18n.tr("menu.title"));
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 46px; -fx-font-weight: bold;");

        var play = new MFXButton(I18n.tr("menu.play"), Icons.of(Material2MZ.PLAY_ARROW, theme.onPrimary()));
        Styles.primary(play, theme);
        play.setMinWidth(220);
        play.setOnAction(e -> app.startSetup(PlayMode.NEW_LOCAL));

        var online = new MFXButton(I18n.tr("menu.online"), Icons.of(Material2MZ.WIFI, theme.onPrimaryContainer()));
        Styles.button(online, theme);
        online.setMinWidth(220);
        online.setOnAction(e -> app.startSetup(PlayMode.JOIN));

        var settings = new MFXButton(I18n.tr("menu.settings"), Icons.of(Material2MZ.SETTINGS, theme.onPrimaryContainer()));
        Styles.button(settings, theme);
        settings.setMinWidth(220);
        settings.setOnAction(e -> app.showSettings(null));

        var exit = new MFXButton(I18n.tr("menu.exit"), Icons.of(Material2AL.EXIT_TO_APP, theme.onPrimaryContainer()));
        Styles.button(exit, theme);
        exit.setMinWidth(220);
        exit.setOnAction(e -> app.exit());

        getChildren().addAll(title, play, online, settings, exit);
    }
}
