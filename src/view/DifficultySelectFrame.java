package view;

import model.Difficulty;
import model.DifficultyPreset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

import static view.GameFrame.isOnlinePlay;
import static view.MenuFrame.difficulty;
import static view.MenuFrame.startPlayMode;

public class DifficultySelectFrame extends MyFrame{
    public static File selectedFile;
    private final JFileChooser jf = new JFileChooser(".\\");

    public DifficultySelectFrame(MenuFrame menuFrame){
        setTitle("Select a difficulty");
        setSize(600,400);
        setLayout(new GridLayout(1,1));
        setLocationRelativeTo(null);

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter ff = new FileNameExtensionFilter("savedGame.txt", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        JPanel selectPanel = new JPanel(new GridLayout(3,1));
        var startButton = initButton("Start Game!");
        startButton.addActionListener(e -> {
            menuFrame.generateNewGame();
            this.dispose();
        });
        selectPanel.add(startButton);
        if (isOnlinePlay()){
            var onlineButtons = initSelectButtons("Host Game","Join Game");
            for (var i:onlineButtons){
                i.setBackground(Color.DARK_GRAY);
                i.setForeground(Color.WHITE);
            }
            onlineButtons.get(0).addActionListener(e -> startPlayMode=3);
            onlineButtons.get(1).addActionListener(e -> startPlayMode=4);
            onlineButtons.get(1).setSelected(true);
            JPanel panel = new JPanel(new GridLayout(1,2));
            for (int i = 0; i < 2; i++) panel.add(onlineButtons.get(i));
            selectPanel.add(panel);
        }
        else {
            var localButtons = initSelectButtons("New","Load");
            for (var i:localButtons){
                i.setBackground(Color.DARK_GRAY);
                i.setForeground(Color.WHITE);
            }
            localButtons.get(0).addActionListener(e -> startPlayMode = 1);
            localButtons.get(1).addActionListener(e -> {
                startPlayMode = 0;
                int result = jf.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = jf.getSelectedFile();
                    startPlayMode=2;
                }else {
                    localButtons.getFirst().setSelected(true);
                    startPlayMode=1;
                    System.out.println("No file selected!");
                    JOptionPane.showMessageDialog(this,"\"No file selected!\"");
                }
            });
            localButtons.get(0).setSelected(true);
            JPanel panel = new JPanel(new GridLayout(1,2));
            for (int i = 0; i < 2; i++) panel.add(localButtons.get(i));
            selectPanel.add(panel);
        }

        var difficultyButtons = initSelectButtons("Easy", "Normal", "Hard", "Custom");
        DifficultyPreset[] presets = DifficultyPreset.values();
        for (int i = 0; i < presets.length; i++) {
            Difficulty preset = presets[i].difficulty();
            difficultyButtons.get(i).addActionListener(e -> {
                difficulty = preset;
                System.out.println("Difficulty Selected: " + preset.name());
            });
        }
        difficultyButtons.getLast().addActionListener(e -> {
            var difficultyCreateFrame = new DifficultyCreateFrame();
            difficultyCreateFrame.setVisible(true);
        });
        JPanel panel = new JPanel(new GridLayout(1,4));
        for (int i = 0; i < 4; i++) panel.add(difficultyButtons.get(i));
        selectPanel.add(panel);
        difficultyButtons.getFirst().setSelected(true);
        add(selectPanel);
    }

    public void setDarkMode() {

    }
}
