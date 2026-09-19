package controller;

import config.GameSettings;
import javafx.application.Platform;
import listener.GameListener;
import model.*;
import net.NetGame;
import ui.*;

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

/**
 * Controller is the connection between model and view,
 * when a Controller receive a request from a view, the Controller
 * analyzes and then hands over to the model for processing
 * [in this demo the request methods are onPlayerClickCell() and
 * onPlayerClickPiece()]
 */
public class GameController implements GameListener {

    /** 一枚棋子落到位的停顿。整批一起动就只能看到首尾两帧，没有"一个个下坠"。 */
    private static final int FALL_STEP_MS = 55;
    /** 消除后到开始下落之间的停顿，让"变空"这一帧看得见。 */
    private static final int ELIMINATE_PAUSE_MS = 120;

    private final CountDownLatch boardReady = new CountDownLatch(1);
    private final GameSettings settings;
    private final Board model;
    private final BoardView view;
    private final NetGame net;
    /** auto 确认动作的串行队列，避免连点时两步棋的业务逻辑互相穿插。 */
    private final ExecutorService autoConfirmWorker = Executors.newSingleThreadExecutor(r -> {
        Thread worker = new Thread(r, "auto-confirm");
        worker.setDaemon(true);
        return worker;
    });
    /** 下落动画跑在这上面：改视图回 EDT，停顿留在这一侧。 */
    private final ExecutorService fallAnimator = Executors.newSingleThreadExecutor(r -> {
        Thread worker = new Thread(r, "fall-animation");
        worker.setDaemon(true);
        return worker;
    });
    private final java.util.concurrent.atomic.AtomicBoolean falling =
            new java.util.concurrent.atomic.AtomicBoolean();
    private Thread autoModeThread;
    public boolean isAutoConfirm = false;
    public int timeLeft;
    private final GameScreen screen;
    private boolean isAutoMode = false;
    // Record whether there is a selected piece before
    private BoardPoint selectedPoint;
    private BoardPoint selectedPoint2;
    private int score, stepLeft;
    private boolean isAlive = true;
    private int victoryMode = 0; // 1=win 2=loss
    public Thread timerThread = new Thread(() -> {
        timeLeft = difficulty().timeLimit();
        refreshStatus();
        Log.info("Timer Start: " + difficulty().timeLimit() + "s");
        if (difficulty().timeLimit() != -1) {
            for (int i = difficulty().timeLimit(); i >= 0; i--) {
                if (!isAlive) break;
                pauseMilliSeconds(998);
                timeLeft--;
                refreshStatus();
                checkVictory();
                if (timeLeft % 10 == 0 || timeLeft <= 5) {
                    Log.info("TimeLeft:" + timeLeft);
                }
            }
        }
    });

    public GameController(BoardView view, Board model, NetGame net, GameSettings settings, GameScreen screen) {
        this.settings = settings;
        this.screen = screen;
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

    /** 当前难度。读档会改写它，所以每次都从 settings 取。 */
    private Difficulty difficulty() {
        return settings.difficulty();
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
        timeLeft = difficulty().timeLimit();
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }


    private void resetCounters() {
        timeLeft = difficulty().timeLimit();
        stepLeft = difficulty().stepLimit();
    }

    // When initialize from the gaming interface, this was used
    public void initialize() {
        score = 0;
        timeLeft = difficulty().timeLimit();
        victoryMode = 0;
        isAlive = true;
        do {
            view.removeAllTiles();
            this.model.initPieces();
            paintTilesFromModel();
        } while (isNotContinuable());

        refreshStatus();
        refreshStatus();
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
    public void onPlayerClickCell(BoardPoint point) {
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
            if (settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.autoShuffle"));
            onPlayerShuffle();
            return;
        }
        if (model.hasEmptyCells()) {
            if (settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.swapEmpty"));
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
                if (settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.swapNoMatch"));
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
            refreshStatus();
        }
    }

    /** 取格子上的棋子视图；格子空着时返回 null。 */
    private TileView tileAt(BoardPoint point) {
        return view.tileAt(point);
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
        if (matched.isEmpty()) return false;

        for (BoardPoint point : matched) {
            model.removePieceAt(point);
            score += 1;
        }
        runOnFx(() -> {
            for (BoardPoint point : matched) view.removeTileAt(point);
            view.repaint();
            refreshStatus();
        });
        pauseIfAnimating(ELIMINATE_PAUSE_MS);
        runOnFx(this::checkVictory);
        return true;
    }

    private void checkVictory() {
        if (!isAlive) return;
        if (score >= difficulty().goal()) {
            Dialogs.info(I18n.tr("msg.win"));
            Log.info("Victory: Reach the goal");
            playEffect("victory");
            victoryMode = 1;
            screen.finish();
            this.terminate();
        }
        if (score < difficulty().goal() && stepLeft == 0 || timeLeft <= 0 && difficulty().timeLimit() > 0) {
            if (!settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.lose"));
            else if (stepLeft == 0) {
                Dialogs.info(I18n.tr("msg.noSteps"));
                Log.info("Loss: Step limit exceeded");
            } else if (timeLeft <= 0 && difficulty().timeLimit() > 0) {
                Dialogs.info(I18n.tr("msg.noTime"));
                Log.info("Loss: Time limit exceeded");
            }
            victoryMode = 2;
            screen.finish();
            this.terminate();
        }
    }

    public void onPlayerNextStep() {
        new Thread(this::nextStep).start();
    }

    public void nextStep() {
        if (!model.hasEmptyCells()) {
            if (settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.nextStepNoEmpty"));
            Log.info("NextStep Fail: no empty cells");
            playWarning();
            return;
        }

        // 手动点按钮时这里是 EDT。下落要一帧一帧地停，睡在 EDT 上重绘根本发不出来，
        // 所以 EDT 上只提交任务；auto 线程本来就在后台，直接跑完再决定下一轮。
        if (Platform.isFxApplicationThread()) fallAnimator.execute(this::runGuardedFallCycle);
        else runGuardedFallCycle();
    }

    private void runGuardedFallCycle() {
        if (!falling.compareAndSet(false, true)) return;
        try {
            runFallCycle();
        } finally {
            falling.set(false);
        }
    }

    private void runFallCycle() {
        doFallDown();
        do {
            // Fall done has done, if there is any match-3, eliminate them
            if (settings.verboseDialogs()) {
                runOnFx(() -> {
                    Dialogs.info(I18n.tr("msg.bonus"));
                    Log.info(I18n.tr("msg.bonus"));
                });
            }
            runOnFx(() -> view.repaint());
            doFallDown();
        } while (doChessEliminate());

        stepLeft--;
        runOnFx(() -> {
            refreshStatus();
            checkVictory();
        });
    }

    /** 反复"顶部补棋 + 整列下落"，直到棋盘重新填满。每枚棋子各占一帧。 */
    private void doFallDown() {
        for (;;) {
            List<Spawn> spawned = model.refill();
            List<Move> moves = model.collapse();
            if (spawned.isEmpty() && moves.isEmpty()) return;
            for (Spawn spawn : spawned) {
                animateStep(() -> view.setTileAt(spawn.point(), new TileView(view.getCHESS_SIZE(), spawn.type())));
            }
            for (Move move : moves) {
                animateStep(() -> view.setTileAt(move.to(), view.removeTileAt(move.from())));
            }
        }
    }

    /** 动一下视图、重绘、停一帧。 */
    private void animateStep(Runnable mutation) {
        runOnFx(mutation);
        view.repaint();
        pauseIfAnimating(FALL_STEP_MS);
    }

    /** 只有在后台线程上才停——在 EDT 上睡等于把重绘一起堵死，动画就没了。 */
    private static void pauseIfAnimating(int ms) {
        if (!Platform.isFxApplicationThread()) pauseMilliSeconds(ms);
    }

    /** 四个状态位一次算好交给界面，控制器不再碰标签控件。 */
    public void refreshStatus() {
        screen.setStatus(
                difficulty().name(),
                score + "/" + difficulty().goal(),
                difficulty().hasStepLimit() ? (difficulty().stepLimit() - stepLeft) + "/" + difficulty().stepLimit() : "∞",
                difficulty().hasTimeLimit() ? timeLeft + "s" : "∞");
    }

    public void loadFromFile(File file) {
        if (!file.exists() || !file.canRead()) {
            Dialogs.info(I18n.tr("msg.fileNoAccess"));
            return;
        }
        String text;
        try {
            text = Files.readString(file.toPath());
        } catch (IOException _) {
            Dialogs.info(I18n.tr("msg.fileNoAccess"));
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
            Dialogs.info(I18n.tr("msg.fileFormat"));
            return;
        }
        applyState(loaded.get());
        Log.info("Difficulty:" + difficulty().name() + "\nLoaded.");
        refreshStatus();
        refreshStatus();
        if (restartTimer) {
            refreshStatus();
            startTimer();
        }
        view.repaint();
        checkVictory();
    }

    private void applyState(GameState state) {
        score = state.score();
        timeLeft = state.timeLeft();
        stepLeft = state.stepLeft();
        settings.setDifficulty(state.difficulty());
        view.removeAllTiles();
        for (BoardPoint point : model.points()) {
            PieceType type = state.board().typeAt(point);
            model.setPieceAt(point, type);
            if (type != null) view.setTileAt(point, new TileView(view.getCHESS_SIZE(), type));
        }
    }

    public GameState gameState() {
        return new GameState(score, timeLeft, stepLeft, settings.difficulty(), model.snapshot());
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
            Dialogs.info(I18n.tr("msg.win"));
            Log.info("Victory: Your Competitor Loss");
            victoryMode = 1;
        } else {
            Dialogs.info(I18n.tr("msg.competitorWin"));
            Log.info("Loss: Your Competitor Win");
            victoryMode = 2;
        }
        score = 0;
        isAlive = false;
        screen.finish();
    }

    public boolean isNotContinuable() {
        return !model.hasValidSwap();
    }

    /** 当前棋盘上一处可行的交换；没有就是死局。 */
    public Optional<Swap> currentHint() {
        return model.findHint();
    }

    public void hint() {
        if (model.hasEmptyCells()) return;
        Optional<Swap> hint = model.findHint();
        if (hint.isEmpty()) {
            Log.info("Dead end: shuffled");
            if (settings.verboseDialogs()) Dialogs.info(I18n.tr("msg.autoShuffle"));
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
            while (score <= difficulty().goal() && isAutoMode && isAlive) autoStep();
        }, "auto-mode");
        autoModeThread.setDaemon(true);
        autoModeThread.start();
    }

    /**
     * auto 的一轮：找一处可行交换，换掉它，再把空位补满。
     * 只有动 Swing 的那两步回 EDT；下落动画必须留在本线程上跑——
     * 它靠一帧一帧的停顿来表现，而 EDT 一旦被睡住，重绘就再也发不出来了。
     */
    private void autoStep() {
        runOnFx(this::hint);
        if (selectedPoint == null || selectedPoint2 == null) return;
        runOnFx(this::onPlayerSwapChess);
        nextStep();
    }

    // To handle auto confirm when it is on
    private void doAutoConfirm() {
        // 串行执行：连点时后一次不会和前一次的交换/落子重叠
        autoConfirmWorker.execute(() -> {
            if (selectedPoint == null || selectedPoint2 == null) return;
            autoStep();
        });
    }

    /**
     * 动视图的动作一律回到 JavaFX 应用线程；auto 线程和动画线程只是等它做完再继续。
     * JavaFX 没有 invokeAndWait，所以自己用闩等一下。
     */
    private static void runOnFx(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            task.run();
            return;
        }
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                Log.warn("Auto step failed: " + e);
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
        // 胜利时 checkVictory 和 returnToTitle 会各调一次，这里必须只生效一次
        if (!isAlive) return;
        isAlive = false;
        isAutoMode = false;
        autoConfirmWorker.shutdownNow();
        fallAnimator.shutdownNow();
        if (settings.playMode().isOnline()) net.stopHandler();
        else screen.finish();
    }

    public boolean isAlive() {
        return isAlive;
    }

    public BoardView getBoard() {
        return view;
    }

    public void setAutoConfirm(boolean autoConfirm) {
        this.isAutoConfirm = autoConfirm;
    }

    public int getVictoryMode() {
        return victoryMode;
    }



    public void startTimer() {
        timeLeft = difficulty().timeLimit();
        try {
            timerThread.start();
        } catch (IllegalThreadStateException _) {
        }
    }
}
