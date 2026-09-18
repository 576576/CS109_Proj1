package listener;

import model.BoardPoint;
import ui.TileView;

import java.io.File;

public interface GameListener {

    /** 点了一个空格子。 */
    void onPlayerClickCell(BoardPoint point);

    /** 点了一枚棋子。 */
    void onPlayerClickPiece(BoardPoint point, TileView component);

    void onPlayerSwapChess();

    void onPlayerNextStep();

    void loadFromFile(File file);
    void saveToFile(File file);

    void terminate();
}
