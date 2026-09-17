package controller;

import listener.GameListener;
import model.*;
import net.NetGame;
import view.*;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

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
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();
    private final CountDownLatch boardReady = new CountDownLatch(1);
    private final Board model;
    private final BoardView view;
    private final NetGame net;
    private final ArrayList<DifficultyPreset> difficultyPresets = new ArrayList<>();
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
        timeLeft = difficulty.getTimeLimit();
        updateTimerLabel();
        System.out.println("Timer Start: " + difficulty.getTimeLimit() + "s");
        if (difficulty.getTimeLimit() != -1) {
            for (int i = difficulty.getTimeLimit(); i >= 0; i--) {
                if (!isAlive) break;
                pauseMilliSeconds(998);
                timeLeft--;
                updateTimerLabel();
                checkVictory();
                if (timeLeft % 10 == 0 || timeLeft <= 5) {
                    System.out.println("TimeLeft:" + timeLeft);
                }
            }
        }
    });

    public GameController(BoardView view, Board model, NetGame net) {
        initDifficultyPresets();
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // For auto mode, to avoid it ends immediately
    // a workaround
    public static void pauseMilliSeconds(int ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    public void resetTimeLeft() {
        timeLeft = difficulty.getTimeLimit();
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public void setStatusLabels(JLabel[] statusLabels) {
        this.statusLabels = statusLabels;
    }

    private void initDifficultyPresets() {
        difficultyPresets.add(DifficultyPreset.EASY);
        difficultyPresets.add(DifficultyPreset.NORMAL);
        difficultyPresets.add(DifficultyPreset.HARD);
        timeLeft = difficulty.getTimeLimit();
        stepLeft = difficulty.getStepLimit();
    }

    // When initialize from the gaming interface, this was used
    public void initialize() {
        score = 0;
        timeLeft = difficulty.getTimeLimit();
        victoryMode = 0;
        isAlive = true;
        view.removeAllTiles();

        // call method to refresh a board with new random pieces
        this.model.initPieces();
        paintTilesFromModel();

        updateDifficultyLabel();
        updateScoreAndStepLabel();
        view.repaint();
        System.out.println("New game initialized");
        boardReady.countDown();
        if (isNotContinuable()) initialize();

        //complete it when restart game (auto-mode)
        if (isAutoMode) doAutoMode();
    }

    public void onPlayerShuffle() {
        view.removeAllTiles();

        // call method to refresh a board with new random pieces
        this.model.initPieces();
        paintTilesFromModel();
        view.repaint();
        System.out.println("Board Shuffled");

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
        if (isNotContinuable()) {
            System.out.println("Dead end: shuffled");
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Auto Shuffled: Dead end");
            onPlayerShuffle();
            return;
        }
        if (model.hasEmptyCells()) {
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Swap Fail: board has empty");
            System.out.println("Swap Fail: has empty");
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
                System.out.println("Swap Fail: Nothing can be match");
            }
        } catch (Exception e) {
            System.out.println("Swap Failed!");
        } finally {
            clearSelection(selectedPoint);
            clearSelection(selectedPoint2);
            selectedPoint = null;
            selectedPoint2 = null;
            view.repaint();
            updateScoreAndStepLabel();
        }
    }

    private void clearSelection(BoardPoint point) {
        if (point == null) return;
        try {
            var tile = (TileView) view.getGridComponentAt(point).getComponent(0);
            tile.setSelected(false);
            tile.repaint();
        } catch (Exception ignored) {
        }
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
        if (score >= difficulty.getGoal()) {
            JOptionPane.showMessageDialog(chessGameFrame, "Congratulations! You win.");
            System.out.println("Victory: Reach the goal");
            playEffect("victory");
            victoryMode = 1;
            chessGameFrame.returnToTitle();
            this.terminate();
        }
        if (score < difficulty.getGoal() && stepLeft == 0 || timeLeft <= 0 && difficulty.getTimeLimit() > 0) {
            if (!isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Oh no,you loss.");
            else if (stepLeft == 0) {
                JOptionPane.showMessageDialog(chessGameFrame, "Oh no, no more steps!");
                System.out.println("Loss: Step limit exceeded");
            } else if (timeLeft <= 0 && difficulty.getTimeLimit() > 0) {
                JOptionPane.showMessageDialog(chessGameFrame, "Oh no, you DON'T have time!");
                System.out.println("Loss: Time limit exceeded");
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
            System.out.println("NextStep Fail: no empty cells");
            playWarning();
            return;
        }

        doFallDown();
        do {
            // Fall done has done, if there is any match-3, eliminate them
            if (isDetailedDialog) {
                JOptionPane.showMessageDialog(chessGameFrame, "Bonus! Match occurs after falling down.");
                System.out.println("Bonus! Match occurs after falling down.");
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
        statusLabels[1].setText("Score:" + score + "/" + difficulty.getGoal());
        statusLabels[2].setText("StepLeft:" + ((difficulty.getStepLimit() > 0) ? (stepLeft + "/" + difficulty.getStepLimit()) : ('∞')));
    }

    public void updateDifficultyLabel() {
        if (statusLabels[0] == null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[0].setText("Difficulty:" + difficulty.getName());
    }

    public void updateTimerLabel() {
        if (difficulty.getTimeLimit() == -1) statusLabels[3].setText("TimeLimit:∞");
        else statusLabels[3].setText("TimeLimit:" + timeLeft);
    }

    public void loadFromFile(File file) {
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(chessGameFrame, "Can't Access the file!");
            return;
        }
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(chessGameFrame, "Can't Access the file!");
            return;
        }
        int[][] gridIndices = readSaveHead(sc);
        sc.close();
        System.out.println("Difficulty:" + difficulty.getName() + "\nLoaded from File:");
        applyGridIndices(gridIndices);
        updateDifficultyLabel();
        updateScoreAndStepLabel();
        view.repaint();
        checkVictory();
    }

    public void loadFromString(String string) {
        initialize();
        Scanner sc;
        try {
            sc = new Scanner(string);
        } catch (NullPointerException e) {
            throw new RuntimeException(e);
        }
        int[][] gridIndices = readSaveHead(sc);
        System.out.println("Difficulty:" + difficulty.getName() + "\nLoaded from String:");
        applyGridIndices(gridIndices);
        updateDifficultyLabel();
        updateScoreAndStepLabel();
        updateTimerLabel();
        startTimer();
        view.repaint();
        checkVictory();
    }

    /** 读取存档头部（分数/计时/步数与难度），再读满一整份棋盘的类目编号。 */
    private int[][] readSaveHead(Scanner sc) {
        int[][] gridIndices = new int[model.rows()][model.cols()];
        score = sc.nextInt();
        timeLeft = sc.nextInt();
        stepLeft = sc.nextInt();
        int goal = sc.nextInt(), timeLimit = sc.nextInt(), stepLimit = sc.nextInt();
        difficulty = new Difficulty(goal, stepLimit, timeLimit);
        for (var dp : difficultyPresets) {
            if (difficulty.equals(new Difficulty(dp))) {
                difficulty = new Difficulty(dp);
                break;
            }
        }
        for (int i = 0; i < model.rows(); i++) {
            for (int j = 0; j < model.cols(); j++) {
                gridIndices[i][j] = sc.hasNextInt() ? sc.nextInt() : RANDOM.nextInt(PieceType.values().length);
            }
        }
        System.out.println(score + " " + timeLeft + " " + stepLeft + " " + goal + " " + timeLimit + " " + stepLimit);
        return gridIndices;
    }

    private void applyGridIndices(int[][] gridIndices) {
        view.removeAllTiles();
        for (int i = 0; i < model.rows(); i++) {
            for (int j = 0; j < model.cols(); j++) {
                PieceType type = PieceType.ofIndex(Math.clamp(gridIndices[j][i], 0, PieceType.values().length - 1));
                BoardPoint point = new BoardPoint(j, i);
                view.setTileAt(point, new TileView(view.getCHESS_SIZE(), type));
                model.setPieceAt(point, type);
            }
        }
    }

    @Override
    public void saveToFile(File file) {
        String text = ConvertToString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(text);
            System.out.println("Game Saved at " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Save Fail: IOException");
        }
    }

    public String ConvertToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(" ").append(timeLeft).append(" ").append(stepLeft).append(" ").append(difficulty.getGoal())
                .append(" ").append(difficulty.getTimeLimit()).append(" ").append(difficulty.getStepLimit()).append(" \n");
        for (int i = 0; i < model.rows(); i++) {
            for (int j = 0; j < model.cols(); j++) {
                sb.append(model.pieceAt(new BoardPoint(i, j)).textureIndex());
                sb.append(" ");
            }
            sb.append("\n");
        }
        System.out.println("Converted to String:");
        System.out.println(sb);
        return sb.toString();
    }

    // click a cell with a chess
    @Override
    public void onPlayerClickPiece(BoardPoint point, TileView component) {
        playEffect("chess_click");
        if (selectedPoint2 != null) {
            var distance2point1 = point.distanceTo(selectedPoint);
            var distance2point2 = point.distanceTo(selectedPoint2);
            var point1 = (TileView) view.getGridComponentAt(selectedPoint).getComponent(0);
            var point2 = (TileView) view.getGridComponentAt(selectedPoint2).getComponent(0);
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
            var grid = (TileView) view.getGridComponentAt(selectedPoint).getComponent(0);
            if (grid == null) return;
            grid.setSelected(false);
            grid.repaint();

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
            System.out.println("Victory: Your Competitor Loss");
            victoryMode = 1;
        } else {
            JOptionPane.showMessageDialog(chessGameFrame, "Oh no! Your competitor win.");
            System.out.println("Loss: Your Competitor Win");
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
            System.out.println("Dead end: shuffled");
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Auto Shuffled: Dead end");
            onPlayerShuffle();
            return;
        }
        clearSelection(selectedPoint);
        clearSelection(selectedPoint2);
        Swap swap = hint.get();
        selectedPoint = swap.first();
        selectedPoint2 = swap.second();
        var tile1 = (TileView) view.getGridComponentAt(selectedPoint).getComponent(0);
        var tile2 = (TileView) view.getGridComponentAt(selectedPoint2).getComponent(0);
        tile1.setSelected(true);
        tile2.setSelected(true);
        tile1.repaint();
        tile2.repaint();
    }

    // Implement auto-mode
    private void doAutoMode() {
        // Create a new thread to run the auto mode logic.
        new Thread(() -> {
            while (score <= difficulty.getGoal() && isAutoMode && isAlive) {
                hint();
                onPlayerSwapChess();
                nextStep();
            }
        }).start();
    }

    // To handle auto confirm when it is on
    private void doAutoConfirm() {
        // Create a new thread to run the auto confirm logic.
        new Thread(() -> {
            while (isAutoConfirm && isAlive) {
                if (selectedPoint != null && selectedPoint2 != null) {
                    onPlayerSwapChess();
                    nextStep();
                }
            }
        }).start();
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
        if (isOnlinePlay()) NetGame.t.interrupt();
        else if (isAlive && isAutoRestart) {
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(chessGameFrame.menuFrame);
            SwingUtilities.invokeLater(() -> difficultySelectFrame.setVisible(true));
        }
        isAlive = false;
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
        timeLeft = difficulty.getTimeLimit();
        try {
            timerThread.start();
        } catch (Exception ignored) {
        }
    }
}
