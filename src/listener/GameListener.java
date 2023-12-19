package listener;

import model.ChessboardPoint;
import view.CellComponent;
import view.ChessComponent;

import java.io.File;

public interface GameListener {

    void onPlayerClickCell(ChessboardPoint point, CellComponent component);


    void onPlayerClickChessPiece(ChessboardPoint point, ChessComponent component);

    void onPlayerSwapChess();

    void onPlayerNextStep();

    void loadFromFile(File file);
    void saveToFile(File file);

    void terminate();
}
