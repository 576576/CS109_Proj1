package view;

import javax.swing.*;
import java.awt.*;

public interface MyFrame {
    static void addComponent(JFrame motherFrame, GridBagLayout gbl, Component comp,
                                    int gridx, int gridy, int gridheight, int gridwidth, int weight_x, int weight_y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = weight_x;
        gbc.weighty = weight_y;
        gbc.gridheight = gridheight;
        gbc.gridwidth = gridwidth;
        gbc.gridx = gridx;
        gbc.gridy = gridy;
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
    static JLabel initLabel(String name){
        JLabel label = new JLabel(name);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        label.setForeground(Color.BLACK);
        return label;
    }
}
