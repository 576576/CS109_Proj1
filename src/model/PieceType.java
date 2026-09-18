package model;

import util.ResourceRoot;

import java.util.random.RandomGenerator;

/**
 * 棋子的全部类目。textureIndex 与存档里的数字编号一一对应，调整顺序会导致旧存档错配。
 * 颜色只存 RGB：模型不依赖任何界面工具包，谁要画谁再去转成自己的颜色类型。
 */
public enum PieceType {
    DIAMOND(0, 0x2196F3, "\uD83D\uDC8E"),
    ORB(1, 0xF5F5F5, "\u26AA"),
    TRIANGLE(2, 0x4CAF50, "\u25B2"),
    HEXAGON(3, 0xFF9800, "\uD83D\uDD36"),
    SMILE(4, 0xFFEB3B, "\uD83D\uDE42"),
    EYE(5, 0xE91E63, "\uD83D\uDC40");

    private static final PieceType[] VALUES = values();

    private final int textureIndex;
    private final int rgb;
    private final String glyph;

    PieceType(int textureIndex, int rgb, String glyph) {
        this.textureIndex = textureIndex;
        this.rgb = rgb;
        this.glyph = glyph;
    }

    public int textureIndex() {
        return textureIndex;
    }

    public int rgb() {
        return rgb;
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
