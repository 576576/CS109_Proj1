package view;


import controller.GameController;
import model.Cell;
import model.ChessPiece;
import model.Chessboard;
import model.ChessboardPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import static model.Constant.DEFAULT_CHESSBOARD_COL_SIZE;
import static model.Constant.DEFAULT_CHESSBOARD_ROW_SIZE;
import static view.MenuFrame.isDarkMode;

/**
 * This class represents the checkerboard component object on the panel
 */
public class ChessboardComponent extends JComponent {
    private final CellComponent[][] gridComponents = new CellComponent[DEFAULT_CHESSBOARD_ROW_SIZE.getNum()][DEFAULT_CHESSBOARD_COL_SIZE.getNum()];
    private final int CHESS_SIZE;
    private final Set<ChessboardPoint> riverCell = new HashSet<>();

    public int getCHESS_SIZE() {
        return CHESS_SIZE;
    }

    private GameController gameController;

    public ChessboardComponent(int chessSize) {
        CHESS_SIZE = chessSize;
        int width = CHESS_SIZE * 8;
        int height = CHESS_SIZE * 8;
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);// Allow mouse events to occur
        setLayout(null); // Use absolute layout.
        setSize(width, height);
        System.out.printf("Chessboard: size=(%d,%d), chess size=%d\n", width, height, CHESS_SIZE);

        initiateGridComponents();
    }


    /**
     * This method represents how to initiate ChessComponent
     * according to Chessboard information
     */
    public void initiateChessComponent(Chessboard chessboard) {
        Cell[][] grid = chessboard.getGrid();
        for (int i = 0; i < DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                // TODO: Implement the initialization checkerboard
                riverCell.add(new ChessboardPoint(i,j));
                if (grid[i][j].getPiece() != null) {
                    ChessPiece chessPiece = grid[i][j].getPiece();
                    gridComponents[i][j].add(new ChessComponent(CHESS_SIZE, chessPiece));
                }
            }
        }

    }

    public void initiateGridComponents() {
        for (int i = 0; i < DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                ChessboardPoint temp = new ChessboardPoint(i, j);
                CellComponent cell;
                if (riverCell.contains(temp)) {
                    cell = new CellComponent(Color.CYAN, calculatePoint(i, j), CHESS_SIZE);
                    this.add(cell);
                } else {
                    cell = new CellComponent(Color.LIGHT_GRAY, calculatePoint(i, j), CHESS_SIZE);
                    this.add(cell);
                }
                gridComponents[i][j] = cell;
            }
        }
    }

    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }

    public void setChessComponentAtGrid(ChessboardPoint point, ChessComponent chess) {
        getGridComponentAt(point).add(chess);
    }

    public void removeAllChessComponentsAtGrids(){
        for (var i:riverCell) removeChessComponentAtGrid(i);
    }

    public ChessComponent removeChessComponentAtGrid(ChessboardPoint point) {
        // Note re-validation is required after remove / removeAll.
        ChessComponent chess = null;
        try {
            chess = (ChessComponent) getGridComponentAt(point).getComponents()[0];
            chess.setSelected(false);
        } catch (ClassCastException ignored) {}
        getGridComponentAt(point).removeAll();
        getGridComponentAt(point).revalidate();
        return chess;
    }

    public CellComponent getGridComponentAt(ChessboardPoint point) {
        return gridComponents[point.row()][point.col()];
    }

    private ChessboardPoint getChessboardPoint(Point point) {
        System.out.println("[" + point.y/CHESS_SIZE +  ", " +point.x/CHESS_SIZE + "] Clicked");
        return new ChessboardPoint(point.y/CHESS_SIZE, point.x/CHESS_SIZE);
    }
    private Point calculatePoint(int row, int col) {
        return new Point(col * CHESS_SIZE, row * CHESS_SIZE);
    }

    public void swapChess(){
        gameController.onPlayerSwapChess();
    }
    public void startNewGame(){gameController.initialize();}

    public void nextStep(){
        gameController.nextStep();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            JComponent clickedComponent = (JComponent) getComponentAt(e.getX(), e.getY());
            if (clickedComponent.getComponentCount() == 0) {
                System.out.print("None chess here and ");
                gameController.onPlayerClickCell(getChessboardPoint(e.getPoint()), (CellComponent) clickedComponent);
            } else {
                System.out.print("One chess here and ");
                gameController.onPlayerClickChessPiece(getChessboardPoint(e.getPoint()), (ChessComponent) clickedComponent.getComponents()[0]);
            }
        }
    }

    public void setDarkMode() {
        for (var i:gridComponents) for (var j : i) j.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(),getHeight());
    }
    public void repaint(){
        super.repaint();
    }
}
