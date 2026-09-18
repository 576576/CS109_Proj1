package view;

import config.GameSettings;
import config.PlayMode;
import model.Difficulty;
import model.DifficultyPreset;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

import util.Log;

public class DifficultySelectFrame extends MyFrame{
    private final GameSettings settings;
    private final JFileChooser jf = new JFileChooser(".\\");

    public DifficultySelectFrame(MenuFrame menuFrame, GameSettings settings){
        this.settings = settings;
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
        if (settings.playMode().isOnline()){
            var onlineButtons = initSelectButtons("Host Game","Join Game");
            for (var i:onlineButtons){
                i.setBackground(Color.DARK_GRAY);
                i.setForeground(Color.WHITE);
            }
            onlineButtons.get(0).addActionListener(e -> settings.setPlayMode(PlayMode.HOST));
            onlineButtons.get(1).addActionListener(e -> settings.setPlayMode(PlayMode.JOIN));
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
            localButtons.get(0).addActionListener(e -> settings.setPlayMode(PlayMode.NEW_LOCAL));
            localButtons.get(1).addActionListener(e -> {
                settings.setPlayMode(PlayMode.NONE);
                int result = jf.showOpenDialog(this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    settings.setSaveFile(jf.getSelectedFile());
                    settings.setPlayMode(PlayMode.LOAD_LOCAL);
                }else {
                    localButtons.getFirst().setSelected(true);
                    settings.setPlayMode(PlayMode.NEW_LOCAL);
                    Log.info("No file selected!");
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
                settings.setDifficulty(preset);
                Log.info("Difficulty Selected: " + preset.name());
            });
        }
        difficultyButtons.getLast().addActionListener(e -> {
            var difficultyCreateFrame = new DifficultyCreateFrame(settings);
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
