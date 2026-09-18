package smoke;

import config.GameSettings;
import config.PlayMode;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import model.DifficultyPreset;
import ui.Match3App;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 把界面截下来看一眼，命令行里没法靠读代码确认 UI 长什么样。 */
public class UiShot {

    public static void main(String[] args) throws Exception {
        String menu = args.length > 0 ? args[0] : "build/shot-menu.png";
        String game = args.length > 1 ? args[1] : "build/shot-game.png";

        FxHarness.boot();
        Thread.sleep(1500);
        FxHarness.onFx(() -> snapshot(Match3App.get().scene(), menu));

        var settings = new GameSettings();
        settings.setDifficulty(DifficultyPreset.EASY.difficulty());
        settings.setPlayMode(PlayMode.NEW_LOCAL);
        FxHarness.onFx(() -> Match3App.get().showGame(settings));
        Thread.sleep(2500);
        FxHarness.onFx(() -> {
            snapshot(Match3App.get().scene(), game);
            System.out.println("tiles on board = " + countTiles());
        });

        FxHarness.shutdown();
        System.exit(0);
    }

    private static int countTiles() {
        int n = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (Match3App.get().controller().getBoard().tileAt(new model.BoardPoint(row, col)) != null) n++;
            }
        }
        return n;
    }

    /** 对整个场景截图写 PNG；不依赖 javafx.swing 的 SwingFXUtils，自己逐像素搬。 */
    private static void snapshot(javafx.scene.Scene scene, String path) {
        try {
            WritableImage image = scene.getRoot().snapshot(new SnapshotParameters(), null);
            int w = (int) image.getWidth(), h = (int) image.getHeight();
            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = image.getPixelReader().getColor(x, y);
                    buffered.setRGB(x, y, (int) (c.getOpacity() * 255) << 24
                            | (int) (c.getRed() * 255) << 16
                            | (int) (c.getGreen() * 255) << 8
                            | (int) (c.getBlue() * 255));
                }
            }
            File out = new File(path);
            out.getParentFile().mkdirs();
            ImageIO.write(buffered, "png", out);
            System.out.println("shot -> " + out.getAbsolutePath() + " " + w + "x" + h);
        } catch (Exception e) {
            System.out.println("snapshot failed: " + e);
        }
    }
}
