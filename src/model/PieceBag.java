package model;

import java.util.random.RandomGenerator;

/** 棋子类目的抽签器。随机源由外部注入，因此同一颗种子可以复现同一局棋。 */
public final class PieceBag {
    private final RandomGenerator rng;

    public PieceBag() {
        this(RandomGenerator.getDefault());
    }

    public PieceBag(RandomGenerator rng) {
        this.rng = rng;
    }

    public PieceType pick() {
        return PieceType.random(rng);
    }
}
