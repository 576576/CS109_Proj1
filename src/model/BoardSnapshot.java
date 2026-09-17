package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 棋盘的不可变快照，先行后列平铺；类目为 {@code null} 表示该格是空位。 */
public record BoardSnapshot(int rows, int cols, List<PieceType> types) {

    public BoardSnapshot {
        types = Collections.unmodifiableList(new ArrayList<>(types));
    }

    public PieceType typeAt(BoardPoint point) {
        return types.get(point.row() * cols + point.col());
    }
}
