package view;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static view.MenuFrame.isDarkMode;
import static view.MenuFrame.musicVolume;

public class SettingFrame extends JFrame implements MyFrame {
    public SettingFrame(MenuFrame menuFrame){
        setTitle("Settings");
        setSize(400, 500);
        setLocationRelativeTo(null);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("Music:");
        JSlider soundSlider = new JSlider(0,100,20);
        soundSlider.addChangeListener(e -> {
            MenuFrame.musicVolume =soundSlider.getValue();
        });

        JLabel themeLabel = new JLabel("Theme:");
        JRadioButton lightRadioButton = new JRadioButton("Light");
        JRadioButton darkRadioButton = new JRadioButton("Dark");
        JRadioButton themeSystemRadioButton = new JRadioButton("System");
        ButtonGroup genderButtonGroup = new ButtonGroup();
        genderButtonGroup.add(lightRadioButton);
        genderButtonGroup.add(darkRadioButton);
        genderButtonGroup.add(themeSystemRadioButton);
        ;

        JButton submitButton = MyFrame.initButton("Confirm");
        submitButton.addActionListener(e -> {

        });

        //设置布局
        constraints.gridx = 0;
        constraints.gridy = 0;
        formPanel.add(nameLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        constraints.gridheight = 6;
        formPanel.add(soundSlider, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        formPanel.add(themeLabel, constraints);

        constraints.gridx = 1;
        formPanel.add(lightRadioButton, constraints);

        constraints.gridx = 2;
        formPanel.add(darkRadioButton, constraints);

        constraints.gridx = 3;
        formPanel.add(themeSystemRadioButton,constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridheight = 8;
        formPanel.add(submitButton, constraints);

        add(formPanel, BorderLayout.CENTER);

        setVisible(true);
    }
    private void saveConfigure(int themeMode,boolean isMute,int volume){
        if (themeMode==0){
            isDarkMode=Integer.parseInt(Preferences.userRoot().get("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\\AppsUseLightTheme",null))==0;
        }
        else isDarkMode=themeMode!=1;
        musicVolume=volume;
    }
}
