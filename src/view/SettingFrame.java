package view;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static view.MenuFrame.*;

public class SettingFrame extends MyFrame{
    JPanel formPanel = new JPanel(new GridLayout(3,1));
    JPanel volumePanel = new JPanel(new FlowLayout());
    JPanel themePanel = getjPanel();
    JButton submitButton;
    public SettingFrame(){
        setTitle("Settings");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new Box(1),BorderLayout.NORTH);

        JLabel volumeLabel = new JLabel("Music:");
        JSlider soundSlider = new JSlider(0,100,20);
        soundSlider.addChangeListener(e -> setVolume(soundSlider.getValue()));
        volumePanel.add(volumeLabel);
        volumePanel.add(soundSlider);

        submitButton = MyFrame.initButton("Confirm");
        submitButton.addActionListener(e -> this.dispose());

        formPanel.add(volumePanel);
        formPanel.add(themePanel);
        add(formPanel, BorderLayout.CENTER);
        add(submitButton,BorderLayout.SOUTH);
        setDarkMode();
        setVisible(true);
    }

    private JPanel getjPanel() {
        JLabel themeLabel = new JLabel("Theme:");
        JRadioButton lightRadioButton = new JRadioButton("Light");
        lightRadioButton.setSelected(!isDarkMode);
        lightRadioButton.addActionListener(e -> {
            isDarkMode=false;
            switchTheme();
        });
        JRadioButton darkRadioButton = new JRadioButton("Dark");
        lightRadioButton.setSelected(isDarkMode);
        darkRadioButton.addActionListener(e -> {
            isDarkMode=true;
            switchTheme();
        });
        JRadioButton themeSystemRadioButton = new JRadioButton("System");
        themeSystemRadioButton.addActionListener(e -> {
            //todo:fix the bug here
            isDarkMode=Integer.parseInt(Preferences.userRoot().get("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\\AppsUseLightTheme","0"))==0;
            switchTheme();
        });
        ButtonGroup themeButtonGroup = new ButtonGroup();
        themeButtonGroup.add(lightRadioButton);
        themeButtonGroup.add(darkRadioButton);
        themeButtonGroup.add(themeSystemRadioButton);
        JPanel themePanel = new JPanel(new FlowLayout());
        themePanel.add(themeLabel);
        themePanel.add(lightRadioButton);
        themePanel.add(darkRadioButton);
        themePanel.add(themeSystemRadioButton);
        return themePanel;
    }

    public void setDarkMode(){
        setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        setDarkModeStatic(formPanel);
        submitButton.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        submitButton.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        this.repaint();
    }
    public static <T extends JComponent> void setDarkModeStatic(T component){
        for (var i:component.getComponents()){
            if (!(i instanceof JComponent)) continue;
            if (i instanceof JPanel){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                setDarkModeStatic((JComponent) i);
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
