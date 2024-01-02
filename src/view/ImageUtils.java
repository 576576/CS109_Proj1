package view;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    public static <T> BufferedImage readImage(T imageInput) throws IOException,NullPointerException{
        if (imageInput instanceof File){
            return ImageIO.read((File) imageInput);
        }
        if (imageInput instanceof InputStream){
            return ImageIO.read((InputStream) imageInput);
        }
        if (imageInput instanceof String){
            return ImageIO.read(new File((String) imageInput));
        }
        return null;
    }
    public static int getImageWidth(BufferedImage image) {
        return image.getWidth();
    }
    public static int getImageHeight(BufferedImage image) {
        return image.getHeight();
    }
    public static BufferedImage createScaledImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }
    public static BufferedImage scaleImage(Image image, int width, int height) {
        BufferedImage scaledImage = createScaledImage(width, height);
        Graphics2D graphics2D = scaledImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return scaledImage;
    }
}
