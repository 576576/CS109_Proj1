package controller;

import listener.GameListener;
import model.*;
import net.NetGame;
import view.*;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static model.Chessboard.calculateDistance;
import static model.Constant.DEFAULT_CHESSBOARD_COL_SIZE;
import static model.Constant.DEFAULT_CHESSBOARD_ROW_SIZE;
import static player.MusicPlayer.*;
import static view.ChessComponent.chessTypes;
import static view.ChessGameFrame.isOnlinePlay;
import static view.MenuFrame.difficulty;
import static view.MenuFrame.isDetailedDialog;

/**
 * Controller is the connection between model and view,
 * when a Controller receive a request from a view, the Controller
 * analyzes and then hands over to the model for processing
 * [in this demo the request methods are onPlayerClickCell() and
 * onPlayerClickChessPiece()]
 */
public class GameController implements GameListener {

    public static boolean isAutoRestart = true;
    public static boolean isNewGameInitialized = false;
    private final Chessboard model;
    private final ChessboardComponent view;
    private final NetGame net;
    private final ArrayList<DifficultyPreset> difficultyPresets = new ArrayList<>();
    public boolean isAutoConfirm = false;
    public int timeLeft;
    private ChessGameFrame chessGameFrame;
    private boolean isAutoMode = false;
    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;
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
                System.out.print("");
            }
        }
        System.out.print("");
    });

    public GameController(ChessboardComponent view, Chessboard model, NetGame net) {
        initDifficultyPresets();
        this.view = view;
        this.model = model;
        this.net = net;
        net.registerController(this);
        view.registerController(this);
        view.initiateChessComponent(model);
        view.repaint();
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
        isNewGameInitialized = false;
        score = 0;
        timeLeft = difficulty.getTimeLimit();
        victoryMode = 0;
        isAlive = true;
        view.removeAllChessComponentsAtGrids();

        // call method to refresh a chessboard with new random pieces
        this.model.initPieces();
        // fetch contents of Chessboard to view
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                view.setChessComponentAtGrid(new ChessboardPoint(i, j), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
            }
        }

        updateDifficultyLabel();
        updateScoreAndStepLabel();
        view.repaint();
        System.out.println("New game initialized");
        isNewGameInitialized = true;
        if (isNotContinuable()) initialize();

        //complete it when restart game (auto-mode)
        if (isAutoMode) doAutoMode();
    }

    public void onPlayerShuffle() {
        view.removeAllChessComponentsAtGrids();

        // call method to refresh a chessboard with new random pieces
        this.model.initPieces();
        // fetch contents of Chessboard to view
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                view.setChessComponentAtGrid(new ChessboardPoint(i, j), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
            }
        }
        view.repaint();
        System.out.println("ChessBoard Shuffled");

        //complete it when restart game (auto-mode)
        if (isAutoMode) {
            doAutoMode();
        }
    }

    // click an empty cell
    @Override
    public void onPlayerClickCell(ChessboardPoint point, CellComponent component) {
        playWarning();
    }

    /*
    1. Click "confirm swap", and if OK the chess is swapped and eliminated. (otherwise do no swap, notice the user)
    2. Click “next step”, the upper chess will fall down.
    3. Click "next step" again, if this time, there are still 3-matches on the chessboard
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
        if (checkChessBoardHasEmpty()) {
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Swap Fail: chessboard has empty");
            System.out.println("Swap Fail: has empty");
            return;
        }
        checkVictory();
        playEffect("swap");
        try {
            // Swap, then check if they are matchable
            model.swapChessPiece(selectedPoint, selectedPoint2);
            ChessComponent tmp = view.removeChessComponentAtGrid(selectedPoint);
            view.setChessComponentAtGrid(selectedPoint, view.removeChessComponentAtGrid(selectedPoint2));
            view.setChessComponentAtGrid(selectedPoint2, tmp);
            view.repaint();

            if (isMatchable()) {
                doChessEliminate();
            } else {
                // Recover if there is nothing matchable
                playWarning();
                model.swapChessPiece(selectedPoint, selectedPoint2);
                view.setChessComponentAtGrid(selectedPoint2, view.removeChessComponentAtGrid(selectedPoint));
                view.setChessComponentAtGrid(selectedPoint, tmp);
                if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Swap Fail! Nothing can be match");
                System.out.println("Swap Fail: Nothing can be match");
            }
        } catch (Exception e) {
            System.out.println("Swap Failed!");
        } finally {
            if (selectedPoint != null) {
                try {
                    var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                    point1.setSelected(false);
                    point1.repaint();
                } catch (Exception ignored) {
                }
                selectedPoint = null;
            }
            if (selectedPoint2 != null) {
                try {
                    var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                    point2.setSelected(false);
                    point2.repaint();
                } catch (Exception ignored) {
                }
                selectedPoint2 = null;
            }
            view.repaint();
            updateScoreAndStepLabel();
        }
    }

    //to check the model to see if sth.'s matchable (3-match only, larger than 3 will be ignored).
    public boolean isMatchable() {
        return Chessboard.checkerBoardValidator(model.getGrid());
    }

    // do the elimination, only after chessboard has been checked
    // notice that there may be multiple matched simultaneously
    private boolean doChessEliminate() {
        int score_before = score;
        int rows = model.getGrid().length;
        int cols = model.getGrid()[0].length;
        boolean[][] labeledChess = new boolean[rows][cols];

        // Check for adjacency
        for (int i = 0; i < rows; i++) {
            int j = 0;
            while (j < cols - 2) {
                ChessPiece chess1 = model.getChessPieceAt(new ChessboardPoint(i, j));
                if (chess1 == null) {
                    j++;
                    continue;
                }
                int matchCount = 1;
                int k = j + 1;
                while (k < cols) {
                    ChessPiece chess2 = model.getChessPieceAt(new ChessboardPoint(i, k));
                    if (chess2 == null || !chess1.getName().equals(chess2.getName())) {
                        break;
                    }
                    matchCount++;
                    k++;
                }
                if (matchCount >= 3) {
                    for (int m = j; m < j + matchCount; m++) {
                        labeledChess[i][m] = true;
                    }
                }
                j = k;
            }
        }
        for (int j = 0; j < cols; j++) {
            int i = 0;
            while (i < rows - 2) {
                ChessPiece chess1 = model.getChessPieceAt(new ChessboardPoint(i, j));
                if (chess1 == null) {
                    i++;
                    continue;
                }
                int matchCount = 1;
                int k = i + 1;
                while (k < rows) {
                    ChessPiece chess2 = model.getChessPieceAt(new ChessboardPoint(k, j));
                    if (chess2 == null || !chess1.getName().equals(chess2.getName())) {
                        break;
                    }
                    matchCount++;
                    k++;
                }
                if (matchCount >= 3) {
                    for (int m = i; m < i + matchCount; m++) {
                        labeledChess[m][j] = true;
                    }
                }
                i = k;
            }
        }

        // do elimination
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (labeledChess[i][j]) {
                    model.removeChessPiece(new ChessboardPoint(i, j));
                    view.removeChessComponentAtGrid(new ChessboardPoint(i, j));
                    score += 1;
                }
            }
        }

        view.repaint();
        updateScoreAndStepLabel();
        checkVictory();
        return score > score_before;
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
        if (!checkChessBoardHasEmpty()) {
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

    private void doFallDown() {
        // check every column for empty cells
        boolean hasEmptyCell = true;
        while (hasEmptyCell) {
            hasEmptyCell = false;
            doGenerateRandomPiecesOnTop();
            for (int col = 0; col < DEFAULT_CHESSBOARD_COL_SIZE.getNum(); col++) {
                int row = DEFAULT_CHESSBOARD_ROW_SIZE.getNum() - 1;
                while (row > 0) {
                    if (this.model.getGrid()[row][col].getPiece() == null && !(this.model.getGrid()[row - 1][col].getPiece() == null)) {
                        // should do swap for model and view

                        ChessboardPoint upperCell = new ChessboardPoint(row - 1, col);
                        ChessboardPoint lowerCell = new ChessboardPoint(row, col);

                        model.swapChessPiece(upperCell, lowerCell);
                        view.setChessComponentAtGrid(lowerCell, view.removeChessComponentAtGrid(upperCell));
                        view.repaint();
                        pauseMilliSeconds(100);
                        hasEmptyCell = true;
                        row = DEFAULT_CHESSBOARD_ROW_SIZE.getNum() - 1;
                    }
                    row--;
                }
            }
        }
    }

    private void doGenerateRandomPiecesOnTop() {
        for (int j = 0; j < DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
            if (this.model.getGrid()[0][j].getPiece() == null) {
                this.model.getGrid()[0][j].setPiece(new ChessPiece(Util.RandomPick(chessTypes)));
                this.view.setChessComponentAtGrid(new ChessboardPoint(0, j), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[0][j].getPiece().getName())));
            }
        }
    }

    // Check if there is any empty cell on chessboard
    private boolean checkChessBoardHasEmpty() {
        for (int i = 0; i < DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                if (this.model.getGrid()[i][j].getPiece() == null) {
                    return true;
                }
            }
        }
        return false;
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
        if (!isFileExtensionName(file, "txt")) {
            JOptionPane.showMessageDialog(chessGameFrame, "File format error:101");
            return;
        }
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(chessGameFrame, "Can't Access the file!");
            return;
        }
        var csb = new int[8][8];
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
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                csb[i][j] = sc.hasNextInt() ? sc.nextInt() : new Random().nextInt(4);
            }
        }
        sc.close();
        System.out.println("Difficulty:" + difficulty.getName() + "\nLoaded from File:");
        view.removeAllChessComponentsAtGrids();
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            String str = Arrays.toString(csb[i]);
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("]", "");
            str = str.replaceAll(",", "");
            System.out.println(str);

            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                String pName = chessTypes[Math.min(Math.max(csb[j][i], 0), chessTypes.length)];
                view.setChessComponentAtGrid(new ChessboardPoint(j, i), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(pName)));
                model.setChessPiece(new ChessboardPoint(j, i), new ChessPiece(pName));
            }
        }
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
        var csb = new int[8][8];
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
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                csb[i][j] = sc.hasNextInt() ? sc.nextInt() : new Random().nextInt(chessTypes.length);
            }
        }
        System.out.println("Difficulty:" + difficulty.getName() + "\nLoaded from String:");
        System.out.println(score + " " + timeLeft + " " + stepLeft + " " + goal + " " + timeLimit + " " + stepLimit);
        try {
            view.removeAllChessComponentsAtGrids();
        } catch (Exception ignored) {
        }
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            String str = Arrays.toString(csb[i]);
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("]", "");
            str = str.replaceAll(",", "");
            System.out.println(str);

            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                String pName = chessTypes[Math.min(Math.max(csb[j][i], 0), chessTypes.length)];
                view.setChessComponentAtGrid(new ChessboardPoint(j, i), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(pName)));
                model.setChessPiece(new ChessboardPoint(j, i), new ChessPiece(pName));
            }
        }
        updateDifficultyLabel();
        updateScoreAndStepLabel();
        updateTimerLabel();
        startTimer();
        view.repaint();
        checkVictory();
    }

    @Override
    public void saveToFile(File file) {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(" ").append(timeLeft).append(" ").append(stepLeft).append(" ").append(difficulty.getGoal())
                .append(" ").append(difficulty.getTimeLimit()).append(" ").append(difficulty.getStepLimit()).append("\n");
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                var cp = model.getChessPieceAt(new ChessboardPoint(i, j)).getName();
                for (int k = 0; k < chessTypes.length; k++) {
                    if (cp.equals(chessTypes[k])) {
                        sb.append(k);
                        break;
                    }
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        System.out.println("Game Saved.\n" + sb);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(sb.toString());
            writer.flush();
            writer.close();
            System.out.println("at" + file.getAbsolutePath() + file.getName());
        } catch (IOException e) {
            System.err.println("Save Fail: IOException");
        }
    }

    public String ConvertToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(" ").append(timeLeft).append(" ").append(stepLeft).append(" ").append(difficulty.getGoal())
                .append(" ").append(difficulty.getTimeLimit()).append(" ").append(difficulty.getStepLimit()).append(" \n");
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                var cp = model.getChessPieceAt(new ChessboardPoint(i, j)).getName();
                for (int k = 0; k < chessTypes.length; k++) {
                    if (cp.equals(chessTypes[k])) {
                        sb.append(k);
                        break;
                    }
                }
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
    public void onPlayerClickChessPiece(ChessboardPoint point, ChessComponent component) {
        playEffect("chess_click");
        if (selectedPoint2 != null) {
            var distance2point1 = calculateDistance(point, selectedPoint);
            var distance2point2 = calculateDistance(point, selectedPoint2);
            var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
            var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
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

        var distance2point1 =
                Math.abs(selectedPoint.col() - point.col()) + Math.abs(selectedPoint.row() - point.row());

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
            var grid = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
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
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum() - 1; j++) {
                var p1 = new ChessboardPoint(i, j);
                var p2 = new ChessboardPoint(i, j + 1);
                model.swapChessPiece(p1, p2);
                if (isMatchable()) {
                    model.swapChessPiece(p1, p2);
                    return false;
                }
                model.swapChessPiece(p1, p2);
            }
        }
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum() - 1; i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                var p1 = new ChessboardPoint(i, j);
                var p2 = new ChessboardPoint(i + 1, j);
                model.swapChessPiece(p1, p2);
                if (isMatchable()) {
                    model.swapChessPiece(p1, p2);
                    return false;
                }
                model.swapChessPiece(p1, p2);
            }
        }
        return true;
    }

    public void hint() {
        if (checkChessBoardHasEmpty()) return;
        if (isNotContinuable()) {
            System.out.println("Dead end: shuffled");
            if (isDetailedDialog) JOptionPane.showMessageDialog(chessGameFrame, "Auto Shuffled: Dead end");
            onPlayerShuffle();
            return;
        }
        if (selectedPoint != null) {
            var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
            point1.setSelected(false);
            point1.repaint();
            selectedPoint = null;
        }
        if (selectedPoint2 != null) {
            var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
            point2.setSelected(false);
            point2.repaint();
            selectedPoint2 = null;
        }
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum() - 1; j++) {
                selectedPoint = new ChessboardPoint(i, j);
                selectedPoint2 = new ChessboardPoint(i, j + 1);
                model.swapChessPiece(selectedPoint, selectedPoint2);
                if (isMatchable()) {
                    model.swapChessPiece(selectedPoint, selectedPoint2);
                    var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                    var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                    point1.setSelected(true);
                    point2.setSelected(true);
                    point1.repaint();
                    point2.repaint();
                    return;
                }
                model.swapChessPiece(selectedPoint, selectedPoint2);
                selectedPoint = null;
                selectedPoint2 = null;
            }
        }
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum() - 1; i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                selectedPoint = new ChessboardPoint(i, j);
                selectedPoint2 = new ChessboardPoint(i + 1, j);
                model.swapChessPiece(selectedPoint, selectedPoint2);
                if (isMatchable()) {
                    model.swapChessPiece(selectedPoint, selectedPoint2);
                    var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                    var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                    point1.setSelected(true);
                    point2.setSelected(true);
                    point1.repaint();
                    point2.repaint();
                    return;
                }
                model.swapChessPiece(selectedPoint, selectedPoint2);
            }
        }
        JDialog jd = new JDialog(chessGameFrame, "Nothing can be done, please start a new game");
        jd.setVisible(true);
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

    public ChessGameFrame getChessGameFrame() {
        return chessGameFrame;
    }

    public void setChessGameFrame(ChessGameFrame chessGameFrame) {
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
