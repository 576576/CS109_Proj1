package ui;

import model.PieceType;

/**
 * 棋盘上一枚棋子的绘制状态。它不是 JavaFX 节点：整块棋盘画在一张 Canvas 上，
 * 棋子只提供"画什么、选没选中"，真正落笔的是 BoardView。
 */
public final class TileView {

    private final PieceType type;
    private boolean selected;
    private BoardView owner;

    public TileView(int size, PieceType type) {
        this.type = type;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /** 放进棋盘时登记归属，棋子自己才知道该让谁重画。 */
    void attach(BoardView board) {
        this.owner = board;
    }

    void detach() {
        this.owner = null;
        this.selected = false;
    }

    public void repaint() {
        if (owner != null) owner.repaint();
    }
}
