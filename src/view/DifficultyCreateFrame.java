package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static view.SettingFrame.setDarkModeStatic;

public class DifficultyCreateFrame extends MyFrame{
    public DifficultyCreateFrame() {
        setTitle("Create your difficulty");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

//        // 设置任务栏图标
//        Image taskbarIcon = Toolkit.getDefaultToolkit().getImage("E:\\develop\\cursor\\java\\icon.png");
//        setIconImage(taskbarIcon);

        // 创建个人信息填写表单
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;

        JLabel nameLabel = new JLabel("Name：");
        JTextField nameTextField = new JTextField(20);

        JLabel goalLabel = new JLabel("Goal：");
        JTextField goalTextField = new JTextField("30",20);

        JLabel stepLabel = new JLabel("Step Limit：");
        JTextField stepTextField = new JTextField("-1",20);

        JLabel timeLabel = new JLabel("Time Limit：");
        JTextField timeTextField = new JTextField("-1",20);

        JLabel genderLabel = new JLabel("性别：");
        JRadioButton maleRadioButton = new JRadioButton("男");
        JRadioButton femaleRadioButton = new JRadioButton("女");
        ButtonGroup genderButtonGroup = new ButtonGroup();
        genderButtonGroup.add(maleRadioButton);
        genderButtonGroup.add(femaleRadioButton);

        JLabel hobbyLabel = new JLabel("爱好：");
        JCheckBox javaCheckBox = new JCheckBox("Java");
        JCheckBox pythonCheckBox = new JCheckBox("Python");
        JCheckBox cSharpCheckBox = new JCheckBox("C#");

        JLabel introLabel = new JLabel("简介：");
        JTextArea introTextArea = new JTextArea(5, 20);

        JLabel dropdownLabel = new JLabel("下拉：");
        String[] dropdownOptions = {"不内卷", "规避竞争的最好方法是避免竞争", "养生上班才好"};
        JComboBox<String> dropdownComboBox = new JComboBox<>(dropdownOptions);

        JButton submitButton = new JButton("提交");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitForm(nameTextField.getText(), maleRadioButton.isSelected(),
                        javaCheckBox.isSelected(), pythonCheckBox.isSelected(),
                        cSharpCheckBox.isSelected(), introTextArea.getText(),
                        dropdownComboBox.getSelectedItem().toString());
            }
        });
        //设置布局
        constraints.gridx = 0;//设置x坐标
        constraints.gridy = 0;//设置y坐标
        formPanel.add(nameLabel, constraints);//添加组件

        constraints.gridx = 1;
        constraints.gridwidth = 3;//设置宽度
        formPanel.add(nameTextField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        formPanel.add(genderLabel, constraints);

        constraints.gridx = 1;
        formPanel.add(maleRadioButton, constraints);

        constraints.gridx = 2;
        formPanel.add(femaleRadioButton, constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        formPanel.add(hobbyLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 1;
        formPanel.add(javaCheckBox, constraints);

        constraints.gridx = 2;
        constraints.gridwidth = 1;
        formPanel.add(pythonCheckBox, constraints);

        constraints.gridx = 3;
        constraints.gridwidth = 1;
        formPanel.add(cSharpCheckBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        formPanel.add(introLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        formPanel.add(introTextArea, constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        formPanel.add(dropdownLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        formPanel.add(dropdownComboBox, constraints);

        constraints.gridx = 5;
        constraints.gridy = 6;
        constraints.gridwidth = 4;
        formPanel.add(submitButton, constraints);

        add(formPanel, BorderLayout.CENTER);

        setVisible(true);
    }
    //弹出表单信息
    private void submitForm(String name, boolean isMale, boolean hasJavaHobby,
                            boolean hasPythonHobby, boolean hasCSharpHobby,
                            String intro, String selectedOption) {
        JOptionPane.showMessageDialog(this, "姓名：" + name + "\n"
                + "性别：" + (isMale ? "男" : "女") + "\n"
                + "爱好：" + (hasJavaHobby ? "Java " : "") + "\n"
                + "      " + (hasPythonHobby ? "Python " : "") + "\n"
                + "      " + (hasCSharpHobby ? "C#" : "") + "\n"
                + "个人简介：" + intro + "\n"
                + "下拉框选项：" + selectedOption, "个人基本信息", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void setDarkMode() {
        setDarkModeStatic(getRootPane());
    }
}
