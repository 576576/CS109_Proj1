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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static model.Constant.CHESSBOARD_COL_SIZE;
import static model.Constant.CHESSBOARD_ROW_SIZE;
import static view.ChessGameFrame.isOnlinePlay;
import static view.ChessboardComponent.chessTypes;
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
    public static boolean isNewGameInitialized=false;
    private ChessGameFrame chessGameFrame;

    private boolean isAutoMode=false;

    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;

    private int score, stepLeft;
    public int timeLeft;
    private boolean isAlive=true;
    private int victoryMode=0; // 1=win 2=loss

    enum NextStepFlag {
        NO_SWAP_DONE,        // 0: no swap done, user cannot click "Next Step"
        SWAP_DONE,      // 1: swap done, user can click "Next Step" for the first time to make it fall down
        FALL_DOWN_DONE, // 2: fall down done. Click "Next Step" again to check if match-3 or more continue to take place,
        //    the player can continue eliminating them by clicking the "Next Step" button until there are no more match left.
        //    if no more match left, reset the flag to NO_SWAP_DONE.
    }

    public void resetTimeLeft() {
        timeLeft = difficulty.getTimeLimit();
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    private NextStepFlag onNextStepFlag;
    private final ArrayList<DifficultyPreset> difficultyPresets = new ArrayList<>();
    private JLabel[] statusLabels = new JLabel[4];
    public void setStatusLabels(JLabel[] statusLabels) {
        this.statusLabels = statusLabels;
    }
    public Thread timerThread = new Thread(()->{
        timeLeft=difficulty.getTimeLimit();
        updateTimerLabel();
        System.out.println("Timer Start: "+difficulty.getTimeLimit()+"s");
        for (;;) {
            if (difficulty.getTimeLimit()!=-1){
                for (int i = difficulty.getTimeLimit(); i >=0 ; i--) {
                    if (!isAlive) break;
                    try{
                        Thread.sleep(998);
                    }catch (Exception ignored){}
                    timeLeft--;
                    updateTimerLabel();
                    checkVictory();
                    if (timeLeft%10==0 || timeLeft<=5) {
                        System.out.println("TimeLeft:"+timeLeft);
                    }
                    System.out.print("");
                }
            }
            System.out.print("");
        }
    });
    public GameController(ChessboardComponent view, Chessboard model, NetGame net) {
        initDifficultyPresets();
        this.view = view;
        this.model = model;
        this.net = net;
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
        difficultyPresets.add(DifficultyPreset.EASY);
        difficultyPresets.add(DifficultyPreset.NORMAL);
        difficultyPresets.add(DifficultyPreset.HARD);
        timeLeft=difficulty.getTimeLimit();
        stepLeft=difficulty.getStepLimit();
    }

    // When initialize from the gaming interface, this was used
    public void initialize() {
        isNewGameInitialized=false;
        score = 0;
        timeLeft=difficulty.getTimeLimit();
        victoryMode=0;
        isAlive=true;
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
        isNewGameInitialized=true;

        //complete it when restart game (auto-mode)
        if(isAutoMode){
            doAutoMode();
        }
    }
    public void onPlayerShuffle(){
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
        view.repaint();
        System.out.println("ChessBoard Shuffled");

        //complete it when restart game (auto-mode)
        if(isAutoMode){
            doAutoMode();
        }
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
        checkVictory();
        try {
            // Try to swap, then check if they are matchable
            model.swapChessPiece(selectedPoint, selectedPoint2);
            ChessComponent tmp = view.removeChessComponentAtGrid(selectedPoint);
            view.setChessComponentAtGrid(selectedPoint, view.removeChessComponentAtGrid(selectedPoint2));
            view.setChessComponentAtGrid(selectedPoint2, tmp);
            view.repaint();
            if (!isMatchable()) {
                // Do nothing if there is nothing matchable
                Thread.sleep(500);
                model.swapChessPiece(selectedPoint, selectedPoint2);
                tmp = view.removeChessComponentAtGrid(selectedPoint);
                view.setChessComponentAtGrid(selectedPoint, view.removeChessComponentAtGrid(selectedPoint2));
                view.setChessComponentAtGrid(selectedPoint2, tmp);
                System.out.println("Swap Fail! Nothing can be match");
            } else {
                // Do swap two chess (in view) if matchable
                // TODO: may need animation for swap and eliminate
                // Do the elimination
                this.onNextStepFlag = NextStepFlag.SWAP_DONE; // Swap done, eliminated, so set the state to SWAP_DONE
                //TODO: cancel cell selection after eliminate? something cause null pointer exception
                doChessEliminate();

                //TODO: cancel cell selection after eliminate? it may cause null pointer exception
            }
        } catch (NullPointerException | InterruptedException e) {
            System.out.println("Swap Failed!");
        } finally {
            if (selectedPoint!=null) {
                var point1 = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
                point1.setSelected(false);
                point1.repaint();
                selectedPoint=null;
            }
            if (selectedPoint2!=null) {
                var point2 = (ChessComponent) view.getGridComponentAt(selectedPoint2).getComponent(0);
                point2.setSelected(false);
                point2.repaint();
                selectedPoint2=null;
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
        checkVictory();
        stepLeft--;
    }

    private void checkVictory() {
        if (score >= difficulty.getGoal()) {
            JOptionPane.showMessageDialog(chessGameFrame,"Congratulations! You win.");
            System.out.println("Victory: Reach the goal");
            victoryMode=1;
            SwingUtilities.invokeLater(()-> chessGameFrame.returnToTitle());
            this.terminate();
        }
        if (score<difficulty.getGoal() && stepLeft==0 || timeLeft<=0 && difficulty.getTimeLimit()>0){
            if (stepLeft==0){
                JOptionPane.showMessageDialog(chessGameFrame,"Oh no, no more steps!");
                System.out.println("Loss: Step limit exceeded");
            }
            else if (timeLeft<=0 && difficulty.getTimeLimit()>0){
                JOptionPane.showMessageDialog(chessGameFrame,"Oh no, you DON'T have time!");
                System.out.println("Loss: Time limit exceeded");
            }
            victoryMode=2;
            SwingUtilities.invokeLater(()-> chessGameFrame.returnToTitle());
            this.terminate();
        }
    }

    @Override
    public void onPlayerNextStep() {
//        // For debug only
//        System.out.println("Next step flag: " + this.onNextStepFlag);

        //user should only click this after something has been swapped & eliminated
        if (this.onNextStepFlag == NextStepFlag.NO_SWAP_DONE) {
            JOptionPane.showMessageDialog(chessGameFrame,"Nothing matches, next step failed.");
            System.out.println("NextStep Fail: nothing matches");
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
                while (checkChessBoardHasEmpty()) {
                    doGenerateRandomPiecesOnTop();
                    view.repaint();
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {}
                    doFallDown();
                }
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
        checkVictory();
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
                        view.repaint();

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
                    this.model.getGrid()[i][j].setPiece(new ChessPiece(Util.RandomPick(chessTypes)));
                    this.view.setChessComponentAtGrid(new ChessboardPoint(i, j), new ChessComponent(view.getCHESS_SIZE(),
                            new ChessPiece(model.getGrid()[i][j].getPiece().getName())));
                }
            }
        }
        view.repaint();
    }
    private void doGenerateRandomPiecesOnTop(){
        for (int j = 0; j < CHESSBOARD_COL_SIZE.getNum(); j++) {
            if (this.model.getGrid()[0][j].getPiece() == null) {
                this.model.getGrid()[0][j].setPiece(new ChessPiece(Util.RandomPick(chessTypes)));
                this.view.setChessComponentAtGrid(new ChessboardPoint(0, j), new ChessComponent(view.getCHESS_SIZE(),
                        new ChessPiece(model.getGrid()[0][j].getPiece().getName())));
            }
        }
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
    public void updateTimerLabel(){
        if (difficulty.getTimeLimit()==-1) statusLabels[3].setText("TimeLimit:∞");
        else statusLabels[3].setText("TimeLimit:"+timeLeft);
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
        System.out.println("Difficulty:"+difficulty.getName()+"\nLoaded from File:");
        view.removeAllChessComponentsAtGrids();
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            String str = Arrays.toString(csb[i]);
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("]", "");
            str = str.replaceAll(",", "");
            System.out.println(str);

            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
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
    public void loadFromString(String string){
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
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                csb[i][j] = sc.hasNextInt() ? sc.nextInt() : new Random().nextInt(chessTypes.length);
            }
        }
        System.out.println("Difficulty:"+difficulty.getName()+"\nLoaded from String:");
        System.out.println(score+" "+timeLeft+" "+stepLeft+" "+goal+" "+timeLimit+" "+stepLimit);
        try {
            view.removeAllChessComponentsAtGrids();
        } catch (Exception ignored) {}
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            String str = Arrays.toString(csb[i]);
            str = str.replaceAll("\\[", "");
            str = str.replaceAll("]", "");
            str = str.replaceAll(",", "");
            System.out.println(str);

            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
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
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
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
            System.out.println("at"+file.getAbsolutePath()+file.getName());
        } catch (IOException e) {
            System.err.println("Save Fail: IOException");
        }
    }
    public String ConvertToString(){
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(" ").append(timeLeft).append(" ").append(stepLeft).append(" ").append(difficulty.getGoal())
                .append(" ").append(difficulty.getTimeLimit()).append(" ").append(difficulty.getStepLimit()).append(" \n");
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
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

    public void onlineGameTerminate(boolean isWinner){
        if (isWinner){
            JOptionPane.showMessageDialog(chessGameFrame,"Congratulations! You win.");
            System.out.println("Victory: Your Competitor Loss");
            victoryMode=1;
        }
        else {
            JOptionPane.showMessageDialog(chessGameFrame,"Oh no! Your competitor win.");
            System.out.println("Loss: Your Competitor Win");
            victoryMode=2;
        }
        score=0;
        isAlive=false;
        SwingUtilities.invokeLater(()-> chessGameFrame.returnToTitle());
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
            while (score <= difficulty.getGoal() && isAutoMode && isAlive) {
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
        Runnable runnable = () -> {
            while (onNextStepFlag != NextStepFlag.NO_SWAP_DONE && isAlive) {
                pause1Second();
                onPlayerNextStep();
                view.repaint();
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

    // For auto mode, to avoid it ends immediately
    // a workaround
    private void pause1Second(){
        try {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException ignored){}
    }
    @Override
    public void terminate() {
        score=0;
        if (isOnlinePlay()){
            NetGame.t.interrupt();
        }
        isAlive=false;
    }
    public boolean isAlive(){
        return isAlive;
    }

    public int getVictoryMode() {
        return victoryMode;
    }

    public ChessGameFrame getChessGameFrame() {
        return chessGameFrame;
    }
    public void startTimer(){
        timeLeft=difficulty.getTimeLimit();
        try {
            timerThread.start();
        } catch (Exception ignored) {}
    }
}
