package view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static view.ImageUtils.scaleImage;
import static view.MenuFrame.isDarkMode;

public abstract class MyFrame extends JFrame{
    public static boolean isImageBackground=false;
    public static Image currentBackgroundImage = ImageUtils.readImage("resource/texture/background/default.png");
    public static ImageIcon currentBackground = new ImageIcon("resource/texture/background/default.png");
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
    static void addComponent(JPanel motherPanel, GridBagLayout gbl, Component comp,
                             int gridx, int grid_y, int grid_height, int grid_width, int weight_x, int weight_y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = weight_x;
        gbc.weighty = weight_y;
        gbc.gridheight = grid_height;
        gbc.gridwidth = grid_width;
        gbc.gridx = gridx;
        gbc.gridy = grid_y;
        gbl.setConstraints(comp, gbc);
        motherPanel.add(comp);
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
    void setDarkMode(){
        setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        setDarkMode((JPanel)getContentPane());
    }
    static BufferedImage pickBackgroundImage(){
        var files = readFiles("resource/texture/background/"+(isDarkMode?"dark/":"light/"));
        try {
            if (files != null) {
                File imageInput = files.get(new Random().nextInt(files.size()));
                System.out.println("CurrentBackground: "+imageInput.getName());
                return ImageUtils.readImage(imageInput);
            }
        } catch (Exception ignored){}
        System.err.println("CurrentBackground: unable to pick one, switch to default");
        return ImageUtils.readImage("resource/texture/background/default.png");
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
    static void resetBackgroundImage(Dimension dimension){
        currentBackgroundImage = pickBackgroundImage();
        scaleBackgroundImage(dimension);
    }
    static void scaleBackgroundImage(Dimension dimension){
        currentBackground = new ImageIcon(scaleImage(currentBackgroundImage,dimension.width,dimension.height));
    }
    public static void switchTheme(){
        try {
            for (var i:getFrames()) {
                MyFrame myFrame = (MyFrame) i;
                myFrame.setDarkMode();
            }
        } catch (Exception ignored) {}
    }
    static <T extends JComponent> void setDarkMode(T component){
        component.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        for (var i:component.getComponents()){
            if (!(i instanceof JComponent)) continue;
            if (i instanceof JPanel){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                setDarkMode((JPanel) i);
            }
            if (i instanceof JButton){
                i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            else {
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
        }
    }
}
