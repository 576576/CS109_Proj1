package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * 存档文本与 {@link GameState} 之间的唯一转换口。
 * 第二版在首行写了版本号与难度名；读不到版本头的旧档按第一版解析。
 */
public final class GameStateCodec {
    private static final String MAGIC = "M3SAVE";
    private static final int VERSION = 2;
    private static final int EMPTY_CELL = -1;

    private GameStateCodec() {
    }

    public static String toText(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(MAGIC).append(' ').append(VERSION).append('\n');
        Difficulty difficulty = state.difficulty();
        sb.append(state.score()).append(' ').append(state.timeLeft()).append(' ').append(state.stepLeft())
                .append(' ').append(difficulty.name())
                .append(' ').append(difficulty.goal())
                .append(' ').append(difficulty.stepLimit())
                .append(' ').append(difficulty.timeLimit()).append('\n');
        BoardSnapshot board = state.board();
        for (int row = 0; row < board.rows(); row++) {
            for (int col = 0; col < board.cols(); col++) {
                PieceType type = board.typeAt(new BoardPoint(row, col));
                sb.append(type == null ? EMPTY_CELL : type.textureIndex()).append(' ');
            }
            sb.setLength(sb.length() - 1);
            sb.append('\n');
        }
        return sb.toString();
    }

    public static Optional<GameState> fromText(String text, int rows, int cols) {
        Scanner sc = new Scanner(text);
        boolean versioned = sc.hasNext(MAGIC);
        if (versioned) {
            sc.next();
            if (!sc.hasNextInt() || sc.nextInt() != VERSION) return Optional.empty();
        }
        int score = sc.nextInt();
        int timeLeft = sc.nextInt();
        int stepLeft = sc.nextInt();
        Difficulty difficulty;
        if (versioned) {
            difficulty = new Difficulty(sc.next(), sc.nextInt(), sc.nextInt(), sc.nextInt());
        } else {
            // 旧档的字段顺序是 goal timeLimit stepLimit
            int goal = sc.nextInt(), timeLimit = sc.nextInt(), stepLimit = sc.nextInt();
            difficulty = DifficultyPreset.matching(goal, stepLimit, timeLimit);
        }

        List<PieceType> types = new ArrayList<>(rows * cols);
        for (int index = 0; index < rows * cols; index++) {
            if (!sc.hasNextInt()) break;
            int code = sc.nextInt();
            types.add(code == EMPTY_CELL ? null : PieceType.ofIndex(Math.clamp(code, 0, PieceType.values().length - 1)));
        }
        while (types.size() < rows * cols) types.add(null);
        return Optional.of(new GameState(score, timeLeft, stepLeft, difficulty, new BoardSnapshot(rows, cols, types)));
    }
}
