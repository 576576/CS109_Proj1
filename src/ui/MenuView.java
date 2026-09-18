package ui;

import config.PlayMode;
import config.GameSettings;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** 主菜单。 */
public class MenuView extends VBox {

    public MenuView(Theme theme, Match3App app) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(40));
        setFillWidth(false);

        Label title = new Label("MATCH-3");
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 46px; -fx-font-weight: bold;");

        Label subtitle = new Label("CS109 · JavaFX · Material You");
        subtitle.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant()) + "; -fx-font-size: 14px;");

        var play = new MFXButton("Play");
        Styles.primary(play, theme);
        play.setMinWidth(220);
        play.setOnAction(e -> app.startSetup(PlayMode.NEW_LOCAL));

        var online = new MFXButton("Online Play");
        Styles.button(online, theme);
        online.setMinWidth(220);
        online.setOnAction(e -> app.startSetup(PlayMode.JOIN));

        var settings = new MFXButton("Settings");
        Styles.button(settings, theme);
        settings.setMinWidth(220);
        settings.setOnAction(e -> app.showSettings(null));

        var exit = new MFXButton("Exit");
        Styles.button(exit, theme);
        exit.setMinWidth(220);
        exit.setOnAction(e -> app.exit());

        getChildren().addAll(title, subtitle, play, online, settings, exit);
    }
}
