package model;

import java.util.ArrayList;
import java.util.List;

/**
 * 存放棋局真实状态的棋盘。格子里存放 {@link PieceType}，{@code null} 表示空位。
 */
public final class Board {
    public static final int DEFAULT_SIZE = 8;

    private final PieceBag bag;
    private final int rows;
    private final int cols;
    private final PieceType[][] grid;

    public Board() {
        this(new PieceBag());
    }

    public Board(PieceBag bag) {
        this(bag, DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public Board(PieceBag bag, int rows, int cols) {
        this.bag = bag;
        this.rows = rows;
        this.cols = cols;
        this.grid = new PieceType[rows][cols];
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public boolean contains(BoardPoint point) {
        return point.row() >= 0 && point.row() < rows && point.col() >= 0 && point.col() < cols;
    }

    /** 返回指定格的棋子类目，空位返回 {@code null}。 */
    public PieceType pieceAt(BoardPoint point) {
        return grid[point.row()][point.col()];
    }

    public void setPieceAt(BoardPoint point, PieceType type) {
        grid[point.row()][point.col()] = type;
    }

    /** 清空指定格并返回原来的类目。 */
    public PieceType removePieceAt(BoardPoint point) {
        PieceType removed = pieceAt(point);
        setPieceAt(point, null);
        return removed;
    }

    public void swapPieces(BoardPoint first, BoardPoint second) {
        PieceType swapIn = pieceAt(first);
        setPieceAt(first, pieceAt(second));
        setPieceAt(second, swapIn);
    }

    public List<BoardPoint> points() {
        List<BoardPoint> all = new ArrayList<>(rows * cols);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) all.add(new BoardPoint(row, col));
        }
        return List.copyOf(all);
    }

    public boolean hasEmptyCells() {
        return points().stream().anyMatch(point -> pieceAt(point) == null);
    }

    /** 横向与纵向全部长度不小于 3 的同类连线，重叠的格子只算一次。 */
    public List<BoardPoint> listMatches() {
        List<BoardPoint> matched = new ArrayList<>();
        collectStraightRuns(matched, true);
        collectStraightRuns(matched, false);
        return List.copyOf(matched);
    }

    public BoardSnapshot snapshot() {
        List<PieceType> types = new ArrayList<>(rows * cols);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) types.add(grid[row][col]);
        }
        return new BoardSnapshot(rows, cols, types);
    }

    /** 重新铺满棋盘，直到开局没有现成的三连。菜单开局与洗牌按钮共用。 */
    public void initPieces() {
        do {
            for (BoardPoint point : points()) setPieceAt(point, bag.pick());
        } while (!listMatches().isEmpty());
    }

    private void collectStraightRuns(List<BoardPoint> matched, boolean scanRows) {
        int lineCount = scanRows ? rows : cols;
        int lineLength = scanRows ? cols : rows;
        for (int line = 0; line < lineCount; line++) {
            int start = 0;
            while (start < lineLength) {
                PieceType type = pieceAt(pointIn(scanRows, line, start));
                int end = start + 1;
                while (end < lineLength && pieceAt(pointIn(scanRows, line, end)) == type) end++;
                if (type != null && end - start >= 3) {
                    for (int index = start; index < end; index++) {
                        BoardPoint point = pointIn(scanRows, line, index);
                        if (!matched.contains(point)) matched.add(point);
                    }
                }
                start = end;
            }
        }
    }

    private BoardPoint pointIn(boolean scanRows, int line, int index) {
        return scanRows ? new BoardPoint(line, index) : new BoardPoint(index, line);
    }
}
