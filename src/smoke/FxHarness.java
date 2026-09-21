package smoke;

import javafx.application.Application;
import javafx.application.Platform;
import ui.Match3App;

import java.util.concurrent.CountDownLatch;

/** 在真实 JavaFX 环境里把窗口起起来，让探针能像玩家一样驱动它。 */
public final class FxHarness {

    private FxHarness() {
    }

    public static void boot() throws InterruptedException {
        Thread launcher = new Thread(() -> Application.launch(Match3App.class), "fx-boot");
        launcher.setDaemon(true);
        launcher.start();
        Match3App.STARTED.await();
    }

    /** 阻塞式地在 JavaFX 线程上跑一段；JavaFX 没有 invokeAndWait，自己用闩等。 */
    public static void onFx(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
            return;
        }
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                task.run();
            } finally {
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /** 同上，但把 JavaFX 线程上的计算结果带回来。 */
    public static <T> T onFxGet(java.util.function.Supplier<T> task) {
        if (Platform.isFxApplicationThread()) {
            return task.get();
        }
        CountDownLatch done = new CountDownLatch(1);
        T[] box = (T[]) new Object[1];
        Platform.runLater(() -> {
            try {
                box[0] = task.get();
            } finally {
                done.countDown();
            }
        });
        try {
            done.await();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
        return box[0];
    }

    public static void shutdown() {
        Platform.exit();
    }
}
