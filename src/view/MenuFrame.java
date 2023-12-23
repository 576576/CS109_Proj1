package view;

import controller.GameController;
import model.Chessboard;
import net.NetGame;

import javax.swing.*;
import java.awt.*;

/**
 * This class build the frame of the main menu window. It defines its size via a constant and creates
 * the layout with JFrame methods. It is also home to mnemonic-actions that activate the buttons in the frame.
 * Since Menu is the first visible window of the used, it is also here that a new game is called upon, as well
 * as showing the HighscoreFrame if the user wants this view.
 */
public class MenuFrame extends JFrame implements MyFrame{
    //    public final Dimension FRAME_SIZE ;
    private final int wdt, hgt;
    public static boolean isDarkMode;

    private final int ONE_CHESS_SIZE;
    private ChessboardComponent chessboardComponent;

    private GameController gameController;
    private JPanel controlPanel = new JPanel(new GridLayout(5,1,4,8));
    private JPanel chessPanel = new JPanel(new BorderLayout());

    private Menu Menu;

    private JLabel label;
    private GridBagLayout gbl = new GridBagLayout();


    public MenuFrame(int width, int height) {
        setTitle("MENU");
        this.wdt = width;
        this.hgt = height;
        this.ONE_CHESS_SIZE = (hgt * 4 / 5) / 9;

        setSize(wdt, hgt);
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

    // public Menu() {}


    /**
     * private void addChessboard() {
     * chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
     * chessboardComponent.setLocation(HEIGTH / 5, HEIGTH / 10);
     * add(chessboardComponent);
     * }
     */
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
        label = new JLabel("CHECKING");
        label.setLocation(hgt - 360, hgt / 10 + 200);
        label.setSize(200, 60);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        controlPanel.add(label);
    }
    private void initChessboard() {
        //planning to add a auto-playing chessboard
        chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
        chessPanel.add(chessboardComponent,BorderLayout.CENTER);
        MyFrame.addComponent(this,gbl,chessPanel,0,0,12,12,0,0);
    }

    public JLabel getLabel() {
        return label;
    }

    public void setLabel(JLabel label) {
        this.label = label;
    }


    private void addPlay() {
        JFrame close = new JFrame();
        JButton button = MyFrame.initButton("Play");
        button.addActionListener(e -> {

            //!important (comment by 576) please use a frame or panel instead of simple joptionpane to have more flexibility.
            //of course that's ok to change the existing code if u can make it run appropriately.
            //TODO:FIX THIS

            //JOptionPane.showMessageDialog(this, "FIX THIS");
            // close.setVisible(false); //hides it temporarily
            //frame2.setVisible(true); //shows it
            //Custom button text
            Object[] options = {"Easy",
                    "Medium",
                    "Hard"};
            int n = JOptionPane.showOptionDialog(this,
                    "Choose your difficulty",
                    "Difficulty",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[2]);
            SwingUtilities.invokeLater(() -> {
                ChessGameFrame mainFrame = new ChessGameFrame(1100, 810, false);
                GameController gameController = new GameController(mainFrame.getChessboardComponent(),
                        new Chessboard(), new NetGame());
                mainFrame.setGameController(gameController);
                mainFrame.setMenuFrame(this);
                gameController.setStatusLabel(mainFrame.getStatusLabel());
                gameController.setDifficultyLabel(mainFrame.getDifficultyLabel());
                gameController.setChessGameFrame(mainFrame);
                mainFrame.setVisible(true);
            });
        });
        controlPanel.add(button);
    }


    private void addOnlineButton() {
        JButton button = MyFrame.initButton("Online Play");

        button.addActionListener(e -> {
            //TODO: start with online play(if host,show difficulty choose panel)
            Object[] options = {"Host", "Join"};
            int n = JOptionPane.showOptionDialog(this,
                    "Choose your play mode",
                    "Play Online",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);
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
}

