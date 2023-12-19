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
    private final boolean isOnlinePlay;

    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,NextStepButton;

    private JLabel statusLabel,difficultyLabel;
    private final JPanel controlPanel = new JPanel(new GridLayout(9,1,2,10));
    private final JPanel chessPanel = new JPanel(null);
    private final ArrayList<JComponent> componentsInControlPanel = new ArrayList<>();
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int wdt, int height,boolean isOnlinePlay) {
        setTitle("CS109 消消乐"); //设置标题
        this.wdt = wdt;
        this.hgt = height;
        this.isOnlinePlay=isOnlinePlay;
        this.ONE_CHESS_SIZE = (hgt * 4 / 5) / 9;

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter ff = new FileNameExtensionFilter("txt", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        setSize(wdt, hgt);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(null);

        initBasicComponents();
        if (!isOnlinePlay) initLocalPlayPanel();
        else initOnlinePlayPanel();
        add(controlPanel);

    }
    private void initBasicComponents(){
        initChessboard();
        initLabel();
        initDifficultyLabel();
        initDarkModeButton();
        initAutoConfirmButton();
        controlPanel.setBounds(wdt-350, hgt / 6,200,600);
    }
    private void initLocalPlayPanel(){
        initNewGameButton();
        initSwapConfirmButton();
        initNextStepButton();
        initLoadButton();
        initSaveButton();
        initReturnTitleButton();
        initExitButton();
        for (var button: componentsInControlPanel) controlPanel.add(button);
    }
    private void initOnlinePlayPanel(){
        initNewGameButton();
        initReturnTitleButton();
        initExitButton();
        for (var button: componentsInControlPanel) controlPanel.add(button);
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
        chessPanel.setBounds(wdt / 8, hgt / 7,576,576);
        chessPanel.add(chessboardComponent);
        add(chessPanel);
    }

    /**
     * 在游戏面板中添加标签
     */
    private void initLabel() {
        this.statusLabel = new JLabel("StepLeft:∞  Score:0/30");
        statusLabel.setLocation(wdt-350, hgt/12);
        statusLabel.setSize(wdt/2, 60);
        statusLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(statusLabel);
    }
    private void initDifficultyLabel(){
        this.difficultyLabel = new JLabel("Difficulty:EASY  TimeLimit:∞");
        difficultyLabel.setLocation(wdt/8,hgt/12);
        difficultyLabel.setSize(wdt/3, 60);
        difficultyLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(difficultyLabel);
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JLabel getDifficultyLabel() {
        return difficultyLabel;
    }

    private void initDarkModeButton() {
        JButton button = initButton("SetDarkMode");
        button.addActionListener(e -> {
            isDarkMode=!isDarkMode;
            setDarkMode(isDarkMode);
        });
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        statusLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        difficultyLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        chessPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        controlPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        for (var i: componentsInControlPanel){
            i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
            i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        }
        chessboardComponent.setDarkMode(isDarkMode);
        repaint();
    }

    private void initNewGameButton(){
        JButton button = initButton("Start New");
        button.addActionListener((e) -> chessboardComponent.startNewGame());
    }
    private void initAutoConfirmButton(){
        JButton button = initButton("Manual");
        button.addActionListener(e -> {
            boolean isAutoConfirm = !getGameController().isAutoConfirm();
            getGameController().setAutoConfirm(isAutoConfirm);
            button.setText(isAutoConfirm?"Auto":"Manual");

        });
    }

    private void initSwapConfirmButton() {
        JButton button = initButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
    }

    private void initNextStepButton() {
        JButton button = initButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
    }
    private void initLoadButton() {
        JButton button = initButton("Load");
        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.loadFromFile(file);
            }
        });
    }
    private void initSaveButton(){
        JButton button = initButton("Save");
        button.addActionListener(e -> {
            int result = jf.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = jf.getSelectedFile();
                gameController.saveToFile(file);
            }
        });
    }
    private void initReturnTitleButton(){
        JButton button = initButton(isOnlinePlay?"Disconnect":"Return Title");
        button.addActionListener(e -> {
            gameController.terminate();
            dispose();
        });
    }
    public void initExitButton(){
        JButton button = initButton("Exit");
        button.addActionListener(e -> System.exit(0));
    }
    private JButton initButton(String name){
        JButton button = new JButton(name);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        button.setForeground(Color.BLACK);
        button.setBackground(Color.LIGHT_GRAY);
        componentsInControlPanel.add(button);
        return button;
    }
}
