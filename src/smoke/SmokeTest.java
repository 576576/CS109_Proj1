package smoke;

import model.Board;
import model.BoardPoint;
import model.DifficultyPreset;
import model.GameState;
import model.GameStateCodec;
import model.PieceBag;
import model.PieceType;
import model.Swap;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** 纯 main 方法的冒烟入口：{@code java -cp out smoke.SmokeTest}。 */
public final class SmokeTest {
    private static int failures = 0;

    public static void main(String[] args) {
        checkInitialBoardHasNoMatch();
        checkSameSeedReproducesBoard();
        checkListMatchesFindsRowAndColumn();
        checkBoardSettlesIntoNoHoles();
        checkHintLeadsToAMatch();
        checkSaveRoundTrip();
        checkLegacySaveStillLoads();
        checkSnapshotIsImmutable();
        checkPointDistance();
        checkTexturesResolveFromAnyWorkingDirectory();

        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("All smoke checks passed");
    }

    /** 打成 exe 后启动目录不一定是工程目录，纹理必须还能找到。 */
    private static void checkTexturesResolveFromAnyWorkingDirectory() {
        for (PieceType type : PieceType.values()) {
            var texture = java.nio.file.Path.of(type.texturePath());
            expect(java.nio.file.Files.isRegularFile(texture),
                    "texture should resolve: " + texture + " (cwd=" + java.nio.file.Path.of("").toAbsolutePath() + ")");
        }
    }

    private static void checkInitialBoardHasNoMatch() {
        for (int seed = 0; seed < 50; seed++) {
            Board board = new Board(new PieceBag(new Random(seed)));
            board.initPieces();
            expect(board.listMatches().isEmpty(), "initial board should hide no match, seed=" + seed);
        }
    }

    private static void checkSameSeedReproducesBoard() {
        var seeded = new Board(new PieceBag(new Random(7)));
        var repeat = new Board(new PieceBag(new Random(7)));
        seeded.initPieces();
        repeat.initPieces();
        expect(seeded.snapshot().equals(repeat.snapshot()), "same seed must reproduce the same board");
    }

    private static void checkListMatchesFindsRowAndColumn() {
        PieceType[] cycle = PieceType.values();
        var board = new Board();
        for (BoardPoint point : board.points()) {
            board.setPieceAt(point, cycle[(point.row() + point.col()) % 3]);
        }
        expect(board.listMatches().isEmpty(), "the diagonal pattern should hide no match");

        board.setPieceAt(new BoardPoint(0, 0), cycle[4]);
        board.setPieceAt(new BoardPoint(0, 1), cycle[4]);
        board.setPieceAt(new BoardPoint(0, 2), cycle[4]);
        board.setPieceAt(new BoardPoint(4, 4), cycle[5]);
        board.setPieceAt(new BoardPoint(5, 4), cycle[5]);
        board.setPieceAt(new BoardPoint(6, 4), cycle[5]);

        List<BoardPoint> matched = board.listMatches();
        expect(matched.size() == 6, "should report the row triple and the column triple only, got " + matched.size());
        expect(matched.contains(new BoardPoint(0, 0)) && matched.contains(new BoardPoint(6, 4)),
                "reported cells should cover both triples");
    }

    private static void checkSnapshotIsImmutable() {
        var board = new Board();
        board.initPieces();
        try {
            board.snapshot().types().set(0, PieceType.EYE);
            expect(false, "snapshot should reject mutation");
        } catch (UnsupportedOperationException _) {
            expect(true, "snapshot rejects mutation");
        }
    }

    private static void checkBoardSettlesIntoNoHoles() {
        var board = new Board(new PieceBag(new Random(11)));
        board.initPieces();
        for (int round = 0; round < 200; round++) {
            for (BoardPoint point : board.listMatches()) board.removePieceAt(point);
            board.refill();
            board.collapse();
        }
        expect(!board.hasEmptyCells(), "a settled board should carry no hole");
        expect(board.collapse().isEmpty(), "a settled board has nothing left to fall");
    }

    private static void checkHintLeadsToAMatch() {
        for (int seed = 0; seed < 50; seed++) {
            var board = new Board(new PieceBag(new Random(seed)));
            board.initPieces();
            Optional<Swap> hint = board.findHint();
            if (hint.isEmpty()) continue;
            Swap swap = hint.get();
            board.swapPieces(swap.first(), swap.second());
            expect(!board.listMatches().isEmpty(), "the hinted swap should match, seed=" + seed);
            board.swapPieces(swap.first(), swap.second());
        }
    }

    private static void checkSaveRoundTrip() {
        var board = new Board(new PieceBag(new Random(3)));
        board.initPieces();
        var state = new GameState(12, 90, 7, DifficultyPreset.NORMAL.difficulty(), board.snapshot());
        Optional<GameState> back = GameStateCodec.fromText(
                GameStateCodec.toText(state), board.rows(), board.cols());
        expect(back.isPresent(), "a written save should parse back");
        expect(back.isPresent() && back.get().equals(state), "save round trip should be lossless");
    }

    private static void checkLegacySaveStillLoads() {
        String legacy = """
                0 173 46 180 180 46
                0 1 2 3 4 5 0 1
                1 2 3 4 5 0 1 2
                """;
        Optional<GameState> state = GameStateCodec.fromText(legacy, Board.DEFAULT_SIZE, Board.DEFAULT_SIZE);
        if (state.isEmpty()) {
            expect(false, "a legacy save should still parse");
            return;
        }
        GameState loaded = state.get();
        expect(loaded.score() == 0 && loaded.timeLeft() == 173 && loaded.stepLeft() == 46, "legacy counters should match");
        expect(loaded.difficulty().goal() == 180
                        && loaded.difficulty().stepLimit() == 46
                        && loaded.difficulty().timeLimit() == 180,
                "legacy difficulty fields should match");
        expect(loaded.board().typeAt(new BoardPoint(0, 0)) == PieceType.ofIndex(0)
                        && loaded.board().typeAt(new BoardPoint(0, 6)) == PieceType.ofIndex(0)
                        && loaded.board().typeAt(new BoardPoint(0, 7)) == PieceType.ofIndex(1),
                "legacy board should be read row by row");
    }

    private static void checkPointDistance() {
        expect(new BoardPoint(0, 0).distanceTo(new BoardPoint(3, 4)) == 7, "distance should be Manhattan");
        expect(new BoardPoint(2, 2).isAdjacentTo(new BoardPoint(2, 3)), "neighbours in the same row are adjacent");
        expect(!new BoardPoint(2, 2).isAdjacentTo(new BoardPoint(3, 3)), "diagonal cells are not adjacent");
    }

    private static void expect(boolean condition, String description) {
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + description);
        }
    }
}
