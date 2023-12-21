package controller;

import listener.GameListener;
import model.*;
import net.NetGame;
import view.CellComponent;
import view.ChessComponent;
import view.ChessboardComponent;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 * Controller is the connection between model and view,
 * when a Controller receive a request from a view, the Controller
 * analyzes and then hands over to the model for processing
 * [in this demo the request methods are onPlayerClickCell() and
 * onPlayerClickChessPiece()]
 */
public class GameController implements GameListener {

    private final Chessboard model;
    private final ChessboardComponent view;
    private final NetGame net;
    private boolean isAutoConfirm=false;

    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;

    private int score, timeLeft, stepLeft;
    private Difficulty difficulty;
    private final ArrayList<DifficultyPreset> difficultyPresets = new ArrayList<>();
    private JLabel statusLabel, difficultyLabel;

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getDifficultyLabel() {
        return difficultyLabel;
    }

    public void setStatusLabel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
    }

    public void setDifficultyLabel(JLabel difficultyLabel) {
        this.difficultyLabel = difficultyLabel;
    }

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

    private void initDifficultyPresets() {
        difficulty=new Difficulty(DifficultyPreset.EASY);
        difficultyPresets.add(DifficultyPreset.EASY);
        difficultyPresets.add(DifficultyPreset.NORMAL);
        difficultyPresets.add(DifficultyPreset.HARD);
        timeLeft=difficulty.getTimeLimit();
        stepLeft=difficulty.getStepLimit();
    }

    // When initialize from the gaming interface, this was used
    public void initialize() {
        boolean needToInit=true;
        score = 0;
        statusLabel.setText("Score:" + score);
        view.removeAllChessComponentsAtGrids();

        // call method to refresh a chessboard with new random pieces
        this.model.initPieces();
        // fetch contents of Chessboard to view
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                view.setChessComponentAtGrid(new ChessboardPoint(j, i), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
            }
        }
        //todo: complete it when restart game (auto-mode)

        updateStatusLabel();
        view.repaint();
        System.out.println("New game initialized");
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    // click an empty cell
    @Override
    public void onPlayerClickCell(ChessboardPoint point, CellComponent component) {
    }

    @Override
    public void onPlayerSwapChess() {
        try {
            model.swapChessPiece(selectedPoint, selectedPoint2);
            if (!isMatchable()) {
                model.swapChessPiece(selectedPoint, selectedPoint2);
                System.out.println("Swap Fail! Nothing can be match");
            } else {
                ChessComponent tmp = view.removeChessComponentAtGrid(selectedPoint);
                view.setChessComponentAtGrid(selectedPoint, view.removeChessComponentAtGrid(selectedPoint2));
                view.setChessComponentAtGrid(selectedPoint2, tmp);
            }
        } catch (NullPointerException e) {
            System.out.println("Swap Failed!");
            selectedPoint = null;
            selectedPoint2 = null;
        } finally {
            view.repaint();
        }
    }

    public boolean isMatchable() {
        //TODO:to check the model to see if sth.'s matchable.
        return Chessboard.checkerBoardValidator(model.getGrid());
    }

    @Override
    public void onPlayerNextStep() {
        //TODO:onPlayerNextStep
        score++;
        stepLeft--;
        updateStatusLabel();
        System.out.println("Score updated:" + score);
    }
    public void updateStatusLabel(){
        statusLabel.setText("StepLeft:" +((difficulty.getStepLimit()>0)?(stepLeft + "/"+difficulty.getStepLimit()):('∞')) + "  Score:" + score + "/" + difficulty.getGoal());
    }
    public void loadFromFile(File file) {
        if (!file.exists() || !file.canRead()) return;
        Scanner sc;
        try {
            sc = new Scanner(file);
        } catch (FileNotFoundException e) {
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
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                csb[i][j] = sc.hasNextInt() ? sc.nextInt() : new Random().nextInt(4);
            }
        }
        sc.close();
        System.out.println("Game Loaded.\n" + score+" "+difficulty.getName());
        view.removeAllChessComponentsAtGrids();
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            String str = Arrays.toString(csb[i]);
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("]", "");
            str = str.replaceAll(",", "");
            System.out.println(str);

            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                String pName = new String[]{"💎", "⚪", "▲", "🔶"}[Math.min(Math.max(csb[j][i], 0), 3)];
                view.setChessComponentAtGrid(new ChessboardPoint(j, i), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(pName)));
                model.setChessPiece(new ChessboardPoint(j, i), new ChessPiece(pName));
            }
        }
        updateStatusLabel();
        view.repaint();
    }

    @Override
    public void saveToFile(File file) {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(" ").append(timeLeft).append(" ").append(stepLeft).append(" ").append(difficulty.getGoal())
                .append(" ").append(difficulty.getTimeLimit()).append(" ").append(difficulty.getStepLimit()).append("\n");
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                var cp = model.getChessPieceAt(new ChessboardPoint(i, j)).getName();
                switch (cp) {
                    case "💎":
                        sb.append(0);
                        break;
                    case "⚪":
                        sb.append(1);
                        break;
                    case "▲":
                        sb.append(2);
                        break;
                    default:
                        sb.append(3);
                        break;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // click a cell with a chess
    @Override
    public void onPlayerClickChessPiece(ChessboardPoint point, ChessComponent component) {
        if (selectedPoint2 != null) {
            var distance2point1 =
                    Math.abs(selectedPoint.col() - point.col()) + Math.abs(selectedPoint.row() - point.row());
            var distance2point2 =
                    Math.abs(selectedPoint2.col() - point.col()) + Math.abs(selectedPoint2.row() - point.row());
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
            } else if (distance2point2 == 1 && point1 != null) {
                point1.setSelected(false);
                point1.repaint();
                selectedPoint = selectedPoint2;
                selectedPoint2 = point;
                component.setSelected(true);
                component.repaint();
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

    public String getNetGameData() {
        return difficulty.getDifficultyInfo() + " " + score;
    }

    @Override
    public void terminate() {
        //TODO:terminate the game
    }
    public void hint(){
        //todo:introduce hint function
    }

    public void setAutoConfirm(boolean autoConfirm) {
        isAutoConfirm = autoConfirm;
    }

    public boolean isAutoConfirm() {
        return isAutoConfirm;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}
