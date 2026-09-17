package model;

import java.util.ArrayList;
import java.util.List;

/**
 * 棋盘上的坐标。左上角为 (0, 0)，row 向下增长、col 向右增长。
 */
public record BoardPoint(int row, int col) {

    public int distanceTo(BoardPoint other) {
        return Math.abs(row - other.row()) + Math.abs(col - other.col());
    }

    public boolean isAdjacentTo(BoardPoint other) {
        return distanceTo(other) == 1;
    }

    /** 四邻域内位于棋盘内的坐标，棋盘外的方向会被裁掉。 */
    public List<BoardPoint> neighbors(int rows, int cols) {
        List<BoardPoint> found = new ArrayList<>(4);
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            int otherRow = row + offset[0];
            int otherCol = col + offset[1];
            if (otherRow >= 0 && otherRow < rows && otherCol >= 0 && otherCol < cols) {
                found.add(new BoardPoint(otherRow, otherCol));
            }
        }
        return List.copyOf(found);
    }

    @Override
    public String toString() {
        return "(%d,%d)".formatted(row, col);
    }
}
