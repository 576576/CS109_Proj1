package smoke;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import model.Board;
import model.BoardPoint;
import model.Difficulty;
import net.NetGame;
import view.CellComponent;
import view.GameFrame;
import view.TileView;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 下落是"一帧一帧"的还是"首尾两帧"，光看有没有异常证明不了。
 * 这里在动画期间反复采样棋盘视图上真实摆着的棋子，统计出现了多少种不同的画面：
 * 只有 2 种说明中间帧全被跳过了（或者 EDT 被睡住，重绘压根没发出来）。
 * 只用命令行手动跑：`java -cp build/libs/match3.jar smoke.FallAnimProbe`
 */
public class FallAnimProbe {

    public static void main(String[] args) throws Exception {
        var board = new Board();
        var holder = new GameController[1];

        var settings = new GameSettings();
        settings.setDifficulty(new Difficulty("PROBE", 1_000_000, -1, -1));
        settings.setPlayMode(PlayMode.NEW_LOCAL);

        SwingUtilities.invokeAndWait(() -> {
            GameFrame frame = new GameFrame(1100, 810, settings);
            GameController controller =
                    new GameController(frame.getBoardView(), board, new NetGame(settings), settings);
            frame.setGameController(controller);
            controller.setGameFrame(frame);
            holder[0] = controller;
        });

        int manualFrames = measure(holder[0], board, false);
        int autoFrames = measure(holder[0], board, true);

        System.out.println("distinct frames (manual next-step) = " + manualFrames);
        System.out.println("distinct frames (auto mode)        = " + autoFrames);
        boolean ok = manualFrames >= 3 && autoFrames >= 3;
        System.out.println(ok ? "PASS" : "FAIL - fall animation is not rendering intermediate frames");
        System.exit(ok ? 0 : 1);
    }

    private static int measure(GameController controller, Board board, boolean autoMode) throws Exception {
        SwingUtilities.invokeAndWait(() -> controller.setAutoMode(autoMode));
        Thread.sleep(300);

        Sampler sampler = new Sampler(controller);
        sampler.start();
        if (!autoMode) {
            // 先走出一次消除，让棋盘出现空洞，再点"下一步"
            SwingUtilities.invokeAndWait(() -> {
                Optional<model.Swap> hint = board.findHint();
                hint.ifPresent(swap -> {
                    controller.onPlayerClickPiece(swap.first(), tileAt(controller, swap.first()));
                    controller.onPlayerClickPiece(swap.second(), tileAt(controller, swap.second()));
                    controller.onPlayerSwapChess();
                });
            });
            SwingUtilities.invokeAndWait(controller::nextStep);
        }
        Thread.sleep(2500);
        return sampler.stop();
    }

    private static TileView tileAt(GameController controller, BoardPoint point) {
        CellComponent cell = controller.getGameFrame().getBoardView().getGridComponentAt(point);
        return cell.getComponentCount() == 0 ? null : (TileView) cell.getComponent(0);
    }

    /** 每 12ms 把棋盘视图上真实摆着的棋子拍一张快照。 */
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
                    try {
                        SwingUtilities.invokeAndWait(() -> seen.add(snapshot()));
                        Thread.sleep(12);
                    } catch (InterruptedException _) {
                        return;
                    } catch (Exception _) {
                        // 采样失败不影响判定
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
                    CellComponent cell = controller.getGameFrame().getBoardView()
                            .getGridComponentAt(new BoardPoint(row, col));
                    var components = cell.getComponents();
                    if (components.length == 0) text.append('.');
                    else if (components[0] instanceof TileView tile && tile.getType() != null) {
                        text.append(tile.getType().textureIndex());
                    } else text.append('?');
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
