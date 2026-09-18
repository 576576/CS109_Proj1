package view;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import model.Board;
import net.NetGame;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import util.Log;

/**
 * This class build the frame of the main menu window. It defines its size via a constant and creates
 * the layout with JFrame methods. It is also home to mnemonic-actions that activate the buttons in the frame.
 * Since Menu is the first visible window of the used, it is also here that a new game is called upon, as well
 * as showing the HighScoreFrame if the user wants this view.
 */
public class MenuFrame extends MyFrame{
    public static int musicVolume;
    public static ArrayList<File> musicFiles = new ArrayList<>();
    private final int ONE_CHESS_SIZE;
    private final GameSettings settings = new GameSettings();

    private final JPanel controlPanel = new JPanel(new GridLayout(5,1,4,8));
    private final JPanel chessPanel = new JPanel(new BorderLayout());
    private final GridBagLayout gbl = new GridBagLayout();

    public MenuFrame(int width, int height) {
        setTitle("MATCH-3 CS109");
        //    public final Dimension FRAME_SIZE ;
        this.ONE_CHESS_SIZE = (height * 4 / 5) / 9;
        setMinimumSize(new Dimension(905,600));

        setSize(width, height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(gbl);

        initLabel();
        initPlayButton();
        initOnlineButton();
        initSettingButton();
        initExitButton();
        controlPanel.setOpaque(false);
        addComponent(this,gbl,controlPanel,1,1,5,5,0,0);
        musicFiles = readFiles("resource/music");
        if (musicFiles==null || musicFiles.isEmpty()) return;
        Log.info("Musics Loaded: "+musicFiles.size());
        setDarkMode();
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
        button.addActionListener(e -> openDifficultySelect(PlayMode.NEW_LOCAL));
        controlPanel.add(button);
    }
    private void initOnlineButton() {
        JButton button = initButton("Online Play");
        button.addActionListener(e -> openDifficultySelect(PlayMode.JOIN));
        controlPanel.add(button);
    }
    private void openDifficultySelect(PlayMode mode) {
        settings.setPlayMode(mode);
        DifficultySelectFrame difficultySelectFrame = new DifficultySelectFrame(this, settings);
        difficultySelectFrame.setVisible(true);
    }
    private void initSettingButton(){
        JButton button = initButton("Settings");
        button.addActionListener(e -> {
            SettingFrame settingFrame = new SettingFrame(settings);
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
        if (settings.playMode() == PlayMode.NONE) {
            JOptionPane.showMessageDialog(this,"No game-mode selected!");
            return;
        }
        GameFrame mainFrame = new GameFrame(1100, 810, settings);
        GameController gameController = new GameController(mainFrame.getBoardView(),
                new Board(), new NetGame(settings), settings);
        mainFrame.setGameController(gameController);
        mainFrame.setMenuFrame(this);
        gameController.setGameFrame(mainFrame);
        Log.info("GameFrame: Initialize done");
        Log.info("Difficulty: "+settings.difficulty().name());
        mainFrame.setVisible(true);
        this.setState(Frame.ICONIFIED);
        mainFrame.beginPlay();
    }

    public static void setVolume(int volume) {
        musicVolume=volume;
        for (var mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                var mixer = AudioSystem.getMixer(mixerInfo);
                mixer.open();
                SourceDataLine sourceDataLine = (SourceDataLine) mixer.getLine(mixer.getSourceLineInfo()[0]); // 选择第n个音频设备
                FloatControl.Type volumeControlType = FloatControl.Type.MASTER_GAIN; // 主音量控制
                if (!sourceDataLine.isControlSupported(volumeControlType)) {
                    Log.info("不支持音量控制");
                    return;
                }
                FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(volumeControlType); // 获取音量控制对象
                float dB = (float) (Math.log(musicVolume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(dB);
            } catch (Exception _) {}
        }
    }
}
