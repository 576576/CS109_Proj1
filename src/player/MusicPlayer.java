package player;

import util.Log;
import util.ResourceRoot;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;

/**
 * 播放音频。格式识别与解码全部交给 javax.sound.sampled 的 SPI：
 * AudioSystem 会从 classpath 上发现 AudioFileReader（wav 内置，flac 由 jflac 提供，
 * mp3 由 mp3spi 提供），再由 FormatConversionProvider 统一转成 PCM。
 * 这里不按扩展名分支，因此新增格式只需加一个 provider jar。
 */
public class MusicPlayer {

    private static final int BUFFER_SIZE = 4096;

    private volatile SourceDataLine line;
    private volatile boolean stopped;

    /** 播放一个音频文件；没有 provider 认得这个格式时只记一条日志。 */
    public void play(File file) {
        if (file == null || !file.isFile()) {
            Log.info("Invalid file input.");
            return;
        }
        Log.info("Current Music: " + file.getName());
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(file)) {
            AudioFormat pcm = pcmOf(raw.getFormat());
            try (AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, raw)) {
                writeToLine(decoded, pcm);
            }
        } catch (UnsupportedAudioFileException _) {
            Log.info("Unsupported music format: " + file.getName());
        } catch (Exception e) {
            Log.info("Error playing file: " + e.getMessage());
        }
    }

    /** 源文件不是 PCM 时给出要转成的目标格式；已经是 PCM 就原样用。 */
    private static AudioFormat pcmOf(AudioFormat source) {
        if (source.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) return source;
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                source.getSampleRate(),
                16,
                source.getChannels(),
                source.getChannels() * 2,
                source.getSampleRate(),
                false);
    }

    private void writeToLine(AudioInputStream decoded, AudioFormat pcm) throws Exception {
        var info = new SourceDataLine.Info(SourceDataLine.class, pcm);
        if (!(AudioSystem.getLine(info) instanceof SourceDataLine target)) {
            throw new IllegalStateException("No source line for " + pcm);
        }
        line = target;
        stopped = false;
        target.open(pcm);
        try {
            target.start();
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int read = decoded.read(buffer); read != -1 && !stopped; read = decoded.read(buffer)) {
                target.write(buffer, 0, read);
            }
            target.drain();
        } finally {
            target.stop();
            target.close();
            line = null;
        }
    }

    /** 停止当前播放。 */
    public void stop() {
        stopped = true;
        SourceDataLine current = line;
        if (current != null && current.isOpen()) current.close();
    }

    /** 播放音效，找不到文件就跳过。 */
    public static void playEffect(String effectName) {
        playEffect(ResourceRoot.path("effect/sound/" + effectName + ".mp3").toFile());
    }

    public static void playEffect(File effectFile) {
        if (effectFile == null || !effectFile.isFile()) {
            Log.info("Effect file not found: " + effectFile);
            return;
        }
        new Thread(() -> new MusicPlayer().play(effectFile), "effect-" + effectFile.getName()).start();
    }

    public static void playWarning() {
        playEffect("warning");
    }
}
