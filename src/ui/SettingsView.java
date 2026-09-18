package ui;

import config.GameSettings;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXSlider;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** 明暗、壁纸取色、音量与若干开关。改完主题会当场重新取色。 */
public class SettingsView extends VBox {

    public SettingsView(GameSettings settings, Theme theme, Match3App app) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(32));
        setFillWidth(false);

        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 26px; -fx-font-weight: bold;");

        ToggleGroup brightness = new ToggleGroup();
        MFXRadioButton dark = new MFXRadioButton("Dark");
        dark.setToggleGroup(brightness);
        dark.setSelected(theme.isDark());
        dark.setOnAction(e -> {
            theme.setDark(true);
            app.applyTheme();
        });
        MFXRadioButton light = new MFXRadioButton("Light");
        light.setToggleGroup(brightness);
        light.setSelected(!theme.isDark());
        light.setOnAction(e -> {
            theme.setDark(false);
            app.applyTheme();
        });

        MFXCheckbox wallpaper = new MFXCheckbox("Colour from wallpaper (Monet)");
        wallpaper.setSelected(theme.isWallpaper());
        wallpaper.setOnAction(e -> {
            theme.setWallpaper(wallpaper.isSelected());
            app.applyTheme();
        });

        var shuffleWallpaper = new MFXButton("Next wallpaper");
        Styles.button(shuffleWallpaper, theme);
        shuffleWallpaper.setOnAction(e -> {
            theme.nextWallpaper();
            app.applyTheme();
        });

        MFXSlider volume = new MFXSlider(0, 100, app.musicLibrary().volume());
        volume.setOnMouseReleased(e -> app.musicLibrary().setVolume((int) volume.getValue()));

        MFXCheckbox verbose = new MFXCheckbox("Show a dialog on every step");
        verbose.setSelected(settings.verboseDialogs());
        verbose.setOnAction(e -> settings.setVerboseDialogs(verbose.isSelected()));

        MFXCheckbox autoRestart = new MFXCheckbox("Back to setup when a game ends");
        autoRestart.setSelected(settings.autoRestart());
        autoRestart.setOnAction(e -> settings.setAutoRestart(autoRestart.isSelected()));

        var back = new MFXButton("Back");
        Styles.primary(back, theme);
        back.setMinWidth(200);
        back.setOnAction(e -> app.showMenu());

        getChildren().addAll(title,
                new HBox(16, dark, light),
                wallpaper,
                shuffleWallpaper,
                new Label("Music volume"), volume,
                verbose,
                autoRestart,
                back);
    }
}
