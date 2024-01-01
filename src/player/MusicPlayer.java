package player;

import javazoom.jl.player.Player;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import static view.MenuFrame.musicThread;

public class MusicPlayer {
    SourceDataLine sourceDataLine;
    public static Player player_mp3;
    public void play(File f) {
        System.out.println("Current Music: "+f.getName());
        if (isFileExtensionName(f,"flac")) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
                AudioFormat audioFormat = audioInputStream.getFormat();
                if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
                    audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioInputStream);
                }

                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
                sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

                int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
                int numBytes = 1024 * bytesPerFrame;
                byte[] audioBytes = new byte[numBytes];
                while (audioInputStream.read(audioBytes) != -1) {
                    sourceDataLine.write(audioBytes, 0, audioBytes.length);
                }
                sourceDataLine.drain();
                sourceDataLine.stop();
                sourceDataLine.close();
            } catch (Exception ignored) {}
        }
        else if (isFileExtensionName(f,"mp3")){
            try {
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(f));
                    player_mp3 = new javazoom.jl.player.Player(stream);
                    player_mp3.play();
                    player_mp3.close();
            } catch (Exception ignored) {}
        }
        else System.out.println("Not supported music(mp3 flac)");
    }
    public void close(){
        try {
            musicThread.interrupt();
            sourceDataLine.close();
            player_mp3.close();
        } catch (Exception ignored) {}
    }
    public static boolean isFileExtensionName(File f,String extension) {
        return f.getName().lastIndexOf('.') > 0 && f.getName().substring(f.getName().lastIndexOf('.') + 1).equals(extension);
    }
}
