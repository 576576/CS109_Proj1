package smoke;

import model.Board;
import model.BoardPoint;
import model.PieceBag;
import model.PieceType;

import java.util.List;
import java.util.Random;

/** 纯 main 方法的冒烟入口：{@code java -cp out smoke.SmokeTest}。 */
public final class SmokeTest {
    private static int failures = 0;

    public static void main(String[] args) {
        checkInitialBoardHasNoMatch();
        checkSameSeedReproducesBoard();
        checkListMatchesFindsRowAndColumn();
        checkSnapshotIsImmutable();
        checkPointDistance();

        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("All smoke checks passed");
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
        } catch (UnsupportedOperationException expected) {
            expect(true, "snapshot rejects mutation");
        }
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
