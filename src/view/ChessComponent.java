package view;


import model.ChessPiece;
import model.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static view.ImageUtils.scaleImage;

/**
 * This is the equivalent of the ChessPiece class,
 * but this class only cares how to draw Chess on ChessboardComponent
 */
public class ChessComponent extends JComponent {
    private boolean selected;
    private final ChessPiece chessPiece;
    public static String[] chessTypes = new String[]{"💎", "⚪", "▲", "🔶", "🙂", "👀"};
    public ChessComponent(int size, ChessPiece chessPiece) {
        this.selected = false;
        setSize(size-4, size-4);
        setLocation(2,2);
        setOpaque(false);
        setVisible(true);
        this.chessPiece = chessPiece;
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
        boolean isTextureFound = false;
        if (chessPiece != null) {
            for (int i = 0; i < chessTypes.length; i++) {
                if (chessTypes[i].equals(chessPiece.getName())) {
                    try {
                        BufferedImage chessImage=ImageUtils.readImage("resource/texture/chess/" + i + ".png");
//                        Graphics2D g2 = Objects.requireNonNull(chessImage).createGraphics();
//                        chessImage = g2.getDeviceConfiguration().createCompatibleImage(getWidth(),getHeight(),Transparency.TRANSLUCENT);
//                        g2.dispose(); //FIX ME:make the image translucent
                        g.drawImage(scaleImage(chessImage,68,68), 0, 0,null);
                        isTextureFound = true;
                        break;
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            if (!isTextureFound) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Font font = new Font("Helvetica", Font.PLAIN, getWidth() / 2);
                g2.setFont(font);
                g2.setColor(this.chessPiece.getColor());
                g2.drawString(this.chessPiece.getName(), getWidth() / 4, getHeight() * 5 / 8);
            }
        }
        if (isSelected()) { // Highlights the model if selected.
            try {
                BufferedImage pointerImage=ImageUtils.readImage("resource/texture/chess/select_pointer"+ Util.RandomPick(new String[]{"","2"})+".png");
                g.drawImage(scaleImage(pointerImage,16,16), 0, 0,null);
                g.drawImage(scaleImage(pointerImage,16,16), 0, 52,null);
                g.drawImage(scaleImage(pointerImage,16,16), 52, 0,null);
                g.drawImage(scaleImage(pointerImage,16,16), 52, 52,null);
            }catch (Exception e){
                g.setColor(Color.gray);
                g.drawOval(3, 3, getWidth()-4, getHeight()-4);
                g.drawOval(2, 2, getWidth()-6, getHeight()-6);
            }
        }
    }
}
