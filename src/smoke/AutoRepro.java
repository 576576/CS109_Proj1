package smoke;

import controller.GameController;
import model.Board;
import model.BoardPoint;
import model.Difficulty;
import net.NetGame;
import view.CellComponent;
import view.GameFrame;
import view.TileView;

import javax.swing.*;

/**
 * 在真实 Swing 环境下跑一段时间 auto / auto-confirm，把后台线程抛出的异常抓出来。
 * 只用命令行手动跑：`java -cp build/libs/<fat jar> smoke.AutoRepro [秒数] [confirm]`
 * 它需要能创建窗口，所以不适合放进 CI。
 */
public class AutoRepro {

    public static void main(String[] args) throws Exception {
        var failures = new java.util.ArrayList<String>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            failures.add(thread.getName() + ": " + error);
            System.out.println("!!! UNCAUGHT in " + thread.getName() + ": " + error);
            for (StackTraceElement el : error.getStackTrace()) {
                String cls = el.getClassName();
                if (cls.startsWith("controller") || cls.startsWith("view") || cls.startsWith("model")) {
                    System.out.println("      at " + el);
                }
            }
        });

        int seconds = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        boolean autoConfirm = args.length > 1 && args[1].equals("confirm");
        var holder = new GameController[1];
        var board = new Board();

        // 目标定得极高、步数与时间不限，避免中途弹出胜利对话框把线程卡住
        view.MenuFrame.difficulty = new Difficulty("REPRO", 1_000_000, -1, -1);
        view.MenuFrame.startPlayMode = 1;

        SwingUtilities.invokeAndWait(() -> {
            GameFrame frame = new GameFrame(1100, 810);
            GameController controller = new GameController(frame.getBoardView(), board, new NetGame());
            frame.setGameController(controller);
            controller.setGameFrame(frame);
            GameFrame.isGameFrameInitDone = true;
            holder[0] = controller;
            if (autoConfirm) controller.isAutoConfirm = true;
            else controller.setAutoMode(true);
        });

        if (autoConfirm) {
            // 反复挑一处真能成三的交换，用点击把它走完，模拟开着 auto confirm 连点
            for (int i = 0; i < 12 && failures.isEmpty(); i++) {
                var hint = board.findHint();
                if (hint.isEmpty()) break;
                click(holder[0], hint.get().first());
                click(holder[0], hint.get().second());
                Thread.sleep(700);
            }
        } else {
            Thread.sleep(seconds * 1000L);
        }

        int score = holder[0].gameState().score();
        System.out.println("score after run = " + score);
        System.out.println(failures.isEmpty() && score > 0 ? "PASS" : "FAIL " + failures);
        System.exit(0);
    }

    /** 走 BoardView 的鼠标路径，等价于玩家点了这一格。 */
    private static void click(GameController controller, BoardPoint point) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameFrame frame = controller.getGameFrame();
            CellComponent cell = frame.getBoardView().getGridComponentAt(point);
            controller.onPlayerClickPiece(point, (TileView) cell.getComponent(0));
        });
    }
}
