package listener;

import model.BoardPoint;
import view.CellComponent;
import view.TileView;

import java.io.File;

public interface GameListener {

    void onPlayerClickCell(BoardPoint point, CellComponent component);


    void onPlayerClickPiece(BoardPoint point, TileView component);

    void onPlayerSwapChess();

    void onPlayerNextStep();

    void loadFromFile(File file);
    void saveToFile(File file);

    void terminate();
}
