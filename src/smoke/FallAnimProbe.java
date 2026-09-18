package smoke;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import model.Board;
import model.BoardPoint;
import model.Difficulty;
import model.Swap;
import ui.Match3App;
import ui.TileView;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 下落是"一帧一帧"的还是"首尾两帧"，光看有没有异常证明不了。
 * 这里在动画期间反复采样棋盘上真实摆着的棋子，统计出现了多少种不同的画面：
 * 只有 2 种说明中间帧全被跳过了，或者 JavaFX 线程被睡住、重绘压根没发出来。
 * 只用命令行手动跑：`java -cp build/libs/match3.jar smoke.FallAnimProbe`
 */
public class FallAnimProbe {

    public static void main(String[] args) throws Exception {
        var settings = new GameSettings();
        settings.setDifficulty(new Difficulty("PROBE", 1_000_000, -1, -1));
        settings.setPlayMode(PlayMode.NEW_LOCAL);

        FxHarness.boot();
        FxHarness.onFx(() -> Match3App.get().showGame(settings));
        GameController controller = Match3App.get().controller();

        int manualFrames = measure(controller, false);
        int autoFrames = measure(controller, true);

        System.out.println("distinct frames (manual next-step) = " + manualFrames);
        System.out.println("distinct frames (auto mode)        = " + autoFrames);
        boolean ok = manualFrames >= 3 && autoFrames >= 3;
        System.out.println(ok ? "PASS" : "FAIL - fall animation is not rendering intermediate frames");
        FxHarness.shutdown();
        System.exit(ok ? 0 : 1);
    }

    private static int measure(GameController controller, boolean autoMode) throws Exception {
        FxHarness.onFx(() -> controller.setAutoMode(autoMode));
        Thread.sleep(300);

        Sampler sampler = new Sampler(controller);
        sampler.start();
        if (!autoMode) {
            // 先走出一次消除，让棋盘出现空洞，再点"下一步"
            Swap swap = hintOf(controller);
            if (swap != null) {
                FxHarness.onFx(() -> {
                    TileView first = controller.getBoard().tileAt(swap.first());
                    TileView second = controller.getBoard().tileAt(swap.second());
                    if (first != null && second != null) {
                        controller.onPlayerClickPiece(swap.first(), first);
                        controller.onPlayerClickPiece(swap.second(), second);
                        controller.onPlayerSwapChess();
                    }
                });
                FxHarness.onFx(controller::nextStep);
            }
        }
        Thread.sleep(2500);
        return sampler.stop();
    }

    private static Swap hintOf(GameController controller) {
        var holder = new AtomicReference<Swap>();
        FxHarness.onFx(() -> holder.set(controller.currentHint().orElse(null)));
        return holder.get();
    }

    /** 每 12ms 把棋盘上真实摆着的棋子拍一张快照。 */
    private static final class Sampler {
        private final GameController controller;
        private final Set<String> seen = new LinkedHashSet<>();
        private volatile boolean running = true;

        Sampler(GameController controller) {
            this.controller = controller;
        }

        void start() {
            Thread thread = new Thread(() -> {
                while (running) {
                    FxHarness.onFx(() -> seen.add(snapshot()));
                    try {
                        Thread.sleep(12);
                    } catch (InterruptedException _) {
                        return;
                    }
                }
            }, "fall-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        private String snapshot() {
            StringBuilder text = new StringBuilder();
            for (int row = 0; row < Board.DEFAULT_SIZE; row++) {
                for (int col = 0; col < Board.DEFAULT_SIZE; col++) {
                    TileView tile = controller.getBoard().tileAt(new BoardPoint(row, col));
                    text.append(tile == null ? '.' : tile.getType().textureIndex());
                }
            }
            return text.toString();
        }

        int stop() {
            running = false;
            return seen.size();
        }
    }
}
