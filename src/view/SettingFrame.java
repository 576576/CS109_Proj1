package view;

import javax.swing.*;
import java.awt.*;

import static controller.GameController.isAutoRestart;
import static view.MenuFrame.*;

public class SettingFrame extends MyFrame{
    private final JPanel formPanel = new JPanel(new GridLayout(3,1));
    private final JButton submitButton;
    public SettingFrame(){
        setTitle("Settings");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new Box(1),BorderLayout.NORTH);

        submitButton = initButton("Confirm");
        submitButton.addActionListener(e -> this.dispose());

        formPanel.add(getMusicPanel());
        formPanel.add(getThemePanel());
        formPanel.add(getCustomPanel());
        add(formPanel, BorderLayout.CENTER);
        add(submitButton,BorderLayout.SOUTH);
        setDarkMode();
        setVisible(true);
    }

    private static JPanel getMusicPanel() {
        JLabel volumeLabel = new JLabel("Music:");
        JSlider soundSlider = new JSlider(0,100,20);
        soundSlider.addChangeListener(e -> setVolume(soundSlider.getValue()));
        JPanel volumePanel = new JPanel(new FlowLayout());
        volumePanel.add(volumeLabel);
        volumePanel.add(soundSlider);
        return volumePanel;
    }

    private JPanel getCustomPanel(){
        JPanel customPanel = new JPanel(new FlowLayout());
        JCheckBox detailDialogCheckBox = new JCheckBox("Show detailed dialog");
        detailDialogCheckBox.setSelected(isDetailedDialog);
        detailDialogCheckBox.addActionListener(e -> isDetailedDialog=detailDialogCheckBox.isSelected());
        JCheckBox autoRestartCheckBox = new JCheckBox("Auto restart when game ends");
        autoRestartCheckBox.setSelected(isAutoRestart);
        autoRestartCheckBox.addActionListener(e -> isAutoRestart=autoRestartCheckBox.isSelected());
        customPanel.add(detailDialogCheckBox);
        customPanel.add(autoRestartCheckBox);
        return customPanel;
    }

    private JPanel getThemePanel() {
        JLabel themeLabel = new JLabel("Theme:");
        JRadioButton lightRadioButton = new JRadioButton("Light");
        lightRadioButton.setSelected(!isDarkMode);
        lightRadioButton.addActionListener(e -> {
            isDarkMode=false;
            switchTheme();
        });
        JRadioButton darkRadioButton = new JRadioButton("Dark");
        darkRadioButton.setSelected(isDarkMode);
        darkRadioButton.addActionListener(e -> {
            isDarkMode=true;
            switchTheme();
        });
        JCheckBox pictureCheckBox = new JCheckBox("Picture Background");
        pictureCheckBox.setSelected(isImageBackground);
        pictureCheckBox.addActionListener(e -> {
            isImageBackground=pictureCheckBox.isSelected();
            switchTheme();
        });
        ButtonGroup themeButtonGroup = new ButtonGroup();
        themeButtonGroup.add(lightRadioButton);
        themeButtonGroup.add(darkRadioButton);
        JPanel themePanel = new JPanel(new FlowLayout());
        themePanel.add(themeLabel);
        themePanel.add(lightRadioButton);
        themePanel.add(darkRadioButton);
        themePanel.add(pictureCheckBox);
        return themePanel;
    }
    public void setDarkMode(){
        setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        setDarkMode(formPanel);
        submitButton.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        submitButton.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        this.repaint();
    }
}
