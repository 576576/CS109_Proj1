package view;

import model.PieceType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.random.RandomGenerator;

import static view.ImageUtils.scaleImage;

/**
 * This is the equivalent of the PieceType class,
 * but this class only cares how to draw a tile on BoardView
 */
public class TileView extends JComponent {
    private static final String[] POINTER_SUFFIXES = {"", "2"};
    private static final RandomGenerator POINTER_RANDOM = RandomGenerator.getDefault();

    private boolean selected;
    private final PieceType type;

    public TileView(int size, PieceType type) {
        this.selected = false;
        setSize(size - 4, size - 4);
        setLocation(2, 2);
        setOpaque(false);
        setVisible(true);
        this.type = type;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (type != null && !drawTexture(g)) drawFallbackGlyph(g);
        if (isSelected()) drawSelectionPointer(g);
    }

    private boolean drawTexture(Graphics g) {
        try {
            BufferedImage chessImage = ImageUtils.readImage(type.texturePath());
            g.drawImage(scaleImage(chessImage, 68, 68), 0, 0, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void drawFallbackGlyph(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Helvetica", Font.PLAIN, getWidth() / 2));
        g2.setColor(type.color());
        g2.drawString(type.glyph(), getWidth() / 4, getHeight() * 5 / 8);
    }

    private void drawSelectionPointer(Graphics g) {
        try {
            String suffix = POINTER_SUFFIXES[POINTER_RANDOM.nextInt(POINTER_SUFFIXES.length)];
            BufferedImage pointerImage = ImageUtils.readImage("resource/texture/chess/select_pointer" + suffix + ".png");
            g.drawImage(scaleImage(pointerImage, 16, 16), 0, 0, null);
            g.drawImage(scaleImage(pointerImage, 16, 16), 0, 52, null);
            g.drawImage(scaleImage(pointerImage, 16, 16), 52, 0, null);
            g.drawImage(scaleImage(pointerImage, 16, 16), 52, 52, null);
        } catch (Exception e) {
            g.setColor(Color.gray);
            g.drawOval(3, 3, getWidth() - 4, getHeight() - 4);
            g.drawOval(2, 2, getWidth() - 6, getHeight() - 6);
        }
    }
}
