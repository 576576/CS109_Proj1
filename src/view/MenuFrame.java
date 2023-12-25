package view;

import controller.GameController;
import model.Chessboard;
import model.Difficulty;
import model.DifficultyPreset;
import net.NetGame;

import javax.swing.*;
import java.awt.*;

/**
 * This class build the frame of the main menu window. It defines its size via a constant and creates
 * the layout with JFrame methods. It is also home to mnemonic-actions that activate the buttons in the frame.
 * Since Menu is the first visible window of the used, it is also here that a new game is called upon, as well
 * as showing the HighScoreFrame if the user wants this view.
 */
public class MenuFrame extends JFrame implements MyFrame{
    private final int hgt;
    public static boolean isDarkMode=false,isOnlinePlay=false,isToHost=false;
    public static Difficulty difficulty=new Difficulty(DifficultyPreset.EASY);
    private final int ONE_CHESS_SIZE;

    private final JPanel controlPanel = new JPanel(new GridLayout(5,1,4,8));
    private final JPanel chessPanel = new JPanel(new BorderLayout());
    private JLabel label;
    private final GridBagLayout gbl = new GridBagLayout();


    public MenuFrame(int width, int height) {
        setTitle("MENU");
        //    public final Dimension FRAME_SIZE ;
        this.hgt = height;
        this.ONE_CHESS_SIZE = (hgt * 4 / 5) / 9;
        setMinimumSize(new Dimension(905,600));

        setSize(width, hgt);
        setLocationRelativeTo(null); // Center the window.
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //设置程序关闭按键，如果点击右上方的叉就游戏全部关闭了
        setLayout(gbl);


        //addChessboard();
        addLabel();
        addPlay();
        addOnlineButton();
        addSettings();
        addExitButton();
        MyFrame.addComponent(this,gbl,controlPanel,1,1,0,0,0,0);
    }
    public boolean setDarkMode() {
        isDarkMode = !isDarkMode;
        getContentPane().setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        for (var i : getContentPane().getComponents()) {
            if (i.getClass() == JButton.class) {
                i.setBackground(isDarkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
                i.setForeground(!isDarkMode ? Color.BLACK : Color.WHITE);
            } else {
                i.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
            }
        }
        return isDarkMode;
    }

    private void addLabel() {
        label = new JLabel("MENU");
        label.setLocation(hgt - 360, hgt / 10 + 200);
        label.setSize(200, 60);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        controlPanel.add(label);
    }
    private void initChessboard() {
        //planning to add autoplaying chessboard
        ChessboardComponent chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
        chessPanel.add(chessboardComponent,BorderLayout.CENTER);
        MyFrame.addComponent(this,gbl,chessPanel,0,0,12,12,0,0);
    }
    private void addPlay() {
        JButton button = MyFrame.initButton("Play");
        button.addActionListener(e -> {
            isOnlinePlay=false;
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(this);
            difficultySelectFrame.setVisible(true);
        });
        controlPanel.add(button);
    }

    private void addOnlineButton() {
        JButton button = MyFrame.initButton("Online Play");

        button.addActionListener(e -> {
            isOnlinePlay=true;
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(this);
            difficultySelectFrame.setVisible(true);
        });
        controlPanel.add(button);
    }

    private void addSettings() {
        JButton button = MyFrame.initButton("Settings");
        button.addActionListener(e -> {
            //TODO:add settings panel
            JOptionPane.showMessageDialog(this, "FIX THIS");
        });
        controlPanel.add(button);
    }

    public void addExitButton(){
        JButton button = MyFrame.initButton("Exit");
        button.addActionListener(e -> System.exit(0));
        controlPanel.add(button);
    }
    public void generateNewGame(){
        ChessGameFrame mainFrame = new ChessGameFrame(1100, 810);
        GameController gameController = new GameController(mainFrame.getChessboardComponent(),
                new Chessboard(), new NetGame());
        mainFrame.setGameController(gameController);
        mainFrame.setMenuFrame(this);
        gameController.setChessGameFrame(mainFrame);
        mainFrame.setVisible(true);
        this.dispose();
    }
}

