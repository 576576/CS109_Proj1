package player;

import javazoom.jl.player.Player;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import static view.MenuFrame.musicThread;

public class MusicPlayer {
    private SourceDataLine sourceDataLine;

    /**
     * 播放指定的音频文件。
     *
     * @param f 音频文件
     */
    public void play(File f) {
        if (!isValidFile(f)) {
            System.out.println("Invalid file input.");
            return;
        }

        System.out.println("Current Music: " + f.getName());

        try {
            String fileName = f.getName().toLowerCase();
            switch (fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()) {
                case "flac" -> playFlac(f);
                case "mp3" -> playMp3(f);
                case "wav" -> playWav(f);
                default -> System.out.println("Unsupported music format (mp3, flac, wav)");
            }
        } catch (Exception e) {
            System.out.println("Error playing file: " + e.getMessage());
        }
    }

    /**
     * 播放FLAC格式的音频文件。
     *
     * @param f FLAC音频文件
     * @throws Exception 如果播放过程中出现错误
     */
    private void playFlac(File f) throws Exception {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f)) {
            AudioFormat audioFormat = getPCMFormat(audioInputStream.getFormat());
            playAudioStream(audioInputStream, audioFormat);
        }
    }

    /**
     * 播放MP3格式的音频文件。
     *
     * @param f MP3音频文件
     * @throws Exception 如果播放过程中出现错误
     */
    private void playMp3(File f) throws Exception {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(f))) {
            Player mp3Player = createMp3Player(stream);
            mp3Player.play();
        }
    }

    /**
     * 创建并返回一个MP3播放器实例。
     *
     * @param stream MP3文件的输入流
     * @return MP3播放器实例
     * @throws Exception 如果创建播放器时出错
     */
    private Player createMp3Player(BufferedInputStream stream) throws Exception {
        return new Player(stream);
    }

    /**
     * 播放WAV格式的音频文件。
     *
     * @param f WAV音频文件
     * @throws Exception 如果播放过程中出现错误
     */
    private void playWav(File f) throws Exception {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f)) {
            playAudioStream(audioInputStream, audioInputStream.getFormat());
        }
    }

    /**
     * 播放音频流。
     *
     * @param audioInputStream 音频输入流
     * @param audioFormat      音频格式
     * @throws Exception 如果播放过程中出现错误
     */
    private void playAudioStream(AudioInputStream audioInputStream, AudioFormat audioFormat) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open(audioFormat);
        sourceDataLine.start();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = audioInputStream.read(buffer)) != -1) {
            sourceDataLine.write(buffer, 0, bytesRead);
        }

        sourceDataLine.drain();
        sourceDataLine.stop();
        sourceDataLine.close();
    }

    /**
     * 获取PCM格式的音频格式。
     *
     * @param format 原始音频格式
     * @return PCM格式的音频格式
     */
    private AudioFormat getPCMFormat(AudioFormat format) {
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            return new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false
            );
        }
        return format;
    }

    /**
     * 关闭所有正在播放的音频。
     */
    public void close() {
        try {
            if (musicThread != null) {
                musicThread.interrupt();
            }
            if (sourceDataLine != null && sourceDataLine.isOpen()) {
                sourceDataLine.close();
            }
        } catch (Exception e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否有效。
     *
     * @param f 文件
     * @return 如果文件有效，则返回true；否则返回false
     */
    private boolean isValidFile(File f) {
        return f != null && f.exists() && f.isFile();
    }

    /**
     * 播放警告音效。
     */
    public static void playWarning() {
        playEffect("warning");
    }

    /**
     * 播放指定名称的音效。
     *
     * @param effectName 音效名称
     */
    public static void playEffect(String effectName) {
        new Thread(() -> {
            try {
                File effectFile = new File("resource/effect/sound/" + effectName + ".mp3");
                if (effectFile.exists() && effectFile.isFile()) {
                    try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(effectFile))) {
                        Player mp3Player = new Player(stream);
                        mp3Player.play();
                    }
                } else {
                    System.out.println("Effect file not found: " + effectFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.out.println("Error playing effect: " + e.getMessage());
            }
        }).start();
    }
}