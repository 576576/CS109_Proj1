package model;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public enum Constant {
    DEFAULT_CHESSBOARD_ROW_SIZE(8), DEFAULT_CHESSBOARD_COL_SIZE(8);

    private final int num;
    Constant(int num){
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    static final Map<String, Color> colorMap = new HashMap<>(){{
        put("💎",Color.blue); //chess types:6
        put("⚪",Color.white);
        put("▲",Color.green);
        put("🔶",Color.orange);
        put("🙂",Color.yellow);
        put("👀",Color.magenta);
    }};

}
