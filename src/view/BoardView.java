package view;

import controller.GameController;
import model.Board;
import model.BoardPoint;
import model.BoardSnapshot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

import static view.MenuFrame.isDarkMode;

/**
 * This class represents the checkerboard component object on the panel
 */
public class BoardView extends JComponent {
    private final CellComponent[][] gridComponents;
    private final int chessSize;
    private final int rows;
    private final int cols;

    private GameController gameController;

    public BoardView(int chessSize, int rows, int cols) {
        this.chessSize = chessSize;
        this.rows = rows;
        this.cols = cols;
        this.gridComponents = new CellComponent[rows][cols];
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);// Allow mouse events to occur
        setLayout(null); // Use absolute layout.
        setSize(chessSize * cols, chessSize * rows);
        System.out.printf("Board: size=(%d,%d), chess size=%d%n", getWidth(), getHeight(), chessSize);

        initiateGridComponents();
    }

    public int getCHESS_SIZE() {
        return chessSize;
    }

    /** Puts a TileView into every cell that currently holds a piece. */
    public void initiateTileViews(Board board) {
        BoardSnapshot snapshot = board.snapshot();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                var type = snapshot.typeAt(new BoardPoint(row, col));
                if (type != null) gridComponents[row][col].add(new TileView(chessSize, type));
            }
        }
    }

    public void initiateGridComponents() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                gridComponents[row][col] = new CellComponent(Color.LIGHT_GRAY, calculatePoint(row, col), chessSize);
                this.add(gridComponents[row][col]);
            }
        }
    }

    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }

    public void setTileAt(BoardPoint point, TileView tile) {
        getGridComponentAt(point).add(tile);
    }

    public void removeAllTiles() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) removeTileAt(new BoardPoint(row, col));
        }
    }

    public TileView removeTileAt(BoardPoint point) {
        // Note re-validation is required after remove / removeAll.
        TileView tile = null;
        var components = getGridComponentAt(point).getComponents();
        if (components.length != 0 && components[0] instanceof TileView found) {
            tile = found;
            tile.setSelected(false);
        }
        getGridComponentAt(point).removeAll();
        getGridComponentAt(point).revalidate();
        return tile;
    }

    public CellComponent getGridComponentAt(BoardPoint point) {
        return gridComponents[point.row()][point.col()];
    }

    private BoardPoint getBoardPoint(Point point) {
        System.out.println("[" + point.y / chessSize + ", " + point.x / chessSize + "] Clicked");
        return new BoardPoint(point.y / chessSize, point.x / chessSize);
    }

    private Point calculatePoint(int row, int col) {
        return new Point(col * chessSize, row * chessSize);
    }

    public void swapChess() {
        gameController.onPlayerSwapChess();
    }

    public void startNewGame() {
        gameController.initialize();
    }

    public void nextStep() {
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
                gameController.onPlayerClickCell(getBoardPoint(e.getPoint()), (CellComponent) clickedComponent);
            } else {
                System.out.print("One chess here and ");
                gameController.onPlayerClickPiece(getBoardPoint(e.getPoint()), (TileView) clickedComponent.getComponents()[0]);
            }
        }
    }

    public void setDarkMode() {
        for (var row : gridComponents) {
            for (var cell : row) cell.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidth(), getHeight());
    }
}
