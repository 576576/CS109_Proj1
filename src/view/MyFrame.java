package view;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public interface MyFrame {
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
    void setDarkMode();
}
