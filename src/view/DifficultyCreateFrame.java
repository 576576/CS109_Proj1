package view;

import model.Difficulty;
import model.DifficultyPreset;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

import static view.MenuFrame.difficulty;

public class DifficultyCreateFrame extends MyFrame{
    private final JPanel formPanel = new JPanel(new GridLayout(3,1));
    private final JButton submitButton;
    private JTextField goalInputField,stepInputField,timeInputField;
    private String goal=DifficultyPreset.EASY.goal+"",timeLimit=DifficultyPreset.EASY.timeLimit+"",stepLimit=DifficultyPreset.EASY.stepLimit+"";
    public DifficultyCreateFrame(){
        setTitle("Create a difficulty");
        setSize(500, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new Box(1),BorderLayout.NORTH);

        submitButton = initButton("Confirm");
        submitButton.addActionListener(e -> {
            goal=goalInputField.getText();
            stepLimit=stepInputField.getText();
            timeLimit=timeInputField.getText();
            if (isLegalInput(goal,1)&&isLegalInput(stepLimit,-1)&&isLegalInput(timeLimit,-1)){
                difficulty=new Difficulty(Integer.parseInt(goal),Integer.parseInt(stepLimit),Integer.parseInt(timeLimit));
                System.out.println("Difficulty Selected: "+difficulty.getName());
                dispose();
                return;
            }
            if (!isLegalInput(goal,1)) System.out.println("goal illegal");
            if (!isLegalInput(stepLimit,1)) System.out.println("step illegal");
            if (!isLegalInput(timeLimit,1)) System.out.println("time illegal");
            JOptionPane.showMessageDialog(this,"Illegal difficulty,please check again!");
        });

        formPanel.add(getGoalPanel());
        formPanel.add(getStepPanel());
        formPanel.add(getTimeLimitPanel());
        add(formPanel, BorderLayout.CENTER);
        add(submitButton,BorderLayout.SOUTH);
        setDarkMode();
        setVisible(true);
    }

    private JPanel getStepPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel topicLabel = initLabel("Steps:");
        stepInputField = new JTextField(stepLimit,8);
        stepInputField.setFont(new Font("Rockwell", Font.PLAIN, 20));
        inputPanel.add(topicLabel);
        inputPanel.add(stepInputField);
        return inputPanel;
    }

    private JPanel getGoalPanel(){
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel topicLabel = initLabel("Goal:");
        goalInputField = new JTextField(goal,8);
        goalInputField.setFont(new Font("Rockwell", Font.PLAIN, 20));
        inputPanel.add(topicLabel);
        inputPanel.add(goalInputField);
        return inputPanel;
    }

    private JPanel getTimeLimitPanel() {
        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel topicLabel = initLabel("Time:");
        timeInputField = new JTextField(timeLimit,8);
        timeInputField.setFont(new Font("Rockwell", Font.PLAIN, 20));
        inputPanel.add(topicLabel);
        inputPanel.add(timeInputField);
        return inputPanel;
    }
    private boolean isLegalInput(String str, int floor){
        try {
            return Pattern.compile("^[-+]?\\d*$").matcher(str).matches() && Integer.parseInt(str) >= floor;
        } catch (NumberFormatException e) {
            return false;
        }
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
