package view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static view.MenuFrame.isDarkMode;

public abstract class MyFrame extends JFrame{
    public static boolean isImageBackground=false;
    static void addComponent(JFrame motherFrame, GridBagLayout gbl, Component comp,
                                    int gridx, int grid_y, int grid_height, int grid_width, int weight_x, int weight_y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = weight_x;
        gbc.weighty = weight_y;
        gbc.gridheight = grid_height;
        gbc.gridwidth = grid_width;
        gbc.gridx = gridx;
        gbc.gridy = grid_y;
        gbl.setConstraints(comp, gbc);
        motherFrame.add(comp);
    }
    static JButton initButton(String name){
        JButton button = new JButton(name);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        button.setForeground(Color.BLACK);
        button.setBackground(Color.LIGHT_GRAY);
        return button;
    }
    static ArrayList<JRadioButton> initSelectButtons(String... name){
        ButtonGroup group = new ButtonGroup();
        ArrayList<JRadioButton> buttons = new ArrayList<>();
        for (var i:name){
            var button = new JRadioButton(i);
            button.setFont(new Font("Rockwell", Font.BOLD, 20));
            button.setForeground(Color.BLACK);
            button.setBackground(Color.LIGHT_GRAY);
            group.add(button);
            buttons.add(button);
        }
        return buttons;
    }
    static JLabel initLabel(String name){
        JLabel label = new JLabel(name);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        label.setForeground(Color.BLACK);
        return label;
    }
    abstract void setDarkMode();
    static BufferedImage pickBackgroundImage(){
        var files = readFiles("resource/texture/background/"+(isDarkMode?"dark/":"light/"));
        try {
            if (files != null) return ImageUtils.readImage(files.get(new Random().nextInt(files.size())));
        } catch (Exception ignored){}
        try {
            return ImageUtils.readImage("resource/texture/background/default.png");
        } catch (IOException e) {
            System.err.println("Background picture: Fail to load");
        }
        return null;
    }
    static ArrayList<File> readFiles(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return null;
        ArrayList<File> fileList = new ArrayList<>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) fileList.addAll(readFiles(f.getAbsolutePath()));
                    else fileList.add(f);
                }
            }
        } else fileList.add(file);
        return fileList;
    }

//    @Override
//    public void paint(Graphics g) {
//        var backgroundImage = pickBackgroundImage();
//        if (backgroundImage!=null) g.drawImage(backgroundImage, 0, 0, null);
//    }
}
