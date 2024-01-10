package view;

import javax.swing.*;
import java.awt.*;

import static view.MenuFrame.*;

public class SettingFrame extends MyFrame{
    private final JPanel formPanel = new JPanel(new GridLayout(2,1));
    private final JButton submitButton;
    public SettingFrame(){
        setTitle("Settings");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new Box(1),BorderLayout.NORTH);

        JLabel volumeLabel = new JLabel("Music:");
        JSlider soundSlider = new JSlider(0,100,20);
        soundSlider.addChangeListener(e -> setVolume(soundSlider.getValue()));
        JPanel volumePanel = new JPanel(new FlowLayout());
        volumePanel.add(volumeLabel);
        volumePanel.add(soundSlider);

        submitButton = initButton("Confirm");
        submitButton.addActionListener(e -> this.dispose());

        formPanel.add(volumePanel);
        JPanel themePanel = getThemePanel();
        formPanel.add(themePanel);
        add(formPanel, BorderLayout.CENTER);
        add(submitButton,BorderLayout.SOUTH);
        setDarkMode();
        setVisible(true);
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
//        JRadioButton themeSystemRadioButton = new JRadioButton("System");
//        themeSystemRadioButton.addActionListener(e -> {
//            //todo:fix the bug here
//            isDarkMode=Integer.parseInt(Preferences.userRoot().get("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\\AppsUseLightTheme","0"))==0;
//            switchTheme();
//        });
        ButtonGroup themeButtonGroup = new ButtonGroup();
        themeButtonGroup.add(lightRadioButton);
        themeButtonGroup.add(darkRadioButton);
//        themeButtonGroup.add(themeSystemRadioButton);
        JPanel themePanel = new JPanel(new FlowLayout());
        themePanel.add(themeLabel);
        themePanel.add(lightRadioButton);
        themePanel.add(darkRadioButton);
        themePanel.add(pictureCheckBox);
//        themePanel.add(themeSystemRadioButton);
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
