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
import java.util.concurrent.TimeUnit;

import static model.Constant.CHESSBOARD_COL_SIZE;
import static model.Constant.CHESSBOARD_ROW_SIZE;
import static view.MenuFrame.difficulty;
/**
 * Controller is the connection between model and view,
 * when a Controller receive a request from a view, the Controller
 * analyzes and then hands over to the model for processing
 * [in this demo the request methods are onPlayerClickCell() and
 * onPlayerClickChessPiece()]
 */
public class GameController implements GameListener{

    private final Chessboard model;
    private final ChessboardComponent view;
    private final NetGame net;
    public boolean isAutoConfirm=false;
    private ChessGameFrame chessGameFrame;

    private boolean isAutoMode=false;

    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;

    private int score, timeLeft, stepLeft;

    enum NextStepFlag {
        NO_SWAP_DONE,        // 0: no swap done, user cannot click "Next Step"
        SWAP_DONE,      // 1: swap done, user can click "Next Step" for the first time to make it fall down
        FALL_DOWN_DONE, // 2: fall down done. Click "Next Step" again to check if match-3 or more continue to take place,
        //    the player can continue eliminating them by clicking the "Next Step" button until there are no more match left.
        //    if no more match left, reset the flag to NO_SWAP_DONE.
    }

    private NextStepFlag onNextStepFlag;
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
        setDifficulty(difficulty);
        this.onNextStepFlag = NextStepFlag.NO_SWAP_DONE; // first, no swap done so initiate with this state
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
        this.onNextStepFlag = NextStepFlag.NO_SWAP_DONE;  // first, no swap done, so initiate it with this state

        // call method to refresh a chessboard with new random pieces
        this.model.initPieces();
        // fetch contents of Chessboard to view
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                view.setChessComponentAtGrid(new ChessboardPoint(i, j), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
            }
        }

        updateDifficultyLabel();
        updateScoreAndStepLabel();
        view.repaint();
        System.out.println("New game initialized");

        //complete it when restart game (auto-mode)
        if(isAutoMode){
            doAutoMode();
        }
    }

    public void setDifficulty(Difficulty difficulty1) {
        difficulty = difficulty1;
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
        if ( stepLeft<0 && !difficulty.getName().equals("EASY")){
            // TODO: notice player no step. using a window
            System.out.println("No step left!");
            initialize();
        }

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
                this.onNextStepFlag = NextStepFlag.SWAP_DONE; // Swap done, eliminated, so set the state to SWAP_DONE
                //TODO: cancel cell selection after eliminate? something cause null pointer exception
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
            updateScoreAndStepLabel();
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

        // Check for horizontal adjacency
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
                    // Do elimination for matched chess pieces
                    for (int m = j; m < j + matchCount; m++) {
                        model.removeChessPiece(new ChessboardPoint(i, m));
                        view.removeChessComponentAtGrid(new ChessboardPoint(i, m));
                    }
                    score += matchCount ;
                }

                j = k;
            }
        }

        // Check for vertical adjacency
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
                    // Do elimination for matched chess pieces
                    for (int m = i; m < i + matchCount; m++) {
                        model.removeChessPiece(new ChessboardPoint(m, j));
                        view.removeChessComponentAtGrid(new ChessboardPoint(m, j));
                    }
                    score += matchCount ;
                }

                i = k;
            }
        }
        view.repaint();
        updateScoreAndStepLabel();
        if (score > difficulty.getGoal()){
            // TODO: send UI message to notice the user got victory
            System.out.println("You have passed the target score!");
            initialize();
        }
        stepLeft--;
    }

    @Override
    public void onPlayerNextStep() {
        // For debug only
        System.out.println("Next step flag: " + this.onNextStepFlag);

        //user should only click this after something has been swapped & eliminated
        if (this.onNextStepFlag == NextStepFlag.NO_SWAP_DONE) {
            System.out.println("Should not click Next Step when nothing swapped & eliminated!");

        }

        if (this.onNextStepFlag == NextStepFlag.SWAP_DONE) {
            doFallDown();
            this.onNextStepFlag = NextStepFlag.FALL_DOWN_DONE;
            doChessEliminate();
            // Fall done has done, if there is any match-3, eliminate them
            System.out.println("Bonus! Match occurs when pieces fall down.");
            view.repaint();
            return;
        }

        if (this.onNextStepFlag == NextStepFlag.FALL_DOWN_DONE) {
            if (checkChessBoardHasEmpty()) {
                // generate new pieces to fill empty cells
                // if there are new match-3 take place, user could click next step again to eliminate
                doGenerateRandomPiecesEmptyCell();
                view.repaint();
                // in case any new match take place after generated
                while(isMatchable()){
                    doChessEliminate();
                    doGenerateRandomPiecesEmptyCell();
                    view.repaint();
                    System.out.println("New match occurs after generated new pieces!");
                }
            } else {
                // if no empty and nothing to eliminate, back to normal gaming
                this.onNextStepFlag = NextStepFlag.NO_SWAP_DONE;
            }
        }

        updateScoreAndStepLabel();
        System.out.println("Score updated:" + score);
        if (score > difficulty.getGoal()){
            // TODO: send UI message to notice the user got victory
            System.out.println("You have passed the target score!");
            initialize();
        }
    }
    // pieces above those empty cells will fail down until the empty cells are occupied
    // TODO: animation here
    private void doFallDown() {
        // check every column for empty cells
        boolean hasEmptyCell = true;
        while (hasEmptyCell) {
            hasEmptyCell = false;
            for (int col = 0; col < CHESSBOARD_COL_SIZE.getNum(); col++) {
                for (int row = CHESSBOARD_ROW_SIZE.getNum() - 1; row > 0; row--) {
                    if ( this.model.getGrid()[row][col].getPiece() == null && ! (this.model.getGrid()[row - 1][col].getPiece() == null)) {
                        // should do swap for model and view

                        ChessboardPoint upperCell = new ChessboardPoint(row-1,col);
                        ChessboardPoint lowerCell = new ChessboardPoint(row,col);

                        model.swapChessPiece(upperCell, lowerCell);
                        view.setChessComponentAtGrid(lowerCell, view.removeChessComponentAtGrid(upperCell));

                        hasEmptyCell = true;
                    }
                }
            }
        }
    }

    private void doGenerateRandomPiecesEmptyCell() {
        for (int i = 0; i < CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < CHESSBOARD_COL_SIZE.getNum(); j++) {
                if (this.model.getGrid()[i][j].getPiece() == null) {
                    this.model.getGrid()[i][j].setPiece(new ChessPiece(Util.RandomPick(new String[]{"💎", "⚪", "▲", "🔶", "🌞", "🪐"})));
                    this.view.setChessComponentAtGrid(new ChessboardPoint(i, j), new ChessComponent(view.getCHESS_SIZE(),
                            new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
                }
            }
        }
        view.repaint();
    }

    // Check if there is any empty cell on chessboard
    private boolean checkChessBoardHasEmpty() {
        for (int i = 0; i < CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < CHESSBOARD_COL_SIZE.getNum(); j++) {
                if (this.model.getGrid()[i][j].getPiece() == null) {
                    return true;
                }
            }
        }
        return false;
    }
    public void updateScoreAndStepLabel(){
        if (statusLabels[0]==null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[1].setText("Score:" + score + "/" + difficulty.getGoal());
        statusLabels[2].setText("StepLeft:" +((difficulty.getStepLimit()>0)?(stepLeft + "/"+difficulty.getStepLimit()):('∞')));
    }
    public void updateDifficultyLabel(){
        if (statusLabels[0]==null) setStatusLabels(chessGameFrame.getStatusLabels());
        statusLabels[0].setText("Difficulty:" + difficulty.getName());
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
                // TODO: is there a bug? ChessboardPoint(j, i) or ChessboardPoint(i, j)?
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
                if (isAutoConfirm()&& !isAutoConfirm()){
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
                if (isAutoConfirm()&& !isAutoMode()){
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
            if (isAutoConfirm()&&!isAutoMode()){
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

    public String getNetGameData() {
        return difficulty.getDifficultyInfo() + " " + score;
    }

    @Override
    public void terminate() {
        //TODO:terminate the game
    }
    public void onlineGameTerminate(boolean isWinner){
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
        System.out.println("autoConfirm "+autoConfirm);
    }

    // TODO: toggle auto mode from UI interaction
    public void setAutoMode(boolean autoMode){
        isAutoMode=autoMode;
        if (isAutoMode()){
            doAutoMode();
        }
    }


    // Implement auto-mode
    private void doAutoMode() {
        // Create a new thread to run the auto mode logic.
        new Thread(() -> {
            while (score <= difficulty.getGoal() && isAutoMode) {
                hint();
                onPlayerSwapChess();
                pause1Second();

                // Wait until the player has made their next move.
                while (this.onNextStepFlag != NextStepFlag.NO_SWAP_DONE) {
                    pause1Second();
                    onPlayerNextStep();
                }
            }
        }).start();
    }

    // To handle auto confirm when it is on
    private void doAutoConfirm() {
        onPlayerSwapChess();
        view.repaint();
        pause1Second();
        // runner for new thread, to avoid UI freeze
        Runnable runnable = new Runnable() {
            public void run() {
                while (onNextStepFlag != NextStepFlag.NO_SWAP_DONE) {
                    pause1Second();
                    onPlayerNextStep();
                    view.repaint();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public boolean isAutoConfirm() {
        return isAutoConfirm;
    }

    public boolean isAutoMode(){
        return isAutoMode;
    }
    public Difficulty getDifficulty() {
        return difficulty;
    }

    // For auto mode, to avoid it ends immediately
    // a workaround
    private void pause1Second(){
        try {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e){}
    }
}
