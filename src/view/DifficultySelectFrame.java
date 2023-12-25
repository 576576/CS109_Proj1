package view;

import model.Difficulty;
import model.DifficultyPreset;

import javax.swing.*;
import java.awt.*;
import static view.MenuFrame.isOnlinePlay;
import static view.MenuFrame.isToHost;
import static view.MenuFrame.difficulty;

public class DifficultySelectFrame extends JFrame implements MyFrame {
    private int goal=0,timeLimit=0,stepLimit=0;
    private MenuFrame menuFrame;
    public DifficultySelectFrame(MenuFrame menuFrame){
        setTitle("Select a difficulty");
        setSize(600,400);
        setLayout(new GridLayout(1,1));
        JPanel selectPanel = new JPanel(new GridLayout(isOnlinePlay?3:2,1));
        var startButton = MyFrame.initButton("Start Game!");
        startButton.addActionListener(e -> {
            menuFrame.generateNewGame();
            this.dispose();
        });
        selectPanel.add(startButton);
        if (isOnlinePlay){
            var onlineButtons = MyFrame.initSelectButtons("Host Game","Join Game");
            for (var i:onlineButtons){
                i.setBackground(Color.DARK_GRAY);
                i.setForeground(Color.WHITE);
            }
            onlineButtons.get(0).addActionListener(e -> isToHost=true);
            onlineButtons.get(1).addActionListener(e -> isToHost=false);
            JPanel panel = new JPanel(new GridLayout(1,2));
            for (int i = 0; i < 2; i++) panel.add(onlineButtons.get(i));
            selectPanel.add(panel);
        }
        else selectPanel.add(new Box(1));
        var difficultyButtons = MyFrame.initSelectButtons("Easy","Normal","Hard","Custom");
        difficultyButtons.get(0).addActionListener(e -> difficulty=new Difficulty(DifficultyPreset.EASY));
        difficultyButtons.get(1).addActionListener(e -> difficulty=new Difficulty(DifficultyPreset.NORMAL));
        difficultyButtons.get(2).addActionListener(e -> difficulty=new Difficulty(DifficultyPreset.NORMAL));
        difficultyButtons.get(3).addActionListener(e -> {
            try {
                String input;
                do {
                    input = JOptionPane.showInputDialog(null,"Input goal","Creating Difficulty",JOptionPane.PLAIN_MESSAGE);
                }while (!isNumeric(input));
                goal=Integer.parseInt(input);
                do {
                    input = JOptionPane.showInputDialog(null,"Input Step Limit","Creating Difficulty",JOptionPane.PLAIN_MESSAGE);
                }while (!isNumeric(input)||input.isEmpty());
                stepLimit=Integer.parseInt(input)==0?-1:Integer.parseInt(input);
                do {
                    input = JOptionPane.showInputDialog(null,"Input Time Limit","Creating Difficulty",JOptionPane.PLAIN_MESSAGE);
                }while (!isNumeric(input)||input.isEmpty());
                timeLimit=Integer.parseInt(input)==0?-1:Integer.parseInt(input);
                difficulty=new Difficulty(goal,stepLimit,timeLimit);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,"Custom Difficulty Stop,set difficulty to Easy.","Error",JOptionPane.WARNING_MESSAGE);
                difficultyButtons.get(3).setSelected(false);
                difficultyButtons.get(0).setSelected(true);
            }
        });
        JPanel panel = new JPanel(new GridLayout(1,4));
        for (int i = 0; i < 4; i++) panel.add(difficultyButtons.get(i));
        selectPanel.add(panel);
        add(selectPanel);
    }

    private boolean isNumeric(String str) {
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
