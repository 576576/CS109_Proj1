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
    private JButton darkButton,loadButton,swayConfirmButton,nextStepButton,newGameButton,saveButton;
    private final JButton[] jButtons;
    FileDialog openDialog = new FileDialog(this, "Open File", FileDialog.LOAD);
    FileDialog saveDialog = new FileDialog(this, "Save File", FileDialog.SAVE);
    private JFileChooser jf = new JFileChooser(".\\");
    private FileNameExtensionFilter ff = new FileNameExtensionFilter("txt","txt");

    public ChessGameFrame(int wdt, int height) {
        setTitle("2023 CS109 Project Demo"); //设置标题
        this.wdt = wdt;
        this.hgt = height;
        this.ONE_CHESS_SIZE = (hgt * 4 / 5) / 9;

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
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
        addDialogLabel();
        jButtons = new JButton[]{darkButton,loadButton,swayConfirmButton,nextStepButton,newGameButton,saveButton};
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
        this.dialogLabel = new JLabel("dddd");
        dialogLabel.setLocation(16,hgt-80);
        dialogLabel.setSize(wdt-20, 60);
        dialogLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(dialogLabel);
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }
    private void addDarkModeButton() {
        JButton button = new JButton("Dark");
        button.addActionListener(e -> {
            isDarkMode=!isDarkMode;
            getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            button.setText(isDarkMode?"Day":"Dark");
            statusLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            dialogLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            dialogLabel.setText("Switch to "+(isDarkMode?"Dark":"Day")+" Mode");
            for (var i:jButtons){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            chessboardComponent.setDarkMode(isDarkMode);
            repaint();
        });
        button.setLocation(hgt, hgt / 10 + 120);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        darkButton=button;
        add(button);
    }
    private void addNewGameButton(){
        JButton button = new JButton("Start New");
        button.addActionListener((e) -> chessboardComponent.startNewGame());
        button.setLocation(hgt, hgt / 10 + 200);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        newGameButton=button;
        add(button);
    }

    private void addSwapConfirmButton() {
        JButton button = new JButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
        button.setLocation(hgt, hgt / 10 + 280);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        swayConfirmButton=button;
        add(button);
    }

    private void addNextStepButton() {
        JButton button = new JButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        button.setLocation(hgt, hgt / 10 + 360);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        nextStepButton=button;
        add(button);
    }

    private void addLoadButton() {
        JButton button = new JButton("Load");
        button.setLocation(hgt, hgt / 10 + 440);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);

        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.loadFromFile(file);
            }
        });
//        button.addActionListener(e -> {
//            openDialog.setVisible(true);
//            File file = openDialog.getFiles()[0];
//            System.out.println("Load from file: "
//                    + openDialog.getDirectory()
//                    + openDialog.getFile());
//            gameController.loadFromFile(file);
//        });
        loadButton=button;
    }
    private void addSaveButton(){
        JButton button = new JButton("Save");
        button.setLocation(hgt, hgt / 10 + 520);
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
//        button.addActionListener(e -> {
//            openDialog.setVisible(true);
//            File file = saveDialog.getFiles()[0];
//            System.out.println("Saved to file: "
//                    + saveDialog.getDirectory()
//                    + saveDialog.getFile());
//            gameController.saveToFile(file);
//        });
        saveButton=button;
    }

}
