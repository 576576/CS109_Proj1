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
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import java.util.Locale;
import java.util.Objects;

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

        var shuffleWallpaper = new MFXButton(I18n.tr("settings.nextWallpaper"),
                Icons.of(Material2MZ.SHUFFLE, theme.onPrimaryContainer()));
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

        var back = new MFXButton(I18n.tr("settings.back"), Icons.of(Material2AL.ARROW_BACK, theme.onPrimary()));
        Styles.primary(back, theme);
        back.setMinWidth(200);
        back.setOnAction(e -> app.showMenu());

        getChildren().addAll(
                Icons.beside(Material2MZ.TUNE, theme.primary(), title, 26),
                new HBox(16,
                        Icons.beside(Material2MZ.NIGHTS_STAY, theme.onSurface(), dark),
                        Icons.beside(Material2MZ.WB_SUNNY, theme.onSurface(), light)),
                Icons.beside(Material2MZ.WALLPAPER, theme.onSurface(), wallpaper),
                shuffleWallpaper,
                Icons.beside(Material2MZ.VOLUME_UP, theme.onSurface(), volumeLabel),
                volume,
                Icons.beside(Material2MZ.TRANSLATE, theme.onSurface(), languageLabel),
                languageBox(app),
                Icons.beside(Material2AL.CHAT, theme.onSurface(), verbose),
                Icons.beside(Material2AL.AUTORENEW, theme.onSurface(), autoRestart),
                back);
    }

    /** 下拉选语言，选中即切、整屏重建。第一项「自动」的 locale 是 null。 */
    private MFXComboBox<I18n.Language> languageBox(Match3App app) {
        MFXComboBox<I18n.Language> box = new MFXComboBox<>(FXCollections.observableArrayList(I18n.languages()));
        box.setPrefWidth(240);
        // 文字画在内层 BoundTextField 上，它的对齐被皮肤写死，只能用 API 改
        box.setAlignment(Pos.CENTER);
        I18n.Language current = I18n.languages().stream()
                .filter(language -> I18n.isAuto()
                        ? language.locale() == null
                        : Objects.equals(language.locale(), I18n.locale()))
                .findFirst()
                .orElse(null);
        if (current != null) box.selectItem(current);
        box.setOnAction(e -> {
            I18n.Language selected = box.getSelectedItem();
            if (selected == null) return;
            Locale wanted = selected.locale();
            if (I18n.isAuto() ? wanted != null : !Objects.equals(wanted, I18n.locale())) app.setLocale(wanted);
        });
        return box;
    }
}
