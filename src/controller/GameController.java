package controller;

import listener.GameListener;
import model.*;
import view.CellComponent;
import view.ChessComponent;
import view.ChessboardComponent;

import javax.swing.*;
import java.io.*;
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


    // Record whether there is a selected piece before
    private ChessboardPoint selectedPoint;
    private ChessboardPoint selectedPoint2;

    private int score;

    private JLabel statusLabel;

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
    }

    public GameController(ChessboardComponent view, Chessboard model) {
        this.view = view;
        this.model = model;

        view.registerController(this);
        view.initiateChessComponent(model);
        view.repaint();
    }

    public void initialize() {
        score=0;
        statusLabel.setText("Score:"+score);
        view.removeAllChessComponentsAtGrids();
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                //todo: complete it when restart game
                view.setChessComponentAtGrid(new ChessboardPoint(j,i),new ChessComponent(view.getCHESS_SIZE(),new ChessPiece(Util.RandomPick(new String[]{"💎", "⚪", "▲", "🔶"}))));
                model.setChessPiece(new ChessboardPoint(j,i),new ChessPiece(Util.RandomPick(new String[]{"💎", "⚪", "▲", "🔶"})));
            }
        }
        view.repaint();
    }

    // click an empty cell
    @Override
    public void onPlayerClickCell(ChessboardPoint point, CellComponent component) {
    }

    @Override
    public void onPlayerSwapChess() {
        try {
            model.swapChessPiece(selectedPoint,selectedPoint2);
            ChessComponent tmp=view.removeChessComponentAtGrid(selectedPoint);
            view.setChessComponentAtGrid(selectedPoint,view.removeChessComponentAtGrid(selectedPoint2));
            view.setChessComponentAtGrid(selectedPoint2,tmp);
        } catch (NullPointerException e) {
            System.out.println("Swap Failed!");
            selectedPoint=null;
            selectedPoint2=null;
        } finally {
            view.repaint();
        }
    }

    @Override
    public void onPlayerNextStep() {

        score++;
        this.statusLabel.setText("Score:" + score);
        System.out.println("Score updated:"+score);
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
        score=sc.nextInt();
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                csb[i][j]=sc.hasNextInt()?sc.nextInt():new Random().nextInt(4);
            }
        }
        System.out.println("Game Loaded.\nScore: "+score+"\n");
        view.removeAllChessComponentsAtGrids();
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            System.out.println(Arrays.toString(csb[i]));
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                String pName = new String[]{"💎", "⚪", "▲", "🔶"}[Math.min(Math.max(csb[j][i],0),3)];
                view.setChessComponentAtGrid(new ChessboardPoint(j,i),new ChessComponent(view.getCHESS_SIZE(),new ChessPiece(pName)));
                model.setChessPiece(new ChessboardPoint(j,i),new ChessPiece(pName));
            }
        }
        view.repaint();
        sc.close();
    }

    @Override
    public void saveToFile(File file) {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append("\n");
        for (int i = 0; i < Constant.CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.CHESSBOARD_COL_SIZE.getNum(); j++) {
                var cp = model.getChessPieceAt(new ChessboardPoint(i,j)).getName();
                switch (cp){
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
        System.out.println(sb);
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
            var distance2point1 = Math.abs(selectedPoint.getCol() - point.getCol()) + Math.abs(selectedPoint.getRow() - point.getRow());
            var distance2point2 = Math.abs(selectedPoint2.getCol() - point.getCol()) + Math.abs(selectedPoint2.getRow() - point.getRow());
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
            }
            return;
        }


        if (selectedPoint == null) {
            selectedPoint = point;
            component.setSelected(true);
            component.repaint();
            return;
        }

        var distance2point1 = Math.abs(selectedPoint.getCol() - point.getCol()) + Math.abs(selectedPoint.getRow() - point.getRow());

        if (distance2point1 == 0) {
            selectedPoint = null;
            component.setSelected(false);
            component.repaint();
            return;
        }

        if (distance2point1 == 1) {
            selectedPoint2 = point;
        } else {
            selectedPoint2 = null;

            var grid = (ChessComponent) view.getGridComponentAt(selectedPoint).getComponent(0);
            if (grid == null) return;
            grid.setSelected(false);
            grid.repaint();

            selectedPoint = point;
        }
        component.setSelected(true);
        component.repaint();

    }

}
