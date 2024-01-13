package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Random;

import static controller.GameController.pauseMilliSeconds;
import static view.DifficultySelectFrame.selectedFile;
import static view.MenuFrame.*;

public class ChessGameFrame extends MyFrame{
    private final int ONE_CHESS_SIZE;
    public static boolean isGameFrameInitDone =false;
    private GameController gameController;
    public MenuFrame menuFrame;
    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,nextStepButton;
    private final JPanel panelRight = new JPanel();
    private final JPanel panelLeft = new JPanel();
    private final JLabel[] statusLabels = new JLabel[4];
    private final GridBagLayout gbl = new GridBagLayout();
    private final JPanel playPanel = new JPanel(gbl);
    private final JPanel backgroundPanel = new JPanel(gbl){
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isImageBackground) g.drawImage(currentBackground.getImage(), 0, 0, null);
        }
    };
    private final JFileChooser jf = new JFileChooser(".\\");

    public ChessGameFrame(int width, int height) {
        setTitle("CS109 消消乐");
        int CHESSBOARD_SIZE = (int) (3 * Math.sqrt(width * height) / 5);
        ONE_CHESS_SIZE = CHESSBOARD_SIZE /8;
        setMinimumSize(new Dimension(905,600));

        jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter ff = new FileNameExtensionFilter("SavedGameFiles", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(null);

        playPanel.setSize(getSize());
        backgroundPanel.setSize(getSize());
        playPanel.setOpaque(false);
        backgroundPanel.setOpaque(false);
        add(playPanel);
        add(backgroundPanel);

        System.out.println("Play Start: "+startPlayMode);

        initChessboard();
        initStatusLabels();
        if (!isOnlinePlay()) initLocalPlayPanel();
        else initOnlinePlayPanel();

        addComponent(playPanel,gbl, panelLeft,1,1,24,24,0,1);
        addComponent(playPanel,gbl, panelRight,590,1,560,4,0,1);

        musicThread = new Thread(() -> {
            int i=new Random().nextInt(musicFiles.size());
            while (gameController.isAlive()) {
                var f = musicFiles.get(i);
                i++;
                i%=musicFiles.size();
                musicPlayer.play(f);
            }
        });

        SwingUtilities.invokeLater(()->{ //initialize game functions in order
            while (isOnlinePlay()) {
                System.out.println("OnlineGame: start");
                if (isGameFrameInitDone) {
                    if (startPlayMode == 3) gameController.onPlayerHostGame();
                    if (startPlayMode == 4) gameController.onPlayerJoinGame();
                    uiInitialize();
                    break;
                }
                System.out.print("");
            }
            if (startPlayMode==2){
                for (;;){
                    if (isGameFrameInitDone){
                        gameController.loadFromFile(selectedFile);
                        uiInitialize();
                        break;
                    }
                }
            }
            if (startPlayMode==1){
                for (;;){
                    if (isGameFrameInitDone){
                        uiInitialize();
                        break;
                    }
                }
            }
        });

        //add listeners to adapt window change
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                playPanel.setSize(getSize());
                scaleBackgroundImage(getSize());
                backgroundPanel.setSize(getSize());
            }
        });
        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                super.windowStateChanged(e);
                playPanel.setSize(getSize());
                scaleBackgroundImage(getSize());
                backgroundPanel.setSize(getSize());
            }
        });
    }
    private void uiInitialize(){ //initialize the gui threads
        panelLeft.setOpaque(false);
        panelRight.setOpaque(false);
        gameController.updateDifficultyLabel();
        gameController.updateScoreAndStepLabel();
        gameController.startTimer();
        musicThread.start();
        setDarkMode();
    }
    private void initLocalPlayPanel(){
        panelLeft.setLayout(new GridLayout(8,1,2,6));
        initThemeButton();
        initHintButton();
        initAutoGoButton();
        initShuffleButton();

        panelRight.setLayout(new GridLayout(9,1,2,6));
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

        panelRight.setLayout(new GridLayout(6,1,2,6));
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
        addComponent(playPanel,gbl,chessboardComponent,25,1,560,560,560,560);
    }

    /**
     * 在游戏面板中添加标签面板
     */
    private void initStatusLabels() {
        statusLabels[0] = initLabel("Difficulty:"+difficulty.getName());
        statusLabels[1] = initLabel("Score:0/"+difficulty.getGoal());
        statusLabels[2] = initLabel("StepLeft:"+(difficulty.getStepLimit()!=-1?difficulty.getStepLimit():"∞"));
        statusLabels[3] = initLabel("TimeLimit:"+(difficulty.getTimeLimit()!=-1?difficulty.getTimeLimit():"∞"));
        for (JLabel statusLabel : statusLabels) statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        for (var i:statusLabels) panelLeft.add(i);
    }
    public JLabel[] getStatusLabels(){
        if (statusLabels==null) initStatusLabels();
        return statusLabels;
    }
    public void setDarkMode() {
        super.setDarkMode();
        chessboardComponent.setDarkMode();
        if (isImageBackground){
            resetBackgroundImage(getSize());
        }
        repaint();
    }
    private void initThemeButton() {
        JButton button = initButton("Switch Theme");
        button.addActionListener(e -> {
            isDarkMode=!isDarkMode;
            switchTheme();
        });
        panelLeft.add(button);
    }
    private void initSettingButton(){
        JButton button = initButton("Setting");
        button.addActionListener(e -> {
            SettingFrame settingFrame = new SettingFrame();
            settingFrame.setVisible(true);
        });
        panelRight.add(button);
    }
    private void initNewGameButton(){
        JButton button = initButton("Start New");
        button.addActionListener(e -> chessboardComponent.startNewGame());
        panelRight.add(button);
    }
    private void initAutoConfirmButton(){
        JButton button = initButton("Hand Confirm");
        button.addActionListener(e -> {
            gameController.isAutoConfirm = !gameController.isAutoConfirm;
            button.setText(gameController.isAutoConfirm?"Auto Confirm":"Hand Confirm");
            swapConfirmButton.setEnabled(!gameController.isAutoConfirm);
            nextStepButton.setEnabled(!gameController.isAutoConfirm);
            var dialogMode = isDetailedDialog;
            isDetailedDialog = false;
            try {
                gameController.onPlayerSwapChess();
            } catch (Exception ignored) {}
            pauseMilliSeconds(100);
            try {
                gameController.nextStep();
            } catch (Exception ignored) {}
            isDetailedDialog=dialogMode;
        });
        panelRight.add(button);
    }
    private void initAutoGoButton(){
        JButton button = initButton("Auto:OFF");
        button.addActionListener(e -> {
            boolean isAutoMode = !getGameController().isAutoMode();
            button.setText("Auto:"+(isAutoMode?"ON":"OFF"));
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
        panelRight.add(button);
    }

    private void initNextStepButton() {
        JButton button = initButton("Next Step");
        button.addActionListener((e) -> chessboardComponent.nextStep());
        nextStepButton=button;
        panelRight.add(button);
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
        panelRight.add(button);
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
        panelRight.add(button);
    }
    private void initReturnTitleButton(){
        JButton button = initButton(isOnlinePlay()?"Disconnect":"Return Title");
        button.addActionListener(e -> returnToTitle());
        panelRight.add(button);
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
        panelRight.add(button);
    }
    public static boolean isOnlinePlay(){
        return startPlayMode>2;
    }
}
