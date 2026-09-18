package ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** 统一的对话框出口。调用方可能在后台线程上，所以一律先切回 JavaFX 线程。 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void info(String message) {
        show(Alert.AlertType.INFORMATION, "Match-3", message);
    }

    public static void warn(String message) {
        show(Alert.AlertType.WARNING, "Match-3", message);
    }

    public static void error(String message) {
        show(Alert.AlertType.ERROR, "Match-3", message);
    }

    /** 不阻塞的通知：建房等待对手时用它，调用方还得接着往下走。 */
    public static void notifyInfo(String message) {
        if (Platform.isFxApplicationThread()) build(Alert.AlertType.INFORMATION, "Match-3", message).show();
        else Platform.runLater(() -> build(Alert.AlertType.INFORMATION, "Match-3", message).show());
    }

    public static void show(Alert.AlertType type, String title, String message) {
        if (Platform.isFxApplicationThread()) {
            build(type, title, message).showAndWait();
            return;
        }
        Platform.runLater(() -> build(type, title, message).showAndWait());
    }

    private static Alert build(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert;
    }
}
