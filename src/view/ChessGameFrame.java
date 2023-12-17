package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * 这个类表示游戏过程中的整个游戏界面，是一切的载体
 */
public class ChessGameFrame extends JFrame {
    //    public final Dimension FRAME_SIZE ;
    private final int wdt;
    private final int hgt;
    private boolean isDarkMode=false;
    private final int ONE_CHESS_SIZE;

    private GameController gameController;

    private ChessboardComponent chessboardComponent;

    private JLabel statusLabel,dialogLabel;
    private JButton darkButton,loadButton,swayConfirmButton,nextStepButton,newGameButton,saveButton,netGameButton;
    private final JButton[] jButtons;
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

        addChessboard();
        addLabel();
        addDarkModeButton();
        addNewGameButton();
        addSwapConfirmButton();
        addNextStepButton();
        addLoadButton();
        addSaveButton();
        addNetGameButton();
        addDialogLabel();
        jButtons = new JButton[]{darkButton,loadButton,swayConfirmButton,nextStepButton,newGameButton,saveButton,netGameButton};
        darkButton.doClick();
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
    private void addChessboard() {
        chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
        chessboardComponent.setLocation(hgt / 5, hgt / 10);
        add(chessboardComponent);
    }

    /**
     * 在游戏面板中添加标签
     */
    private void addLabel() {
        this.statusLabel = new JLabel("Score:0");
        statusLabel.setLocation(hgt, wdt / 10);
        statusLabel.setSize(200, 60);
        statusLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(statusLabel);
    }
    private void addDialogLabel() {
        this.dialogLabel = new JLabel("Difficulty:EASY  StepLeft:∞  Goal:30");
        dialogLabel.setLocation(0,hgt-80);
        dialogLabel.setSize(wdt-20, 60);
        dialogLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(dialogLabel);
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getDialogLabel() {
        return dialogLabel;
    }

    private void addDarkModeButton() {
        JButton button = new JButton("Dark");
        button.addActionListener(e -> {
            isDarkMode=!isDarkMode;
            getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            button.setText(isDarkMode?"Day":"Dark");
            statusLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            dialogLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            for (var i:jButtons){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            chessboardComponent.setDarkMode(isDarkMode);
            repaint();
        });
        button.setLocation(hgt, hgt / 10 + 110);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        darkButton=button;
        add(button);
    }
    private void addNewGameButton(){
        JButton button = new JButton("Start New");
        button.addActionListener((e) -> chessboardComponent.startNewGame());
        button.setLocation(hgt, hgt / 10 + 180);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        newGameButton=button;
        add(button);
    }

    private void addSwapConfirmButton() {
        JButton button = new JButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
        button.setLocation(hgt, hgt / 10 + 250);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        swayConfirmButton=button;
        add(button);
    }

    private void addNextStepButton() {
        JButton button = new JButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        button.setLocation(hgt, hgt / 10 + 320);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        nextStepButton=button;
        add(button);
    }

    private void addLoadButton() {
        JButton button = new JButton("Load");
        button.setLocation(hgt, hgt / 10 + 390);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.loadGame(file);
            }
        });
        loadButton=button;
    }
    private void addSaveButton(){
        JButton button = new JButton("Save");
        button.setLocation(hgt, hgt / 10 + 460);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.saveToFile(file);
            }
        });
        saveButton=button;
    }
    private void addNetGameButton(){
        JButton button = new JButton("Net PVP");
        button.setLocation(hgt, hgt / 10 + 530);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
            //TODO:net functions gui
        });
        netGameButton=button;
    }

}
