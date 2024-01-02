package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import static view.MenuFrame.*;
import static view.DifficultySelectFrame.selectedFile;

public class ChessGameFrame extends JFrame implements MyFrame{
    private final int ONE_CHESS_SIZE;
    public static boolean isGameFrameInitDone =false;
    private GameController gameController;
    public MenuFrame menuFrame;
    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,nextStepButton;
    private final JPanel controlPanelRight = new JPanel();
    private final JPanel panelLeft = new JPanel();
    private final JLabel[] statusLabels = new JLabel[4];
    private final GridBagLayout gbl = new GridBagLayout();
    private final ArrayList<JComponent> controlComponents = new ArrayList<>();
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int width, int height) {
        setTitle("CS109 消消乐");
        int CHESSBOARD_SIZE = (int) (3 * Math.sqrt(width * height) / 5);
        this.ONE_CHESS_SIZE = CHESSBOARD_SIZE /8;
        setMinimumSize(new Dimension(905,600));

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter ff = new FileNameExtensionFilter("savedGame.txt", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(gbl);

        System.out.println("Play Start: "+startPlayMode);

        initChessboard();
        initStatusLabels();
        if (!isOnlinePlay()) initLocalPlayPanel();
        else initOnlinePlayPanel();

        MyFrame.addComponent(this,gbl, panelLeft,1,1,24,24,0,1);
        MyFrame.addComponent(this,gbl, controlPanelRight,590,1,560,4,0,1);
        setDarkMode();
        new Thread(()->{ //initialize the gui thread
            for (;;){
                if (isGameFrameInitDone){
                    gameController.updateDifficultyLabel();
                    gameController.updateScoreAndStepLabel();
                    gameController.startTimer();
                    break;
                }
                else System.out.print("");
            }
        }).start();

        musicThread = new Thread(() -> {
            int i=0;
            while (true) {
                var f = musicFiles.get(i);
                i++;
                i%=musicFiles.size();
                musicPlayer.play(f);
            }
        });
        setDarkMode();
        SwingUtilities.invokeLater(()->{
            while (isOnlinePlay()) {
                System.out.println("OnlineGame: start");
                if (isGameFrameInitDone) {
                    if (startPlayMode == 3) gameController.onPlayerHostGame();
                    if (startPlayMode == 4) gameController.onPlayerJoinGame();
                    break;
                }
                System.out.print("");
            }
            if (startPlayMode==2){
                for (;;){
                    if (isGameFrameInitDone){
                        gameController.loadFromFile(selectedFile);
                        break;
                    }
                }
            }
        });
        musicThread.start();
    }
    private void initLocalPlayPanel(){
        panelLeft.setLayout(new GridLayout(8,1,2,6));
        initThemeButton();
        initHintButton();
        initAutoGoButton();
        initShuffleButton();

        controlPanelRight.setLayout(new GridLayout(9,1,2,6));
        initSettingButton();
        initNewGameButton();
        initAutoConfirmButton();
        initSwapConfirmButton();
        initNextStepButton();
        initLoadButton();
        initSaveButton();
        initReturnTitleButton();
        initExitButton();
    }
    private void initOnlinePlayPanel(){
        panelLeft.setLayout(new GridLayout(6,1,2,6));
        initThemeButton();
        initShuffleButton();

        controlPanelRight.setLayout(new GridLayout(6,1,2,6));
        initSettingButton();
        initAutoConfirmButton();
        initSwapConfirmButton();
        initNextStepButton();
        initReturnTitleButton();
        initExitButton();
    }
    public ChessboardComponent getChessboardComponent() {
        return chessboardComponent;
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
        MyFrame.addComponent(this,gbl,chessboardComponent,25,1,560,560,560,560);
    }

    /**
     * 在游戏面板中添加标签面板
     */
    private void initStatusLabels() {
        statusLabels[0] = MyFrame.initLabel("Difficulty:"+difficulty.getName());
        statusLabels[1] = MyFrame.initLabel("Score:0/"+difficulty.getGoal());
        statusLabels[2] = MyFrame.initLabel("StepLeft:"+(difficulty.getStepLimit()!=-1?difficulty.getStepLimit():"∞"));
        statusLabels[3] = MyFrame.initLabel("TimeLimit:"+(difficulty.getTimeLimit()!=-1?difficulty.getTimeLimit():"∞"));
        for (JLabel statusLabel : statusLabels) statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        for (var i:statusLabels) panelLeft.add(i);
    }
    public JLabel[] getStatusLabels(){
        if (statusLabels==null) initStatusLabels();
        return statusLabels;
    }
    public void setDarkMode() {
        try {
            getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            chessboardComponent.setDarkMode(isDarkMode);
            for (var i:getContentPane().getComponents()){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            }
            for (var i:panelLeft.getComponents()){
                i.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            }
            for (var i: controlComponents){
                i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            chessboardComponent.setDarkMode(isDarkMode);
        } catch (Exception ignored){}
        repaint();
    }

    private void initThemeButton() {
        JButton button = initButton("Switch Theme");
        button.addActionListener(e -> switchTheme());
        panelLeft.add(button);
    }
    private void switchTheme(){
        isDarkMode=!isDarkMode;
        MenuFrame.switchTheme();
    }

    private void initSettingButton(){
        JButton button = initButton("Setting");
        button.addActionListener(e -> {
            SettingFrame settingFrame = new SettingFrame();
            settingFrame.setVisible(true);
        });
        controlPanelRight.add(button);
    }
    private void initNewGameButton(){
        JButton button = initButton("Start New");
        button.addActionListener(e -> chessboardComponent.startNewGame());
        controlPanelRight.add(button);
    }
    private void initAutoConfirmButton(){
        JButton button = initButton("Auto Confirm");
        button.addActionListener(e -> {
            boolean isAutoConfirm = !gameController.isAutoConfirm();
            getGameController().setAutoConfirm(isAutoConfirm);
            button.setText(isAutoConfirm?"Auto Confirm":"Not Auto");
            swapConfirmButton.setVisible(!isAutoConfirm);
            nextStepButton.setVisible(!isAutoConfirm);
        });
        controlPanelRight.add(button);
    }
    private void initAutoGoButton(){
        JButton button = initButton("AutoGo");
        button.addActionListener(e -> {
            boolean isAutoMode = !getGameController().isAutoMode();
            button.setText(isAutoMode?"FullAuto":"AutoGo");
            getGameController().setAutoMode(isAutoMode);
        });
        panelLeft.add(button);
    }
    public void initHintButton(){
        JButton button = initButton("Hint!");
        button.addActionListener(e -> gameController.hint());
        panelLeft.add(button);
    }
    public void initShuffleButton(){
        JButton button = initButton("Shuffle");
        button.addActionListener(e -> gameController.onPlayerShuffle());
        panelLeft.add(button);
    }
    private void initSwapConfirmButton() {
        JButton button = initButton("Confirm Swap");
        button.addActionListener((e) -> chessboardComponent.swapChess());
        swapConfirmButton=button;
        controlPanelRight.add(button);
    }

    private void initNextStepButton() {
        JButton button = initButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        nextStepButton=button;
        controlPanelRight.add(button);
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
        controlPanelRight.add(button);
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
        controlPanelRight.add(button);
    }
    private void initReturnTitleButton(){
        JButton button = initButton(isOnlinePlay()?"Disconnect":"Return Title");
        button.addActionListener(e -> returnToTitle());
        controlPanelRight.add(button);
    }

    public void returnToTitle() {
        gameController.terminate();
        menuFrame.setState(Frame.NORMAL);
        musicPlayer.close();
        dispose();
    }

    public void initExitButton(){
        JButton button = initButton("Exit");
        button.addActionListener(e -> System.exit(0));
        controlPanelRight.add(button);
    }
    public JButton initButton(String name){
        JButton button = MyFrame.initButton(name);
        controlComponents.add(button);
        return button;
    }
    public static boolean isOnlinePlay(){
        return startPlayMode>2;
    }
}
