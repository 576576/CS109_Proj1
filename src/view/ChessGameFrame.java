package view;

import controller.GameController;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import static view.MenuFrame.isOnlinePlay;

public class ChessGameFrame extends JFrame implements MyFrame{
    private final int ONE_CHESS_SIZE;

    private GameController gameController;
    private MenuFrame menuFrame;
    private ChessboardComponent chessboardComponent;
    private JButton swapConfirmButton,nextStepButton;
    private final JPanel controlPanel = new JPanel();
    private final JPanel statusPanel = new JPanel(new GridLayout(10,1,2,6));
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
        FileNameExtensionFilter ff = new FileNameExtensionFilter("txt", "txt");
        jf.addChoosableFileFilter(ff);
        jf.setFileFilter(ff);

        setSize(width, height);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(gbl);

        initBasicComponents();
        if (!isOnlinePlay) initLocalPlayPanel();
        else initOnlinePlayPanel();
        MyFrame.addComponent(this,gbl,controlPanel,590,1,560,4,0,1);
        SwingUtilities.invokeLater(()->{
            setDarkMode();setDarkMode();
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                SwingUtilities.invokeLater(()-> System.out.println("Window size changed to \\["+getWidth()+" , "+getHeight()+"]"));
            }
        });
    }
    private void initBasicComponents(){
        initChessboard();
        initStatusLabels();
        for (var i:statusLabels) statusPanel.add(i);
        MyFrame.addComponent(this,gbl,statusPanel,1,1,24,24,0,1);
        initDarkModeButton();
        initAutoConfirmButton();
    }
    private void initLocalPlayPanel(){
        controlPanel.setLayout(new GridLayout(10,1,2,6));
        initNewGameButton();
        initHintButton();
        initSwapConfirmButton();
        initNextStepButton();
        initLoadButton();
        initSaveButton();
        initReturnTitleButton();
        initExitButton();
        for (var component: controlComponents) controlPanel.add(component);
    }
    private void initOnlinePlayPanel(){
        controlPanel.setLayout(new GridLayout(8,1,2,6));
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
        statusLabels[0] = MyFrame.initLabel("Difficulty:EASY");
        statusLabels[1] = MyFrame.initLabel("Score:0/30");
        statusLabels[2] = MyFrame.initLabel("StepLeft:∞");
        statusLabels[3] = MyFrame.initLabel("TimeLimit:∞");
    }
    public JLabel[] getStatusLabels(){
        if (statusLabels==null) initStatusLabels();
        return statusLabels;
    }
    public void setDarkMode() {
        boolean isDarkMode=menuFrame.setDarkMode();
        try {
            getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            chessboardComponent.setDarkMode(isDarkMode);
            for (var i:getContentPane().getComponents()){
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            }
            for (var i:statusPanel.getComponents()){
                i.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            }
            for (var i: controlComponents){
                i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            }
            chessboardComponent.setDarkMode(isDarkMode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        repaint();
    }

    private void initDarkModeButton() {
        JButton button = initButton("SetDarkMode");
        button.addActionListener(e -> setDarkMode());
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
            menuFrame.setState(Frame.NORMAL);
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
