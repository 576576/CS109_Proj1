package view;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static view.MenuFrame.isDarkMode;
import static view.MenuFrame.switchTheme;

public class SettingFrame extends JFrame implements MyFrame {
    public SettingFrame(){
        setTitle("Settings");
        setSize(400, 500);
        setLocationRelativeTo(null);

        JPanel formPanel = new JPanel(new GridLayout(3,1));

        JLabel volumeLabel = new JLabel("Music:");
        JSlider soundSlider = new JSlider(0,100,20);
        soundSlider.addChangeListener(e -> MenuFrame.musicVolume =soundSlider.getValue());
        JPanel volumePanel = new JPanel(new FlowLayout());
        volumePanel.add(volumeLabel);
        volumePanel.add(soundSlider);

        JPanel themePanel = getjPanel();

        JButton submitButton = MyFrame.initButton("Confirm");
        submitButton.addActionListener(e -> this.dispose());

        formPanel.add(volumePanel);
        formPanel.add(themePanel);
        add(formPanel, BorderLayout.CENTER);
        add(submitButton,BorderLayout.SOUTH);

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
            isDarkMode=Integer.parseInt(Preferences.userRoot().get("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\\AppsUseLightTheme",null))==0;
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
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        for (var i : getComponents()) {
            if (i instanceof JPanel) {
                for (var j : ((JPanel) i).getComponents()) {
                    j.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                    j.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                }
            }
            if (i instanceof JButton){
                i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
        }
    }
}
