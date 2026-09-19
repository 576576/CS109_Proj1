package ui;

import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import util.Log;

/**
 * 统一的对话框出口：一张跟着壁纸取色的 Material 卡片，配 Material 图标和主色按钮。
 * 之前用的是 JavaFX 自带的 Alert，是系统样式，跟取色界面完全不搭。
 * 调用方可能在后台线程上，所以一律先切回 JavaFX 线程。
 */
public final class Dialogs {

    private enum Kind {
        INFO("dlg.title.info", Material2AL.INFO),
        WARN("dlg.title.warn", Material2MZ.WARNING),
        ERROR("dlg.title.error", Material2AL.ERROR);

        private final String titleKey;
        private final Ikon ikon;

        Kind(String titleKey, Ikon ikon) {
            this.titleKey = titleKey;
            this.ikon = ikon;
        }
    }

    private Dialogs() {
    }

    public static void info(String message) {
        show(Kind.INFO, message, true);
    }

    public static void warn(String message) {
        show(Kind.WARN, message, true);
    }

    public static void error(String message) {
        show(Kind.ERROR, message, true);
    }

    /** 不阻塞的通知：建房等待对手时用它，调用方还得接着往下走。 */
    public static void notifyInfo(String message) {
        show(Kind.INFO, message, false);
    }

    private static void show(Kind kind, String message, boolean wait) {
        if (Platform.isFxApplicationThread()) present(kind, message, wait);
        else Platform.runLater(() -> present(kind, message, wait));
    }

    private static void present(Kind kind, String message, boolean wait) {
        Match3App app = Match3App.get();
        if (app == null || app.theme() == null) {
            Log.warn("Dialog dropped before the window is up: " + message);
            return;
        }
        Theme theme = app.theme();

        Label title = new Label(I18n.tr(kind.titleKey));
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + ";"
                + " -fx-font-size: 21px; -fx-font-weight: bold;");

        Label body = new Label(message);
        body.setWrapText(true);
        body.setPrefWidth(340);
        body.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 14px;");

        HBox header = new HBox(10, Icons.of(kind.ikon, theme.primary(), 26), title);
        header.setAlignment(Pos.CENTER_LEFT);

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        MFXButton ok = new MFXButton(I18n.tr("dlg.ok"));
        Styles.primary(ok, theme);
        ok.setMinWidth(120);
        ok.setOnAction(e -> dialog.close());

        VBox card = new VBox(18, header, body, ok);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(420);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: " + Theme.hex(theme.surface()) + ";"
                + " -fx-background-radius: 28;"
                + " -fx-border-color: " + Theme.hex(theme.outline()) + ";"
                + " -fx-border-radius: 28;"
                + " -fx-padding: 28 32 28 32;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 28, 0.1, 0, 8);");

        StackPane pane = new StackPane(card);
        pane.setStyle("-fx-padding: 20;");
        Scene scene = new Scene(pane, Color.TRANSPARENT);
        String css = theme.stylesheet();
        if (css != null) scene.getStylesheets().add(css);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) dialog.close();
        });

        dialog.setScene(scene);
        dialog.setResizable(false);
        if (app.stage() != null) {
            dialog.initOwner(app.stage());
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.sizeToScene();
        dialog.centerOnScreen();
        if (wait) dialog.showAndWait();
        else dialog.show();
    }
}
