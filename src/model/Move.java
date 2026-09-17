package model;

/** 一次重力下落：棋子从 {@code from} 落到更靠底部的 {@code to}。 */
public record Move(BoardPoint from, BoardPoint to) {
}
