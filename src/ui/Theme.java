package ui;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.glavo.monetfx.Brightness;
import org.glavo.monetfx.ColorScheme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;

import util.Log;
import util.ResourceRoot;

/**
 * 界面上所有颜色都由 MonetFX 生成，种子色取自当前壁纸的主色。
 * 换一张背景，按钮、面板、棋盘格子的配色就整套跟着变——这是 Monet 的本来用法，
 * 也正好接住游戏原本"随机挑一张背景图"的行为。
 */
public final class Theme {

    private static final Color FALLBACK_SEED = Color.web("#6750A4");
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private boolean dark = true;
    private boolean wallpaper = true;

    private ColorScheme scheme;
    private Image background;
    private Path stylesheet;

    public Theme() {
        regenerate();
    }

    /** 明暗、壁纸开关或壁纸本身变化之后重新取色。 */
    public void regenerate() {
        Image image = wallpaper ? randomWallpaper() : null;
        background = image;
        var builder = ColorScheme.newBuilder().setBrightness(dark ? Brightness.DARK : Brightness.LIGHT);
        scheme = image == null ? builder.setPrimaryColorSeed(FALLBACK_SEED).build()
                : builder.setWallpaper(image).build();
        stylesheet = writeStylesheet();
        Log.info("Theme: " + (dark ? "dark" : "light") + ", primary=" + primary()
                + (image == null ? " (seed)" : " (from wallpaper)"));
    }

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
        regenerate();
    }

    public boolean isWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(boolean wallpaper) {
        this.wallpaper = wallpaper;
        regenerate();
    }

    /** 换一张壁纸并重新取色。 */
    public void nextWallpaper() {
        if (!wallpaper) return;
        regenerate();
    }

    public Image background() {
        return background;
    }

    /** Monet 样式表的 URI，挂到 Scene.getStylesheets() 上让 -monet-* 变量生效。 */
    public String stylesheet() {
        return stylesheet == null ? null : stylesheet.toUri().toString();
    }

    public Color primary() {
        return scheme.getPrimary();
    }

    public Color onPrimary() {
        return scheme.getOnPrimary();
    }

    public Color primaryContainer() {
        return scheme.getPrimaryContainer();
    }

    public Color onPrimaryContainer() {
        return scheme.getOnPrimaryContainer();
    }

    public Color secondaryContainer() {
        return scheme.getSecondaryContainer();
    }

    public Color surface() {
        return scheme.getSurface();
    }

    public Color onSurface() {
        return scheme.getOnSurface();
    }

    public Color surfaceVariant() {
        return scheme.getSurfaceVariant();
    }

    public Color onSurfaceVariant() {
        return scheme.getOnSurfaceVariant();
    }

    public Color outline() {
        return scheme.getOutline();
    }

    public Color error() {
        return scheme.getError();
    }

    /** JavaFX 的 setStyle 只认 CSS 颜色字面量，所以要把 Color 转成 #rrggbb。 */
    public static String hex(Color color) {
        int rgb = (int) (color.getRed() * 255) << 16
                | (int) (color.getGreen() * 255) << 8
                | (int) (color.getBlue() * 255);
        return "#%06x".formatted(rgb);
    }

    /** 带透明度的色值，用于 hover 之类的叠加高亮。 */
    public static String hexA(Color color, double alpha) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return "rgba(%d, %d, %d, %.3f)".formatted(r, g, b, alpha);
    }

    /** 带颜色自身透明度的色值，scrim 之类的半透明底色用。 */
    public static String rgba(Color color) {
        return hexA(color, color.getOpacity());
    }

    /** 背景图上叠一层半透明底色，保证文字在壁纸上面仍然读得清。 */
    public Color scrim() {
        return dark ? Color.rgb(0, 0, 0, 0.55) : Color.rgb(255, 255, 255, 0.62);
    }

    private Image randomWallpaper() {
        List<Path> files = wallpaperFiles();
        if (files.isEmpty()) return null;
        try {
            Path chosen = files.get(RANDOM.nextInt(files.size()));
            Log.info("Wallpaper: " + chosen.getFileName());
            Image image = new Image(chosen.toUri().toString());
            return image.isError() ? null : image;
        } catch (RuntimeException e) {
            Log.warn("Cannot read wallpaper: " + e);
            return null;
        }
    }

    private List<Path> wallpaperFiles() {
        Path directory = ResourceRoot.path("texture/background/" + (dark ? "dark" : "light"));
        try (var entries = Files.list(directory)) {
            return entries.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                    .toList();
        } catch (IOException | RuntimeException e) {
            Log.warn("No wallpapers under " + directory);
            return List.of();
        }
    }

    private Path writeStylesheet() {
        try {
            Path file = Files.createTempFile("monet-", ".css");
            // MaterialFX 把控件文字色写死成黑色 looked-up color（-mfx-text-*），深色下读不清。
            // 作者样式表优先级高于它的 user-agent 样式表，这里统一覆盖成 Monet 色板。
            String overrides = """
                    * {
                      -mfx-text-he: %s;
                      -mfx-text-me: %s;
                      -mfx-text-disabled: %s;
                    }
                    .mfx-checkbox, .mfx-radio-button {
                      -mfx-main: %s;
                      -mfx-secondary: %s;
                    }
                    .mfx-slider {
                      -mfx-main-color: %s;
                      -mfx-main-color-hover: %s;
                      -mfx-main-color-pressed: %s;
                      -mfx-disabled-color: %s;
                    }
                    """.formatted(
                    hex(onSurface()), hex(onSurfaceVariant()), hex(outline()),
                    hex(primary()), hex(onSurfaceVariant()),
                    hex(primary()), hexA(primary(), 0.10), hexA(primary(), 0.30), hex(outline()));
            Files.writeString(file, scheme.toStyleSheet() + "\n" + overrides);
            return file;
        } catch (IOException e) {
            Log.warn("Cannot write Monet stylesheet: " + e);
            return null;
        }
    }
}
