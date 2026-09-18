package model;

import util.ResourceRoot;

import java.awt.Color;
import java.util.random.RandomGenerator;

/**
 * 棋子的全部类目。textureIndex 与存档里的数字编号一一对应，调整顺序会导致旧存档错配。
 */
public enum PieceType {
    DIAMOND(0, Color.BLUE, "\uD83D\uDC8E"),
    ORB(1, Color.WHITE, "\u26AA"),
    TRIANGLE(2, Color.GREEN, "\u25B2"),
    HEXAGON(3, Color.ORANGE, "\uD83D\uDD36"),
    SMILE(4, Color.YELLOW, "\uD83D\uDE42"),
    EYE(5, Color.MAGENTA, "\uD83D\uDC40");

    private static final PieceType[] VALUES = values();

    private final int textureIndex;
    private final Color color;
    private final String glyph;

    PieceType(int textureIndex, Color color, String glyph) {
        this.textureIndex = textureIndex;
        this.color = color;
        this.glyph = glyph;
    }

    public int textureIndex() {
        return textureIndex;
    }

    public Color color() {
        return color;
    }

    /** 贴图缺失时的回退字形。 */
    public String glyph() {
        return glyph;
    }

    public String texturePath() {
        return ResourceRoot.pathText("texture/chess/" + textureIndex + ".png");
    }

    public static PieceType ofIndex(int index) {
        return VALUES[index];
    }

    public static PieceType random(RandomGenerator rng) {
        return VALUES[rng.nextInt(VALUES.length)];
    }
}
