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
    private final int ONE_CHESS_SIZE,CHESSBOARD_SIZE;

    private GameController gameController;
    private final boolean isOnlinePlay;

    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,nextStepButton;

    private JLabel statusLabel,statusLabel2,difficultyLabel;
    private final JPanel controlPanel = new JPanel(new GridLayout(11,1,2,6));
    private final JPanel statusPanel = new JPanel(new GridLayout(2,1));
    private final JPanel chessPanel = new JPanel(new GridLayout(1,1));
    private final ArrayList<JComponent> controlComponents = new ArrayList<>();
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int wdt, int height,boolean isOnlinePlay) {
        setTitle("CS109 消消乐"); //设置标题
        this.wdt = wdt;
        this.hgt = height;
        this.isOnlinePlay=isOnlinePlay;
        this.CHESSBOARD_SIZE = (int)(3*Math.sqrt(wdt*hgt)/5);
        this.ONE_CHESS_SIZE = CHESSBOARD_SIZE/8;

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
        initStatusLabels();
        initDifficultyLabel();
        initDarkModeButton();
        initAutoConfirmButton();
        controlPanel.setBounds(2*wdt/3, hgt / 10,wdt/6,4*hgt/5);
    }
    private void initLocalPlayPanel(){
        initNewGameButton();
        initSwapConfirmButton();
        initNextStepButton();
        initLoadButton();
        initSaveButton();
        initReturnTitleButton();
        initExitButton();
        for (var component: controlComponents) controlPanel.add(component);
    }
    private void initOnlinePlayPanel(){
        initNewGameButton();
        initReturnTitleButton();
        initExitButton();
        for (var button: controlComponents) controlPanel.add(button);
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
        chessPanel.setBounds(wdt / 8, hgt / 7,CHESSBOARD_SIZE,CHESSBOARD_SIZE);
        chessPanel.add(chessboardComponent);
        add(chessPanel);
    }

    /**
     * 在游戏面板中添加标签
     */
    private void initStatusLabels() {
        statusLabel = initLabel("Score:0/30");
        statusLabel2 = initLabel("StepLeft:∞");
        statusPanel.add(statusLabel);
        statusPanel.add(statusLabel2);
        controlComponents.add(statusPanel);
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
    public JLabel getStatusLabel2() {
        return statusLabel2;
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
        statusLabel2.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        difficultyLabel.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
        chessPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        statusPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        controlPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        for (var i: controlComponents){
            if (i.getClass()!=JButton.class) continue;
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
            swapConfirmButton.setVisible(!isAutoConfirm);
            nextStepButton.setVisible(!isAutoConfirm);
        });
    }

    private void initSwapConfirmButton() {
        JButton button = initButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
        swapConfirmButton=button;
    }

    private void initNextStepButton() {
        JButton button = initButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        nextStepButton=button;
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
        controlComponents.add(button);
        return button;
    }
    private JLabel initLabel(String name){
        JLabel label = new JLabel(name);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        label.setForeground(Color.BLACK);
        return label;
    }
}
