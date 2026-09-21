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

import java.util.concurrent.CountDownLatch;

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

    /** 阴影完整淡出需要的余量：blur 28 + 偏移 10 = 38，留 44 不被 scene 边界切出硬边。 */
    private static final int SHADOW_MARGIN = 44;

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

    /**
     * 阻塞式等待弹窗：在 JavaFX 线程上用 showAndWait 呈现（模态，挡住主界面），
     * 同时把 blockingTask 丢到另一条后台线程跑；task 结束（或用户点取消）后弹窗自动关闭，本方法才返回。
     * 用于建房后等待对手连入——accept() 阻塞在 task 里，连上了就关弹窗。
     * 必须在非 JavaFX 线程上调用，否则会死锁。
     */
    public static void waitWhile(String message, Runnable blockingTask, Runnable onCancel) {
        Match3App app = Match3App.get();
        if (app == null || app.theme() == null || Platform.isFxApplicationThread()) {
            // 界面没起来、或在 FX 线程上，都不适合弹阻塞窗：直接跑 task，避免卡死
            Log.warn("waitWhile skipped dialog; running task inline");
            blockingTask.run();
            return;
        }

        CountDownLatch closed = new CountDownLatch(1);
        Stage[] holder = new Stage[1];
        Platform.runLater(() -> {
            Theme theme = app.theme();
            Stage dialog = buildDialog(app, theme, Kind.INFO, message, false);
            holder[0] = dialog;

            MFXButton cancel = new MFXButton(I18n.tr("dlg.cancel"));
            Styles.primary(cancel, theme);
            cancel.setMinWidth(120);
            // 取消只关 socket；accept() 抛出后由 task 的 finally 负责关弹窗，不要在这里 close，
            // 否则连上成功的那次 close() 也会走到取消分支，把刚建好的连接掐掉
            cancel.setOnAction(e -> { if (onCancel != null) onCancel.run(); });
            replaceButtons(dialog, cancel);

            dialog.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE && onCancel != null) onCancel.run();
            });

            // 后台跑阻塞任务，结束后关弹窗；showAndWait 期间 FX 事件循环仍泵，所以能收到这次 close。
            // 必须是 daemon：accept() 还阻塞着时用户退出程序，不能因这条线程把 JVM 拖住
            Thread worker = new Thread(() -> {
                try {
                    blockingTask.run();
                } finally {
                    Platform.runLater(() -> {
                        if (holder[0] != null && holder[0].isShowing()) holder[0].close();
                        closed.countDown();
                    });
                }
            }, "dialog-wait-task");
            worker.setDaemon(true);
            worker.start();

            dialog.showAndWait();
        });
        try {
            closed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        Stage dialog = buildDialog(app, theme, kind, message, true);
        if (wait) dialog.showAndWait();
        else dialog.show();
    }

    /**
     * 组装一张取色 Material 卡片 + 透明场景 + 模态 Stage。
     * withOk=true 时带「确定」按钮（点它或回车/ESC 关窗）；false 时先不挂按钮，交给调用方补。
     */
    private static Stage buildDialog(Match3App app, Theme theme, Kind kind, String message, boolean withOk) {
        Label title = new Label(I18n.tr(kind.titleKey));
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + ";"
                + " -fx-font-size: 21px; -fx-font-weight: bold;");

        Label body = new Label(message);
        body.setWrapText(true);
        body.setPrefWidth(340);
        body.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 14px;");

        HBox header = new HBox(10, Icons.of(kind.ikon, theme.primary(), 26), title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(18, header, body);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(420);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle(cardStyle(theme));

        StackPane pane = new StackPane(card);
        pane.setStyle("-fx-padding: " + SHADOW_MARGIN + ";");
        Scene scene = new Scene(pane, Color.TRANSPARENT);
        String css = theme.stylesheet();
        if (css != null) scene.getStylesheets().add(css);

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.setScene(scene);
        dialog.setResizable(false);
        if (app.stage() != null) {
            dialog.initOwner(app.stage());
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        dialog.sizeToScene();
        dialog.centerOnScreen();

        if (withOk) {
            MFXButton ok = new MFXButton(I18n.tr("dlg.ok"));
            Styles.primary(ok, theme);
            ok.setMinWidth(120);
            ok.setOnAction(e -> dialog.close());
            card.getChildren().add(ok);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) dialog.close();
            });
        }
        return dialog;
    }

    /** 把卡片里的按钮区换成给定按钮（等待弹窗用取消按钮替换默认的确定）。 */
    private static void replaceButtons(Stage dialog, MFXButton button) {
        StackPane pane = (StackPane) dialog.getScene().getRoot();
        VBox card = (VBox) pane.getChildren().get(0);
        card.getChildren().add(button);
    }

    private static String cardStyle(Theme theme) {
        return "-fx-background-color: " + Theme.hex(theme.surface()) + ";"
                + " -fx-background-radius: 28;"
                + " -fx-border-color: " + Theme.hex(theme.outline()) + ";"
                + " -fx-border-radius: 28;"
                + " -fx-padding: 28 32 28 32;"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 28, 0.0, 0, 10);";
    }
}
