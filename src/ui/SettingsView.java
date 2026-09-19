package ui;

import config.GameSettings;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXSlider;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** 明暗、壁纸取色、语言、音量与若干开关。改完主题会当场重新取色。 */
public class SettingsView extends VBox {

    public SettingsView(GameSettings settings, Theme theme, Match3App app) {
        setAlignment(Pos.CENTER);
        setSpacing(16);
        setFillWidth(false);
        // StackPane 会把子节点拉满整页；限成首选尺寸，scrim 卡才贴着内容而不是铺满窗口
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setStyle(Styles.scrimCard(theme));

        Label title = new Label(I18n.tr("settings.title"));
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 26px; -fx-font-weight: bold;");

        ToggleGroup brightness = new ToggleGroup();
        MFXRadioButton dark = new MFXRadioButton(I18n.tr("settings.dark"));
        dark.setToggleGroup(brightness);
        dark.setSelected(theme.isDark());
        dark.setOnAction(e -> {
            theme.setDark(true);
            app.applyTheme();
        });
        MFXRadioButton light = new MFXRadioButton(I18n.tr("settings.light"));
        light.setToggleGroup(brightness);
        light.setSelected(!theme.isDark());
        light.setOnAction(e -> {
            theme.setDark(false);
            app.applyTheme();
        });

        MFXCheckbox wallpaper = new MFXCheckbox(I18n.tr("settings.wallpaper"));
        wallpaper.setSelected(theme.isWallpaper());
        wallpaper.setOnAction(e -> {
            theme.setWallpaper(wallpaper.isSelected());
            app.applyTheme();
        });

        var shuffleWallpaper = new MFXButton(I18n.tr("settings.nextWallpaper"));
        Styles.button(shuffleWallpaper, theme);
        shuffleWallpaper.setOnAction(e -> {
            theme.nextWallpaper();
            app.applyTheme();
        });

        MFXSlider volume = new MFXSlider(0, 100, app.musicLibrary().volume());
        volume.setOnMouseReleased(e -> app.musicLibrary().setVolume((int) volume.getValue()));

        Label volumeLabel = new Label(I18n.tr("settings.volume"));
        volumeLabel.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");

        Label languageLabel = new Label(I18n.tr("settings.language"));
        languageLabel.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");

        MFXCheckbox verbose = new MFXCheckbox(I18n.tr("settings.verbose"));
        verbose.setSelected(settings.verboseDialogs());
        verbose.setOnAction(e -> settings.setVerboseDialogs(verbose.isSelected()));

        MFXCheckbox autoRestart = new MFXCheckbox(I18n.tr("settings.autoRestart"));
        autoRestart.setSelected(settings.autoRestart());
        autoRestart.setOnAction(e -> settings.setAutoRestart(autoRestart.isSelected()));

        var back = new MFXButton(I18n.tr("settings.back"));
        Styles.primary(back, theme);
        back.setMinWidth(200);
        back.setOnAction(e -> app.showMenu());

        getChildren().addAll(title,
                new HBox(16, dark, light),
                wallpaper,
                shuffleWallpaper,
                volumeLabel, volume,
                languageLabel,
                languageBox(app),
                verbose,
                autoRestart,
                back);
    }

    /** 下拉选语言，选中即切、整屏重建。 */
    private MFXComboBox<I18n.Language> languageBox(Match3App app) {
        MFXComboBox<I18n.Language> box = new MFXComboBox<>(FXCollections.observableArrayList(I18n.languages()));
        I18n.languages().stream()
                .filter(language -> language.locale().equals(I18n.locale()))
                .findFirst()
                .ifPresent(box::selectItem);
        box.setOnAction(e -> {
            I18n.Language selected = box.getSelectedItem();
            if (selected != null && !selected.locale().equals(I18n.locale())) app.setLocale(selected.locale());
        });
        return box;
    }
}
