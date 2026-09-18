package smoke;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 画应用图标：渐变圆角底 + 三枚宝石，按 ICO 需要的尺寸各导一份，再合成 .ico。
 * ICO 从 Vista 起支持内嵌 PNG，所以这里直接把 PNG 塞进去，不用手写 BMP。
 * 只用命令行手动跑：`java -cp build/libs/match3.jar smoke.IconGen`
 */
public class IconGen {

    private static final int[] SIZES = {512, 256, 128, 64, 48, 32, 16};

    public static void main(String[] args) throws Exception {
        int master = 512;
        BufferedImage image = draw(master);
        File pngDir = new File("resource/icon");
        pngDir.mkdirs();
        ImageIO.write(image, "png", new File(pngDir, "app.png"));

        File ico = new File("packaging/app.ico");
        ico.getParentFile().mkdirs();
        writeIco(image, ico);
        System.out.println("icon -> " + new File(pngDir, "app.png").getAbsolutePath());
        System.out.println("icon -> " + ico.getAbsolutePath());
    }

    private static BufferedImage draw(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double s = size / 512.0;

        // 圆角底，深紫渐变
        g.setPaint(new GradientPaint(0, 0, new Color(0x6C5CE7), size, size, new Color(0x3B2E8C)));
        g.fill(new RoundRectangle2D.Double(0, 0, size, size, 112 * s, 112 * s));

        // 内侧一圈高光，让底面不那么平
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 46), 0, size, new Color(255, 255, 255, 0)));
        g.fill(new RoundRectangle2D.Double(0, 0, size, size * 0.62, 112 * s, 112 * s));

        // 三枚宝石：菱形 / 圆 / 三角
        int[] colors = {0x4FC3F7, 0xFFD54F, 0x81C784};
        int gem = (int) (118 * s);
        int gap = (int) (26 * s);
        int total = gem * 3 + gap * 2;
        int x0 = (size - total) / 2;
        int y0 = (size - gem) / 2;
        for (int i = 0; i < 3; i++) {
            int x = x0 + i * (gem + gap);
            shadow(g, x, y0, gem, s);
            g.setColor(new Color(colors[i]));
            switch (i) {
                case 0 -> gem(g, x, y0, gem, s);
                case 1 -> orb(g, x, y0, gem, s);
                default -> shard(g, x, y0, gem, s);
            }
        }
        g.dispose();
        return image;
    }

    private static void shadow(Graphics2D g, int x, int y, int gem, double s) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(x + (int) (5 * s), y + (int) (9 * s), gem, gem);
    }

    /** 菱形。 */
    private static void gem(Graphics2D g, int x, int y, int gem, double s) {
        int c = gem / 2;
        Polygon diamond = new Polygon(
                new int[]{x + c, x + gem, x + c, x},
                new int[]{y, y + c, y + gem, y + c}, 4);
        g.fillPolygon(diamond);
        g.setColor(new Color(255, 255, 255, 110));
        Polygon facet = new Polygon(
                new int[]{x + c, x + gem - (int) (28 * s), x + c},
                new int[]{y + (int) (26 * s), y + c, y + gem - (int) (26 * s)}, 3);
        g.fillPolygon(facet);
    }

    /** 圆球，加一块高光。 */
    private static void orb(Graphics2D g, int x, int y, int gem, double s) {
        g.fillOval(x, y, gem, gem);
        g.setColor(new Color(255, 255, 255, 130));
        g.fillOval(x + (int) (24 * s), y + (int) (18 * s), (int) (34 * s), (int) (24 * s));
    }

    /** 三角。 */
    private static void shard(Graphics2D g, int x, int y, int gem, double s) {
        int inset = (int) (10 * s);
        Polygon triangle = new Polygon(
                new int[]{x + gem / 2, x + gem - inset, x + inset},
                new int[]{y + inset, y + gem - inset, y + gem - inset}, 3);
        g.fillPolygon(triangle);
        g.setColor(new Color(255, 255, 255, 90));
        Polygon facet = new Polygon(
                new int[]{x + gem / 2, x + gem / 2, x + gem - inset},
                new int[]{y + inset, y + gem - inset, y + gem - inset}, 3);
        g.fillPolygon(facet);
    }

    /** ICO = 6 字节头 + 每个尺寸 16 字节目录项 + 各尺寸的 PNG。 */
    private static void writeIco(BufferedImage master, File out) throws Exception {
        try (OutputStream raw = new FileOutputStream(out); ZipOutputStream unused = null) {
            var pngs = new java.util.ArrayList<byte[]>();
            for (int size : SIZES) {
                var scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(master, 0, 0, size, size, null);
                g.dispose();
                var bytes = new java.io.ByteArrayOutputStream();
                ImageIO.write(scaled, "png", bytes);
                pngs.add(bytes.toByteArray());
            }

            var buffer = new java.io.ByteArrayOutputStream();
            buffer.write(new byte[]{0, 0, 1, 0, (byte) SIZES.length, 0});
            int offset = 6 + 16 * SIZES.length;
            for (int i = 0; i < SIZES.length; i++) {
                int size = SIZES[i];
                buffer.write(size >= 256 ? 0 : size);
                buffer.write(size >= 256 ? 0 : size);
                buffer.write(0);
                buffer.write(0);
                buffer.write(new byte[]{1, 0, 32, 0});
                int length = pngs.get(i).length;
                buffer.write(length & 0xFF);
                buffer.write(length >> 8 & 0xFF);
                buffer.write(length >> 16 & 0xFF);
                buffer.write(length >> 24 & 0xFF);
                buffer.write(offset & 0xFF);
                buffer.write(offset >> 8 & 0xFF);
                buffer.write(offset >> 16 & 0xFF);
                buffer.write(offset >> 24 & 0xFF);
                offset += length;
            }
            for (byte[] png : pngs) buffer.write(png);
            raw.write(buffer.toByteArray());
        }
    }
}
