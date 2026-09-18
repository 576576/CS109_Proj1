package ui;

import controller.GameController;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.Board;
import model.BoardPoint;
import model.BoardSnapshot;
import model.PieceType;

import java.util.EnumMap;
import java.util.Map;

import util.Log;
import util.ResourceRoot;

/**
 * 棋盘。整块画在一张 Canvas 上：格子、棋子、选中框都是一次绘制的产物，
 * 这样下坠动画每动一枚棋子重画一次就够了。
 */
public class BoardView extends Pane {

    private static final int TILE_INSET = 4;
    private static final int CELL_INSET = 2;
    private static final int CORNER = 14;

    private final int chessSize;
    private final int rows;
    private final int cols;
    private final Canvas canvas;
    private final TileView[][] tiles;
    private final Map<PieceType, Image> textures = new EnumMap<>(PieceType.class);

    private Theme theme;
    private GameController controller;

    public BoardView(int chessSize, int rows, int cols) {
        this.chessSize = chessSize;
        this.rows = rows;
        this.cols = cols;
        this.tiles = new TileView[rows][cols];
        this.canvas = new Canvas(chessSize * cols, chessSize * rows);
        getChildren().add(canvas);
        setPrefSize(chessSize * cols, chessSize * rows);
        setOnMouseClicked(this::onMouseClicked);
    }

    public int getCHESS_SIZE() {
        return chessSize;
    }

    public void registerController(GameController controller) {
        this.controller = controller;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        repaint();
    }

    /** Puts a TileView into every cell that currently holds a piece. */
    public void initiateTileViews(Board board) {
        BoardSnapshot snapshot = board.snapshot();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                var type = snapshot.typeAt(new BoardPoint(row, col));
                if (type != null) setTileAt(new BoardPoint(row, col), new TileView(chessSize, type));
            }
        }
    }

    public void setTileAt(BoardPoint point, TileView tile) {
        TileView previous = tiles[point.row()][point.col()];
        if (previous != null) previous.detach();
        tiles[point.row()][point.col()] = tile;
        if (tile != null) tile.attach(this);
    }

    public TileView removeTileAt(BoardPoint point) {
        TileView tile = tiles[point.row()][point.col()];
        tiles[point.row()][point.col()] = null;
        if (tile != null) tile.detach();
        return tile;
    }

    public void removeAllTiles() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) removeTileAt(new BoardPoint(row, col));
        }
    }

    /** 这一格上的棋子；空格返回 null。 */
    public TileView tileAt(BoardPoint point) {
        if (point == null) return null;
        return tiles[point.row()][point.col()];
    }

    public void repaint() {
        if (Platform.isFxApplicationThread()) draw();
        else Platform.runLater(this::draw);
    }

    private void onMouseClicked(MouseEvent event) {
        if (controller == null) return;
        int col = (int) (event.getX() / chessSize);
        int row = (int) (event.getY() / chessSize);
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        BoardPoint point = new BoardPoint(row, col);
        TileView tile = tileAt(point);
        if (tile == null) controller.onPlayerClickCell(point);
        else controller.onPlayerClickPiece(point, tile);
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                drawCell(g, row, col);
                drawTile(g, row, col);
            }
        }
    }

    private void drawCell(GraphicsContext g, int row, int col) {
        double size = chessSize - CELL_INSET * 2.0;
        double x = col * chessSize + CELL_INSET;
        double y = row * chessSize + CELL_INSET;
        g.setFill(cellColor(row, col));
        g.fillRoundRect(x, y, size, size, CORNER, CORNER);
    }

    /** 棋盘格做成深浅交替，不然整片同色时棋子边界看不出来。 */
    private Color cellColor(int row, int col) {
        if (theme == null) return (row + col) % 2 == 0 ? Color.web("#2b2b33") : Color.web("#33333d");
        return (row + col) % 2 == 0 ? theme.surfaceVariant() : theme.secondaryContainer();
    }

    private void drawTile(GraphicsContext g, int row, int col) {
        TileView tile = tiles[row][col];
        if (tile == null || tile.getType() == null) return;
        double size = chessSize - TILE_INSET * 2.0;
        double x = col * chessSize + TILE_INSET;
        double y = row * chessSize + TILE_INSET;

        Image texture = textureOf(tile.getType());
        if (texture != null) {
            g.drawImage(texture, x, y, size, size);
        } else {
            g.setFill(Color.rgb(tile.getType().rgb() >> 16, tile.getType().rgb() >> 8 & 0xFF,
                    tile.getType().rgb() & 0xFF));
            g.fillOval(x + size * 0.12, y + size * 0.12, size * 0.76, size * 0.76);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(size * 0.42));
            g.fillText(tile.getType().glyph(), x + size * 0.34, y + size * 0.66);
        }

        if (tile.isSelected()) {
            g.setStroke(theme == null ? Color.YELLOW : theme.primary());
            g.setLineWidth(3);
            g.strokeRoundRect(x, y, size, size, CORNER, CORNER);
        }
    }

    private Image textureOf(PieceType type) {
        Image cached = textures.get(type);
        if (cached != null) return cached;
        try {
            Image loaded = new Image(ResourceRoot.path(type.texturePath()).toUri().toString());
            if (loaded.isError()) return null;
            textures.put(type, loaded);
            return loaded;
        } catch (RuntimeException e) {
            Log.warn("Cannot read texture " + type.texturePath() + ": " + e);
            return null;
        }
    }
}
