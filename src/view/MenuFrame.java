package view;

import controller.GameController;
import model.Chessboard;
import model.Difficulty;
import model.DifficultyPreset;
import net.NetGame;
import player.MusicPlayer;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import static view.ChessGameFrame.isGameFrameInitDone;

/**
 * This class build the frame of the main menu window. It defines its size via a constant and creates
 * the layout with JFrame methods. It is also home to mnemonic-actions that activate the buttons in the frame.
 * Since Menu is the first visible window of the used, it is also here that a new game is called upon, as well
 * as showing the HighScoreFrame if the user wants this view.
 */
public class MenuFrame extends MyFrame{
    public static boolean isDetailedDialog=false;
    public static int musicVolume;
    public static int startPlayMode=0;// 0=not to start 1=play new game locally 2=play locally load from file 3=host game 4=join game
    public static Difficulty difficulty=new Difficulty(DifficultyPreset.EASY);
    private final int ONE_CHESS_SIZE;

    private final JPanel controlPanel = new JPanel(new GridLayout(5,1,4,8));
    private final JPanel chessPanel = new JPanel(new BorderLayout());
    private final GridBagLayout gbl = new GridBagLayout();
    public static ArrayList<File> musicFiles = new ArrayList<>();
    public static Thread musicThread;

    public static MusicPlayer musicPlayer = new MusicPlayer();

    public MenuFrame(int width, int height) {
        setTitle("MATCH-3 CS109");
        //    public final Dimension FRAME_SIZE ;
        this.ONE_CHESS_SIZE = (height * 4 / 5) / 9;
        setMinimumSize(new Dimension(905,600));

        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(gbl);

        //initChessboard();
        initLabel();
        initPlayButton();
        initOnlineButton();
        initSettingButton();
        initExitButton();
        controlPanel.setOpaque(false);
        addComponent(this,gbl,controlPanel,1,1,5,5,0,0);
        musicFiles = readFiles("resource/music");
        if (musicFiles==null || musicFiles.isEmpty()) return;
        System.out.println("Musics Loaded: "+musicFiles.size());
        setDarkMode();
    }
    private void initChessboard() {
        //TODO:planning to add autoplaying chessboard
        ChessboardComponent chessboardComponent = new ChessboardComponent(ONE_CHESS_SIZE);
        chessPanel.add(chessboardComponent,BorderLayout.CENTER);
        addComponent(this,gbl,chessPanel,0,0,560,560,0,0);
    }
    private void initLabel() {
        JLabel label = new JLabel("MATCH-3");
        label.setSize(200, 60);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(new Font("Rockwell", Font.BOLD, 20));
        controlPanel.add(label);
    }
    private void initPlayButton() {
        JButton button = initButton("Play");
        button.addActionListener(e -> {
            startPlayMode=1;
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(this);
            difficultySelectFrame.setVisible(true);
        });
        controlPanel.add(button);
    }
    private void initOnlineButton() {
        JButton button = initButton("Online Play");

        button.addActionListener(e -> {
            startPlayMode=4;
            DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(this);
            difficultySelectFrame.setVisible(true);
        });
        controlPanel.add(button);
    }
    private void initSettingButton(){
        JButton button = initButton("Settings");
        button.addActionListener(e -> {
            SettingFrame settingFrame = new SettingFrame();
            settingFrame.setVisible(true);
        });
        controlPanel.add(button);
    }
    public void initExitButton(){
        JButton button = initButton("Exit");
        button.addActionListener(e -> System.exit(0));
        controlPanel.add(button);
    }
    public void generateNewGame(){
        isGameFrameInitDone=false;
        if (startPlayMode!=0) { //when game start, generate new game
            ChessGameFrame mainFrame = new ChessGameFrame(1100, 810);
            GameController gameController = new GameController(mainFrame.getChessboardComponent(),
                    new Chessboard(), new NetGame());
            mainFrame.setGameController(gameController);
            mainFrame.setMenuFrame(this);
            gameController.setChessGameFrame(mainFrame);
            System.out.println("GameFrame: Initialize done");
            System.out.println("Difficulty: "+difficulty.getName());
            mainFrame.setVisible(true);
            this.setState(Frame.ICONIFIED);
            isGameFrameInitDone =true;
        }
        else JOptionPane.showMessageDialog(this,"No game-mode selected!");
    }

    public static void setVolume(int volume) {
        musicVolume=volume;
        for (var mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                var mixer = AudioSystem.getMixer(mixerInfo);
                mixer.open();
//                Line.Info[] lineInfos = mixer.getSourceLineInfo(); // 获取音频设备的Line.Info对象
                SourceDataLine sourceDataLine = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]); // 选择第n个音频设备
                FloatControl.Type volumeControlType = FloatControl.Type.MASTER_GAIN; // 主音量控制
                if (!sourceDataLine.isControlSupported(volumeControlType)) {
                    System.out.println("不支持音量控制");
                    return;
                }
                FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(volumeControlType); // 获取音量控制对象
                float dB = (float) (Math.log(musicVolume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(dB);
            } catch (Exception ignored) {}
        }
    }
}

