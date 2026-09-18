package smoke;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import model.BoardPoint;
import model.Difficulty;
import model.Swap;
import ui.Match3App;
import ui.TileView;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 在真实 JavaFX 环境下跑一段时间 auto / auto-confirm，把后台线程抛出的异常抓出来。
 * 只用命令行手动跑：`java -cp build/libs/match3.jar smoke.AutoRepro [秒数] [confirm]`
 * 它要开窗口，所以不适合放进 CI。
 */
public class AutoRepro {

    public static void main(String[] args) throws Exception {
        var failures = new ArrayList<String>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            failures.add(thread.getName() + ": " + error);
            System.out.println("!!! UNCAUGHT in " + thread.getName() + ": " + error);
            Throwable cause = error.getCause() == null ? error : error.getCause();
            System.out.println("      cause: " + cause);
            for (StackTraceElement el : cause.getStackTrace()) {
                String cls = el.getClassName();
                if (cls.startsWith("controller") || cls.startsWith("ui") || cls.startsWith("model")
                        || cls.startsWith("smoke") || cls.startsWith("net")) {
                    System.out.println("      at " + el);
                }
            }
        });

        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        boolean autoConfirm = args.length > 1 && args[1].equals("confirm");

        // 目标定得极高、步数与时间不限，避免中途弹出胜利对话框把线程卡住
        var settings = new GameSettings();
        settings.setDifficulty(new Difficulty("REPRO", 1_000_000, -1, -1));
        settings.setPlayMode(PlayMode.NEW_LOCAL);

        FxHarness.boot();
        FxHarness.onFx(() -> Match3App.get().showGame(settings));
        GameController controller = Match3App.get().controller();

        if (autoConfirm) {
            controller.setAutoConfirm(true);
            // 反复挑一处真能成三的交换，用点击把它走完，模拟开着自动确认连点
            for (int i = 0; i < 12 && failures.isEmpty(); i++) {
                Swap swap = hintOf(controller);
                if (swap == null) break;
                click(controller, swap.first());
                click(controller, swap.second());
                Thread.sleep(700);
            }
        } else {
            controller.setAutoMode(true);
            Thread.sleep(seconds * 1000L);
        }

        int score = controller.gameState().score();
        System.out.println("score after run = " + score);
        System.out.println(failures.isEmpty() && score > 0 ? "PASS" : "FAIL " + failures);
        FxHarness.shutdown();
        System.exit(0);
    }

    private static Swap hintOf(GameController controller) {
        var holder = new AtomicReference<Swap>();
        FxHarness.onFx(() -> holder.set(controller.currentHint().orElse(null)));
        return holder.get();
    }

    /** 等价于玩家点了这一格。 */
    private static void click(GameController controller, BoardPoint point) {
        FxHarness.onFx(() -> {
            TileView tile = controller.getBoard().tileAt(point);
            if (tile != null) controller.onPlayerClickPiece(point, tile);
        });
    }
}
