package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
public class ChessGameFrame extends JFrame {
    //    public final Dimension FRAME_SIZE ;
    private final int wdt,hgt;
    private boolean isDarkMode=false;
    private final int ONE_CHESS_SIZE;

    private GameController gameController;

    private ChessboardComponent chessboardComponent;

    private JLabel statusLabel;
    private JPanel buttonsPanel = new JPanel(new GridLayout(7,1,2,10));
    private final ArrayList<JButton> jButtons = new ArrayList<>();
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int wdt, int height) {
        setTitle("CS109 消消乐"); //设置标题
        this.wdt = wdt;
        this.hgt = height;
        this.ONE_CHESS_SIZE = (hgt * 4 / 5) / 9;

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter ff = new FileNameExtensionFilter("txt", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        setSize(wdt, hgt);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //设置程序关闭按键，如果点击右上方的叉就游戏全部关闭了
        setLayout(null);

        initBasicComponents();
        initLocalPlayButtons();

        //darkButton.doClick();
    }
    private void initBasicComponents(){
        initChessboard();
        initLabel();
        initDarkModeButton();
        initNewGameButton();
        initSwapConfirmButton();
        initNextStepButton();
        initLoadButton();
        initSaveButton();
        initReturnTitleButton();
        initExitButton();
        buttonsPanel.setBounds(hgt, hgt / 10,200,500);
    }
    private void initLocalPlayButtons(){
        for (var button:jButtons) buttonsPanel.add(button);
        add(buttonsPanel);
    }
    public ChessboardComponent getChessboardComponent() {
        return chessboardComponent;
    }

    public void setChessboardComponent(ChessboardComponent chessboardComponent) {
        this.chessboardComponent = chessboardComponent;
    }

    public GameController getGameController() {
        return gameController;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * 在游戏面板中添加棋盘
     */
    private void initChessboard() {
        chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
        chessboardComponent.setSize(576,576);
        JPanel chessPanel = new JPanel(null);
        chessPanel.setBounds(wdt / 8, hgt / 10,576,576);
        chessPanel.add(chessboardComponent);
        add(chessPanel);
    }

    /**
     * 在游戏面板中添加标签
     */
    private void initLabel() {
        this.statusLabel = new JLabel("Difficulty:EASY  StepLeft:∞  Score:0/30");
        statusLabel.setLocation(wdt/8,20);
        statusLabel.setSize(wdt, 60);
        statusLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(statusLabel);
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    private void initDarkModeButton() {
        JButton button = new JButton("Dark");
        button.addActionListener(e -> {
            isDarkMode=!isDarkMode;
            getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            button.setText(isDarkMode?"Day":"Dark");
            statusLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            for (var i:jButtons){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            chessboardComponent.setDarkMode(isDarkMode);
            repaint();
        });
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);
    }
    private void initNewGameButton(){
        JButton button = new JButton("Start New");
        button.addActionListener((e) -> chessboardComponent.startNewGame());
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);
    }

    private void initSwapConfirmButton() {
        JButton button = new JButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);
    }

    private void initNextStepButton() {
        JButton button = new JButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);
    }

    private void initLoadButton() {
        JButton button = new JButton("Load");
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);

        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.loadFromFile(file);
            }
        });
    }
    private void initSaveButton(){
        JButton button = new JButton("Save");
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);

        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.saveToFile(file);
            }
        });
    }
    private void initReturnTitleButton(){
        JButton button = new JButton("ReturnTitle");
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);

        button.addActionListener(e -> {
            //TODO:return to Menu.java
        });
    }
    public void initExitButton(){
        JButton button = new JButton("Exit");
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        jButtons.add(button);
        button.addActionListener(e -> {
            System.exit(0);
        });
    }
}
