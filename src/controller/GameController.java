package controller;

import listener.GameListener;
import model.*;
import net.NetGame;
import view.CellComponent;
import view.ChessComponent;
import view.ChessGameFrame;
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
    private ChessGameFrame chessGameFrame;

    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;

    private int score, timeLeft, stepLeft;
    private Difficulty difficulty;
    private final ArrayList<DifficultyPreset> difficultyPresets = new ArrayList<>();
    private JLabel[] statusLabels = new JLabel[4];
    public void setStatusLabels(JLabel[] statusLabels) {
        this.statusLabels = statusLabels;
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
    public void setChessGameFrame(ChessGameFrame chessGameFrame){
        this.chessGameFrame=chessGameFrame;
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

        updateScoreAndStepLabel();
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

    /*
    1. Click "confirm swap", and if OK the chess is swapped and eliminated. (otherwise do no swap, notice the user)
    2. Click “next step”, the upper chess will fall down.
    3. Click "next step" again, if this time, there are still 3-matches on the chessboard
        3.1 The click will cause these 3-match to be eliminated
        3.2 If there is not any 3-match randomly generate new pieces on the empty cells.
     */
    @Override
    public void onPlayerSwapChess() {
        try {
            // Try to swap, then check if they are matchable
            model.swapChessPiece(selectedPoint, selectedPoint2);
            if (!isMatchable()) {
                // Do nothing if there is nothing matchable
                model.swapChessPiece(selectedPoint, selectedPoint2);
                System.out.println("Swap Fail! Nothing can be match");
            } else {
                // Do swap two chess (in view) if matchable
                // TODO: may need animation for swap and eliminate
                ChessComponent tmp = view.removeChessComponentAtGrid(selectedPoint);
                view.setChessComponentAtGrid(selectedPoint, view.removeChessComponentAtGrid(selectedPoint2));
                view.setChessComponentAtGrid(selectedPoint2, tmp);
                // Do the elimination
                doChessEliminate();
                //TODO: cancel cell selection after eliminate? it may cause null pointer exception
            }
        } catch (NullPointerException e) {
            // if the selected cell contains empty content, do nothing
            System.out.println("Swap Failed!");
            selectedPoint = null;
            selectedPoint2 = null;
        } finally {
            view.repaint();
        }
    }

    //to check the model to see if sth.'s matchable (3-match only, larger than 3 will be ignored).
    public boolean isMatchable() {
        return Chessboard.checkerBoardValidator(model.getGrid());
    }

    // do the elimination, only after chessboard has been checked
    // notice that there may be multiple matched simultaneously
    private void doChessEliminate() {
        int rows = model.getGrid().length;
        int cols = model.getGrid()[0].length;

        // For debug only. Print chessboard of model to manually see what it is.
        //Chessboard.printChessBoardGrid(model.getGrid());

        // Check for horizontal adjacency
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols - 2; j++) {
                //TODO:FIX THIS

            }
        }

        // Check for vertical adjacency
        for (int i = 0; i < rows - 2; i++) {
            for (int j = 0; j < cols; j++) {
                //TODO:FIX THIS

            }
        }
    }

    @Override
    public void onPlayerNextStep() {
        //TODO:onPlayerNextStep
        score++;
        stepLeft--;
        updateScoreAndStepLabel();
        System.out.println("Score updated:" + score);
    }
    public void updateScoreAndStepLabel(){
        if (statusLabels[0]==null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[1].setText("Score:" + score + "/" + difficulty.getGoal());
        statusLabels[2].setText("StepLeft:" +((difficulty.getStepLimit()>0)?(stepLeft + "/"+difficulty.getStepLimit()):('∞')));
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
        updateScoreAndStepLabel();
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
    public void onlineGameTerminate(boolean isWinner){
        //TODO:(576)terminate the online game
        if (isWinner){
            net.callHostTerminate();
        }
        else {
            terminate();
        }
    }
    public boolean isContinuable(){
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum()-1; j++) {
                var p1=new ChessboardPoint(i,j);
                var p2=new ChessboardPoint(i,j+1);
                model.swapChessPiece(p1,p2);
                if (isMatchable()){
                    model.swapChessPiece(p1,p2);
                    return true;
                }
                model.swapChessPiece(p1,p2);
            }
        }
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum()-1; i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                var p1=new ChessboardPoint(i,j);
                var p2=new ChessboardPoint(i+1,j);
                model.swapChessPiece(p1,p2);
                if (isMatchable()){
                    model.swapChessPiece(p1,p2);
                    return true;
                }
                model.swapChessPiece(p1,p2);
            }
        }
        return false;
    }
    public void hint(){
        if (!isContinuable()) return;
        if (selectedPoint!=null){
            var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
            point1.setSelected(false);
            point1.repaint();
            selectedPoint=null;
        }
        if (selectedPoint2!=null){
            var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
            point2.setSelected(false);
            point2.repaint();
            selectedPoint2=null;
        }
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum()-1; j++) {
                selectedPoint=new ChessboardPoint(i,j);
                selectedPoint2=new ChessboardPoint(i,j+1);
                model.swapChessPiece(selectedPoint,selectedPoint2);
                if (isMatchable()){
                    model.swapChessPiece(selectedPoint,selectedPoint2);
                    var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                    var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                    point1.setSelected(true);
                    point2.setSelected(true);
                    point1.repaint();
                    point2.repaint();
                    return;
                }
                model.swapChessPiece(selectedPoint,selectedPoint2);
                selectedPoint=null;
                selectedPoint2=null;
            }
        }
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum()-1; i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                selectedPoint=new ChessboardPoint(i,j);
                selectedPoint2=new ChessboardPoint(i+1,j);
                model.swapChessPiece(selectedPoint,selectedPoint2);
                if (isMatchable()){
                    model.swapChessPiece(selectedPoint,selectedPoint2);
                    var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                    var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                    point1.setSelected(true);
                    point2.setSelected(true);
                    point1.repaint();
                    point2.repaint();
                    return;
                }
                model.swapChessPiece(selectedPoint,selectedPoint2);
            }
        }
        JDialog jd = new JDialog(chessGameFrame,"Nothing can be done, please start a new game");
        jd.setVisible(true);
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
