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
    public class MenuFrame extends JFrame  {
        //    public final Dimension FRAME_SIZE ;
        private final int WIDTH;
        private final int HEIGTH;

        private final int ONE_CHESS_SIZE;
        private ChessboardComponent chessboardComponent;

      private GameController gameController;

        private Menu Menu;

        private JLabel statusLabel;

        public MenuFrame(int width, int height) {
            setTitle("MENU");
            this.WIDTH = width;
            this.HEIGTH = height;
            this.ONE_CHESS_SIZE = (HEIGTH * 4 / 5) / 9;

            setSize(WIDTH, HEIGTH);
            setLocationRelativeTo(null); // Center the window.
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //设置程序关闭按键，如果点击右上方的叉就游戏全部关闭了
            setLayout(null);


            //addChessboard();
            addLabel();
            addPlay();
            addSettings();
            addExit();
            addLoadButton();
        }

       // public Menu() {}






        /**
         * private void addChessboard() {
            chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
            chessboardComponent.setLocation(HEIGTH / 5, HEIGTH / 10);
            add(chessboardComponent);
        }
         */


        private void addLabel() {
            this.statusLabel = new JLabel("CHECKING");
            statusLabel.setLocation(HEIGTH-360, HEIGTH / 10+200);
            statusLabel.setSize(200, 60);
            statusLabel.setFont(new Font("Rockwell", Font.BOLD, 20));
            add(statusLabel);
        }

        public JLabel getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(JLabel statusLabel) {
            this.statusLabel = statusLabel;
        }



        private void addPlay() {
            JFrame close = new JFrame();
            JButton button = new JButton("Play");
            button.addActionListener(e -> {
                //JOptionPane.showMessageDialog(this, "FIX THIS");
               // close.setVisible(false); //hides it temporarily
                //frame2.setVisible(true); //shows it
                SwingUtilities.invokeLater(() -> {
                    ChessGameFrame mainFrame = new ChessGameFrame(1100, 810);
                    GameController gameController = new GameController(mainFrame.getChessboardComponent(), new Chessboard(),new NetGame());
                    mainFrame.setGameController(gameController);
                    gameController.setStatusLabel(mainFrame.getStatusLabel());
                    gameController.setDifficultyLabel(mainFrame.getDifficultyLabel());
                    mainFrame.setVisible(true);
            }); });

            button.setLocation(HEIGTH-360, HEIGTH / 10 + 240);
            button.setSize(200, 60);
            button.setFont(new Font("Rockwell", Font.BOLD, 20));
            add(button);
        }




        private void addLoadButton() {
            JButton button = new JButton("Load");
            button.setLocation(HEIGTH-360, HEIGTH / 10 + 300);
            button.setSize(200, 60);
            button.setFont(new Font("Rockwell", Font.BOLD, 20));
            add(button);

            button.addActionListener(e -> {
                System.out.println("Click load");
                String path = JOptionPane.showInputDialog(this, "Input Path here");
                System.out.println(path);
//            gameController.loadGameFromFile(path);
            });
        }
    private void addSettings() {
        JButton button = new JButton("Settings");
        button.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "FIX THIS");
        });
        button.setLocation(HEIGTH-360, HEIGTH / 10 + 360);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);
    }
    private void addExit() {
        JButton button = new JButton("Exit");
        /*button.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "FIX THIS");
        });*/
        button.addActionListener(e -> { if (JOptionPane.showConfirmDialog( button,"confirm if you Want to Exit","Name of the Application or Title",
                JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
            System.exit(0);
        });

        button.setLocation(HEIGTH-360, HEIGTH / 10 + 420);
        button.setSize(200, 60);
        button.setFont(new Font("Rockwell", Font.BOLD, 20));
        add(button);
    }
    }

    /*public static void addComponentsToPane(Container pane) {
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        addAButton("Start", pane);
        addAButton("Exit", pane);
        addAButton("Button 3", pane);
        //addAButton("Long-Named Button 4", pane);
        //addAButton("5", pane);
    }

    private static void addAButton(String text, Container container) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(button);
    }*/


    /*private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Match-3");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set up the content pane.
        addComponentsToPane(frame.getContentPane());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }*/

