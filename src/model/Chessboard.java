package model;

import static controller.GameController.isNewGameInitialized;
import static view.ChessComponent.chessTypes;

/**
 * This class store the real chess information.
 * The Chessboard has 8 * 8 cells, and each cell has a position for chess
 */
public class Chessboard {
    private final Cell[][] grid;

    public Chessboard() {
        this.grid =
                new Cell[Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum()][Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum()];
        initGrid();
        initPieces();
    }

    private void initGrid() {
        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                grid[i][j] = new Cell();
            }
        }
    }

    // When initialize from the main menu, this method was used
    public void initPieces() {
        isNewGameInitialized=false;

        for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
            for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                grid[i][j].setPiece(new ChessPiece(Util.RandomPick(chessTypes)));
            }
        }

        while(checkerBoardValidator(grid)){
            for (int i = 0; i < Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum(); i++) {
                for (int j = 0; j < Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum(); j++) {
                    grid[i][j].setPiece(new ChessPiece(Util.RandomPick(chessTypes)));
                }
            }
            // for debug, print generated chessboard
            // printChessBoardGrid(grid);
        }
//        System.out.println("New pieces initialized");
        isNewGameInitialized=true;
    }
    // This method checks if there is any group of 3 same chess pieces, if there is not, return false.
    public static boolean checkerBoardValidator(Cell[][] grid){
        int rows = Constant.DEFAULT_CHESSBOARD_ROW_SIZE.getNum();
        int columns = Constant.DEFAULT_CHESSBOARD_COL_SIZE.getNum();

        for(int i=0; i<rows; i++) {
            for(int j=0; j<columns; j++) {
                try {
                    String currentPieceName = grid[i][j].getPiece().getName();

                // Check right
                if(j < columns-2 &&
                            grid[i][j+1].getPiece() != null &&
                            grid[i][j+2].getPiece() != null &&
                        grid[i][j+1].getPiece().getName().equals(currentPieceName) &&
                        grid[i][j+2].getPiece().getName().equals(currentPieceName)) {
                    return true;
                }

                // Check down
                if(i < rows-2 &&
                            grid[i+1][j].getPiece() != null &&
                            grid[i+2][j].getPiece() != null &&
                        grid[i+1][j].getPiece().getName().equals(currentPieceName) &&
                        grid[i+2][j].getPiece().getName().equals(currentPieceName)) {
                    return true;
                }
                } catch (NullPointerException ignored) {}
            }
        }

        return false;
    }

    public ChessPiece getChessPieceAt(ChessboardPoint point) {
        return getGridAt(point).getPiece();
    }

    public Cell getGridAt(ChessboardPoint point) {
        return grid[point.row()][point.col()];
    }

    public static int calculateDistance(ChessboardPoint src, ChessboardPoint destination) {
        return Math.abs(src.row() - destination.row()) + Math.abs(src.col() - destination.col());
    }

    public ChessPiece removeChessPiece(ChessboardPoint point) {
        ChessPiece chessPiece = getChessPieceAt(point);
        getGridAt(point).removePiece();
        return chessPiece;
    }

    public void setChessPiece(ChessboardPoint point, ChessPiece chessPiece) {
        getGridAt(point).setPiece(chessPiece);
    }


    public void swapChessPiece(ChessboardPoint point1, ChessboardPoint point2) {
        var p1 = getChessPieceAt(point1);
        var p2 = getChessPieceAt(point2);
        setChessPiece(point1, p2);
        setChessPiece(point2, p1);
    }


    public Cell[][] getGrid() {
        return grid;
    }


}
