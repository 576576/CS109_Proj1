package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
public class ChessGameFrame extends JFrame implements MyFrame{
    //    public final Dimension FRAME_SIZE ;
    private final int wdt,hgt;
    private final int ONE_CHESS_SIZE,CHESSBOARD_SIZE;

    private GameController gameController;
    private MenuFrame menuFrame;
    private final boolean isOnlinePlay;

    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,nextStepButton;

    private JLabel statusLabel,statusLabel2,difficultyLabel,timeLimitLabel;
    private final JPanel controlPanel = new JPanel(new GridLayout(11,1,2,6));
    private final JPanel statusPanel = new JPanel(new GridLayout(16,1));
    private final JPanel chessPanel = new JPanel(new BorderLayout());
    private final JPanel panelUp = new JPanel();
    private final ArrayList<JComponent> controlComponents = new ArrayList<>();
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int width, int height,boolean isOnlinePlay) {
        setTitle("CS109 消消乐"); //设置标题
        this.wdt = width;
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
        setLayout(new BorderLayout(6,18));

        initBasicComponents();
        if (!isOnlinePlay) initLocalPlayPanel();
        else initOnlinePlayPanel();
        add(controlPanel,BorderLayout.EAST);

    }
    private void initBasicComponents(){
        initChessboard();
        initStatusLabels();
        initDarkModeButton();
        initAutoConfirmButton();
        add(panelUp,BorderLayout.NORTH);
        controlPanel.setBounds(2*wdt/3, hgt / 10,wdt/6,4*hgt/5);
    }
    private void initLocalPlayPanel(){
        initHintButton();
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
        initSwapConfirmButton();
        initNextStepButton();
        initReturnTitleButton();
        initExitButton();
        for (var component: controlComponents) controlPanel.add(component);
    }
    public ChessboardComponent getChessboardComponent() {
        return chessboardComponent;
    }

    public void setChessboardComponent(ChessboardComponent chessboardComponent) {
        this.chessboardComponent = chessboardComponent;
    }

    public void setMenuFrame(MenuFrame menuFrame) {
        this.menuFrame = menuFrame;
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
        chessPanel.add(chessboardComponent,BorderLayout.CENTER);
        add(chessPanel,BorderLayout.CENTER);
    }

    /**
     * 在游戏面板中添加标签
     */
    private void initStatusLabels() {
        statusLabel = MyFrame.initLabel("Score:0/30");
        statusLabel2 = MyFrame.initLabel("StepLeft:∞");
        difficultyLabel = MyFrame.initLabel("Difficulty:EASY");
        timeLimitLabel = MyFrame.initLabel("TimeLimit:∞");
        statusPanel.add(statusLabel);
        statusPanel.add(statusLabel2);
        statusPanel.add(difficultyLabel);
        statusPanel.add(timeLimitLabel);
        add(statusPanel,BorderLayout.WEST);
    }

    public JLabel getStatusLabel() {
        return getStatusLabel(0);
    }
    public JLabel getStatusLabel(int i){
        if (i>statusPanel.getComponents().length) return null;
        return (JLabel) statusPanel.getComponents()[i];
    }
    public JLabel getDifficultyLabel() {
        return difficultyLabel;
    }

    private void initDarkModeButton() {
        JButton button = initButton("SetDarkMode");
        button.addActionListener(e -> setDarkMode());
    }

    public void setDarkMode() {
        boolean isDarkMode=menuFrame.setDarkMode();
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        chessboardComponent.setDarkMode(isDarkMode);
        for (var i:statusPanel.getComponents()){
            i.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        }
        for (var i: controlComponents){
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
    public void initHintButton(){
        JButton button = initButton("Hint!");
        button.addActionListener(e -> gameController.hint());
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
    public JButton initButton(String name){
        JButton button = MyFrame.initButton(name);
        controlComponents.add(button);
        return button;
    }

}
