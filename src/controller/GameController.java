package controller;

import listener.GameListener;
import model.*;
import net.NetGame;
import view.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.Log;
import static player.MusicPlayer.*;
import static view.GameFrame.isOnlinePlay;
import static view.MenuFrame.difficulty;
import static view.MenuFrame.isDetailedDialog;

/**
 * Controller is the connection between model and view,
 * when a Controller receive a request from a view, the Controller
 * analyzes and then hands over to the model for processing
 * [in this demo the request methods are onPlayerClickCell() and
 * onPlayerClickPiece()]
 */
public class GameController implements GameListener {

    public static boolean isAutoRestart = true;
    private final CountDownLatch boardReady = new CountDownLatch(1);
    private final Board model;
    private final BoardView view;
    private final NetGame net;
    /** auto 确认动作的串行队列，避免连点时两步棋的业务逻辑互相穿插。 */
    private final ExecutorService autoConfirmWorker = Executors.newSingleThreadExecutor(r -> {
        Thread worker = new Thread(r, "auto-confirm");
        worker.setDaemon(true);
        return worker;
    });
    private Thread autoModeThread;
    public boolean isAutoConfirm = false;
    public int timeLeft;
    private GameFrame chessGameFrame;
    private boolean isAutoMode = false;
    // Record whether there is a selected piece before
    private BoardPoint selectedPoint;
    private BoardPoint selectedPoint2;
    private int score, stepLeft;
    private boolean isAlive = true;
    private int victoryMode = 0; // 1=win 2=loss
    private JLabel[] statusLabels = new JLabel[4];
    public Thread timerThread = new Thread(() -> {
        timeLeft = difficulty.timeLimit();
        updateTimerLabel();
        Log.info("Timer Start: " + difficulty.timeLimit() + "s");
        if (difficulty.timeLimit() != -1) {
            for (int i = difficulty.timeLimit(); i >= 0; i--) {
                if (!isAlive) break;
                pauseMilliSeconds(998);
                timeLeft--;
                updateTimerLabel();
                checkVictory();
                if (timeLeft % 10 == 0 || timeLeft <= 5) {
                    Log.info("TimeLeft:" + timeLeft);
                }
            }
        }
    });

    public GameController(BoardView view, Board model, NetGame net) {
        resetCounters();
        this.view = view;
        this.model = model;
        this.net = net;
        net.registerController(this);
        view.registerController(this);
        this.model.initPieces();
        view.initiateTileViews(model);
        view.repaint();
        boardReady.countDown();
    }

    /** 等到第一副棋盘摆好为止。建房的一方要先有棋盘才能同步给对手。 */
    public void awaitBoardReady() {
        try {
            boardReady.await();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    // For auto mode, to avoid it ends immediately
    // a workaround
    public static void pauseMilliSeconds(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    public void resetTimeLeft() {
        timeLeft = difficulty.timeLimit();
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public void setStatusLabels(JLabel[] statusLabels) {
        this.statusLabels = statusLabels;
    }

    private void resetCounters() {
        timeLeft = difficulty.timeLimit();
        stepLeft = difficulty.stepLimit();
    }

    // When initialize from the gaming interface, this was used
    public void initialize() {
        score = 0;
        timeLeft = difficulty.timeLimit();
        victoryMode = 0;
        isAlive = true;
        do {
            view.removeAllTiles();
            this.model.initPieces();
            paintTilesFromModel();
        } while (isNotContinuable());

        updateDifficultyLabel();
        updateScoreAndStepLabel();
        view.repaint();
        Log.info("New game initialized");
        boardReady.countDown();

        //complete it when restart game (auto-mode)
        if (isAutoMode) doAutoMode();
    }

    public void onPlayerShuffle() {
        view.removeAllTiles();

        // call method to refresh a board with new random pieces
        this.model.initPieces();
        paintTilesFromModel();
        view.repaint();
        Log.info("Board Shuffled");

        //complete it when restart game (auto-mode)
        if (isAutoMode) {
            doAutoMode();
        }
    }

    private void paintTilesFromModel() {
        for (BoardPoint point : model.points()) {
            PieceType type = model.pieceAt(point);
            if (type != null) view.setTileAt(point, new TileView(view.getCHESS_SIZE(), type));
        }
    }

    // click an empty cell
    @Override
    public void onPlayerClickCell(BoardPoint point, CellComponent component) {
        playWarning();
    }

    /*
    1. Click "confirm swap", and if OK the chess is swapped and eliminated. (otherwise do no swap, notice the user)
    2. Click “next step”, the upper chess will fall down.
    3. Click "next step" again, if this time, there are still 3-matches on the board
        3.1 The click will cause these 3-match to be eliminated
        3.2 If there is not any 3-match randomly generate new pieces on the empty cells.
     */
    @Override
    public void onPlayerSwapChess() {
        if (selectedPoint == null || selectedPoint2 == null) {
            Log.info("Swap Fail: less than two pieces selected");
            return;
        }
        if (isNotContinuable()) {
            Log.info("Dead end: shuffled");
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Auto Shuffled: Dead end");
            onPlayerShuffle();
            return;
        }
        if (model.hasEmptyCells()) {
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Swap Fail: board has empty");
            Log.info("Swap Fail: has empty");
            return;
        }
        checkVictory();
        playEffect("swap");
        try {
            // Swap, then check if they are matchable
            model.swapPieces(selectedPoint, selectedPoint2);
            TileView tmp = view.removeTileAt(selectedPoint);
            view.setTileAt(selectedPoint, view.removeTileAt(selectedPoint2));
            view.setTileAt(selectedPoint2, tmp);
            view.repaint();

            if (isMatchable()) {
                doChessEliminate();
            } else {
                // Recover if there is nothing matchable
                playWarning();
                model.swapPieces(selectedPoint, selectedPoint2);
                view.setTileAt(selectedPoint2, view.removeTileAt(selectedPoint));
                view.setTileAt(selectedPoint, tmp);
                if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Swap Fail! Nothing can be match");
                Log.info("Swap Fail: Nothing can be match");
            }
        } catch (RuntimeException e) {
            Log.warn("Swap Failed: " + e);
        } finally {
            clearSelection(selectedPoint);
            clearSelection(selectedPoint2);
            selectedPoint = null;
            selectedPoint2 = null;
            view.repaint();
            updateScoreAndStepLabel();
        }
    }

    /** 取格子上的棋子视图；格子空着或还没摆上棋子时返回 null。 */
    private TileView tileAt(BoardPoint point) {
        if (point == null) return null;
        CellComponent cell = view.getGridComponentAt(point);
        if (cell.getComponentCount() == 0) return null;
        return cell.getComponent(0) instanceof TileView tile ? tile : null;
    }

    private void clearSelection(BoardPoint point) {
        TileView tile = tileAt(point);
        if (tile == null) return;
        tile.setSelected(false);
        tile.repaint();
    }

    //to check the model to see if sth.'s matchable (3-match only, larger than 3 will be ignored).
    public boolean isMatchable() {
        return !model.listMatches().isEmpty();
    }

    // do the elimination, only after board has been checked
    // notice that there may be multiple matched simultaneously
    private boolean doChessEliminate() {
        List<BoardPoint> matched = model.listMatches();
        for (BoardPoint point : matched) {
            model.removePieceAt(point);
            view.removeTileAt(point);
            score += 1;
        }
        if (matched.isEmpty()) return false;

        view.repaint();
        updateScoreAndStepLabel();
        checkVictory();
        return true;
    }

    private void checkVictory() {
        if (!isAlive) return;
        if (score >= difficulty.goal()) {
            JOptionPane.showMessageDialog(chessGameFrame, "Congratulations! You win.");
            Log.info("Victory: Reach the goal");
            playEffect("victory");
            victoryMode = 1;
            chessGameFrame.returnToTitle();
            this.terminate();
        }
        if (score < difficulty.goal() && stepLeft == 0 || timeLeft <= 0 && difficulty.timeLimit() > 0) {
            if (!isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Oh no,you loss.");
            else if (stepLeft == 0) {
                JOptionPane.showMessageDialog(chessGameFrame, "Oh no, no more steps!");
                Log.info("Loss: Step limit exceeded");
            } else if (timeLeft <= 0 && difficulty.timeLimit() > 0) {
                JOptionPane.showMessageDialog(chessGameFrame, "Oh no, you DON'T have time!");
                Log.info("Loss: Time limit exceeded");
            }
            victoryMode = 2;
            chessGameFrame.returnToTitle();
            this.terminate();
        }
    }

    public void onPlayerNextStep() {
        new Thread(this::nextStep).start();
    }

    public void nextStep() {
        if (!model.hasEmptyCells()) {
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "NextStep failed: no empty");
            Log.info("NextStep Fail: no empty cells");
            playWarning();
            return;
        }

        doFallDown();
        do {
            // Fall done has done, if there is any match-3, eliminate them
            if (isDetailedDialog) {
                JOptionPane.showMessageDialog(chessGameFrame, "Bonus! Match occurs after falling down.");
                Log.info("Bonus! Match occurs after falling down.");
            }
            view.repaint();
            doFallDown();
        } while (doChessEliminate());

        stepLeft--;
        updateScoreAndStepLabel();
        checkVictory();
    }

    /** 反复“顶部补棋 + 整列下落”，直到棋盘重新填满。 */
    private void doFallDown() {
        for (;;) {
            List<Spawn> spawned = model.refill();
            for (Spawn spawn : spawned) {
                view.setTileAt(spawn.point(), new TileView(view.getCHESS_SIZE(), spawn.type()));
            }
            List<Move> moves = model.collapse();
            for (Move move : moves) {
                view.setTileAt(move.to(), view.removeTileAt(move.from()));
            }
            if (spawned.isEmpty() && moves.isEmpty()) return;
            view.repaint();
            pauseMilliSeconds(100);
        }
    }

    public void updateScoreAndStepLabel() {
        if (statusLabels[0] == null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[1].setText("Score:" + score + "/" + difficulty.goal());
        statusLabels[2].setText("StepLeft:" + ((difficulty.stepLimit() > 0) ? (stepLeft + "/" + difficulty.stepLimit()) : ('∞')));
    }

    public void updateDifficultyLabel() {
        if (statusLabels[0] == null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[0].setText("Difficulty:" + difficulty.name());
    }

    public void updateTimerLabel() {
        if (difficulty.timeLimit() == -1) statusLabels[3].setText("TimeLimit:∞");
        else statusLabels[3].setText("TimeLimit:" + timeLeft);
    }

    public void loadFromFile(File file) {
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(chessGameFrame, "Can't Access the file!");
            return;
        }
        String text;
        try {
            text = Files.readString(file.toPath());
        } catch (IOException _) {
            JOptionPane.showMessageDialog(chessGameFrame, "Can't Access the file!");
            return;
        }
        loadFromState(text, false);
    }

    public void loadFromString(String string) {
        initialize();
        loadFromState(string, true);
    }

    /** 读档：旧档与新档都交给 GameStateCodec 解析。 */
    private void loadFromState(String text, boolean restartTimer) {
        Optional<GameState> loaded = GameStateCodec.fromText(text, model.rows(), model.cols());
        if (loaded.isEmpty()) {
            JOptionPane.showMessageDialog(chessGameFrame, "File format error:101");
            return;
        }
        applyState(loaded.get());
        Log.info("Difficulty:" + difficulty.name() + "\nLoaded.");
        updateDifficultyLabel();
        updateScoreAndStepLabel();
        if (restartTimer) {
            updateTimerLabel();
            startTimer();
        }
        view.repaint();
        checkVictory();
    }

    private void applyState(GameState state) {
        score = state.score();
        timeLeft = state.timeLeft();
        stepLeft = state.stepLeft();
        difficulty = state.difficulty();
        view.removeAllTiles();
        for (BoardPoint point : model.points()) {
            PieceType type = state.board().typeAt(point);
            model.setPieceAt(point, type);
            if (type != null) view.setTileAt(point, new TileView(view.getCHESS_SIZE(), type));
        }
    }

    public GameState gameState() {
        return new GameState(score, timeLeft, stepLeft, difficulty, model.snapshot());
    }

    public String gameStateText() {
        return GameStateCodec.toText(gameState());
    }

    @Override
    public void saveToFile(File file) {
        try {
            Files.writeString(file.toPath(), gameStateText());
            Log.info("Game Saved at " + file.getAbsolutePath());
        } catch (IOException _) {
            Log.warn("Save Fail: IOException");
        }
    }

    // click a cell with a chess
    @Override
    public void onPlayerClickPiece(BoardPoint point, TileView component) {
        playEffect("chess_click");
        if (selectedPoint2 != null) {
            var distance2point1 = point.distanceTo(selectedPoint);
            var distance2point2 = point.distanceTo(selectedPoint2);
            var point1 = tileAt(selectedPoint);
            var point2 = tileAt(selectedPoint2);
            if (distance2point1 == 0 && point1 != null) {
                point1.setSelected(false);
                point1.repaint();
                selectedPoint = selectedPoint2;
                selectedPoint2 = null;
            } else if (distance2point2 == 0 && point2 != null) {
                point2.setSelected(false);
                point2.repaint();
                selectedPoint2 = null;
            } else if (distance2point1 == 1 && point2 != null) {
                point2.setSelected(false);
                point2.repaint();
                selectedPoint2 = point;
                component.setSelected(true);
                component.repaint();
                if (isAutoConfirm() && !isAutoMode()) {
                    doAutoConfirm();
                }
            } else if (distance2point2 == 1 && point1 != null) {
                point1.setSelected(false);
                point1.repaint();
                selectedPoint = selectedPoint2;
                selectedPoint2 = point;
                component.setSelected(true);
                component.repaint();
                // should do auto confirm only on auto confirm is on, and auto mode is off
                if (isAutoConfirm() && !isAutoMode()) {
                    doAutoConfirm();
                }
            } else if (distance2point1 > 1 && distance2point2 > 1) {
                point1.setSelected(false);
                point2.setSelected(false);
                point1.repaint();
                point2.repaint();
                selectedPoint = point;
                selectedPoint2 = null;
                component.setSelected(true);
                component.repaint();
            }
            return;
        }


        if (selectedPoint == null) {
            selectedPoint = point;
            component.setSelected(true);
            component.repaint();
            return;
        }

        var distance2point1 = point.distanceTo(selectedPoint);

        if (distance2point1 == 0) {
            selectedPoint = null;
            component.setSelected(false);
            component.repaint();
            return;
        }

        if (distance2point1 == 1) {
            selectedPoint2 = point;
            if (isAutoConfirm() && !isAutoMode()) {
                doAutoConfirm();
            }
        } else {
            clearSelection(selectedPoint);
            selectedPoint = point;
        }
        component.setSelected(true);
        component.repaint();
    }

    public void onPlayerHostGame() {
        net.serverHost();
    }

    public void onPlayerJoinGame() {
        net.connectHost();
    }

    public void onlineGameTerminate(boolean isWinner) {
        if (isWinner) {
            JOptionPane.showMessageDialog(chessGameFrame, "Congratulations! You win.");
            Log.info("Victory: Your Competitor Loss");
            victoryMode = 1;
        } else {
            JOptionPane.showMessageDialog(chessGameFrame, "Oh no! Your competitor win.");
            Log.info("Loss: Your Competitor Win");
            victoryMode = 2;
        }
        score = 0;
        isAlive = false;
        chessGameFrame.returnToTitle();
    }

    public boolean isNotContinuable() {
        return !model.hasValidSwap();
    }

    public void hint() {
        if (model.hasEmptyCells()) return;
        Optional<Swap> hint = model.findHint();
        if (hint.isEmpty()) {
            Log.info("Dead end: shuffled");
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Auto Shuffled: Dead end");
            onPlayerShuffle();
            return;
        }
        clearSelection(selectedPoint);
        clearSelection(selectedPoint2);
        Swap swap = hint.get();
        selectedPoint = swap.first();
        selectedPoint2 = swap.second();
        TileView tile1 = tileAt(selectedPoint);
        TileView tile2 = tileAt(selectedPoint2);
        if (tile1 == null || tile2 == null) {
            selectedPoint = null;
            selectedPoint2 = null;
            return;
        }
        tile1.setSelected(true);
        tile2.setSelected(true);
        tile1.repaint();
        tile2.repaint();
    }

    // Implement auto-mode
    private void doAutoMode() {
        // 已经有一个循环在跑了就别再开一条，否则每次洗牌都会多出一条永不退出的线程
        if (autoModeThread != null && autoModeThread.isAlive()) return;
        autoModeThread = new Thread(() -> {
            while (score <= difficulty.goal() && isAutoMode && isAlive) {
                runOnEdt(this::autoStep);
            }
        }, "auto-mode");
        autoModeThread.setDaemon(true);
        autoModeThread.start();
    }

    /** auto 的一轮：找一处可行交换，换掉它，再把空位补满。 */
    private void autoStep() {
        hint();
        if (selectedPoint == null || selectedPoint2 == null) return;
        onPlayerSwapChess();
        nextStep();
    }

    // To handle auto confirm when it is on
    private void doAutoConfirm() {
        // 串行执行：连点时后一次不会和前一次的交换/落子重叠
        autoConfirmWorker.execute(() -> {
            if (selectedPoint == null || selectedPoint2 == null) return;
            runOnEdt(this::autoStep);
        });
    }

    /** 走一步棋要动视图，必须回到 EDT；auto 线程只是等它做完再决定下一轮。 */
    private static void runOnEdt(Runnable task) {
        try {
            if (SwingUtilities.isEventDispatchThread()) task.run();
            else SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            Log.warn("Auto step failed: " + e.getCause());
        }
    }

    public boolean isAutoConfirm() {
        return isAutoConfirm;
    }

    public boolean isAutoMode() {
        return isAutoMode;
    }

    public void setAutoMode(boolean autoMode) {
        isAutoMode = autoMode;
        if (isAutoMode()) doAutoMode();
    }

    @Override
    public void terminate() {
        isAlive = false;
        isAutoMode = false;
        autoConfirmWorker.shutdownNow();
        if (isOnlinePlay()) NetGame.t.interrupt();
        else if (isAutoRestart) {
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(chessGameFrame.menuFrame);
            SwingUtilities.invokeLater(() -> difficultySelectFrame.setVisible(true));
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public int getVictoryMode() {
        return victoryMode;
    }

    public GameFrame getGameFrame() {
        return chessGameFrame;
    }

    public void setGameFrame(GameFrame chessGameFrame) {
        this.chessGameFrame = chessGameFrame;
    }

    public void startTimer() {
        timeLeft = difficulty.timeLimit();
        try {
            timerThread.start();
        } catch (IllegalThreadStateException _) {
        }
    }
}
