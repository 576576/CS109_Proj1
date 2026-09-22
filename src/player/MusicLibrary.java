package player;

import util.Log;
import util.ResourceRoot;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** resource/music 下的曲目清单，以及播放音量。 */
public final class MusicLibrary {

    private final List<File> files;
    private int volume = 20;

    public MusicLibrary() {
        files = scan();
        Log.info("Musics Loaded: " + files.size());
    }

    public List<File> files() {
        return files;
    }

    public int volume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        applySystemVolume();
    }

    private List<File> scan() {
        Path directory = ResourceRoot.path("music");
        try (var entries = Files.list(directory)) {
            return entries.filter(Files::isRegularFile).map(Path::toFile).toList();
        } catch (Exception e) {
            Log.warn("No music under " + directory + ": " + e);
            return List.of();
        }
    }

    /** 调的是混音器的主音量，音乐线程本身不改动增益。 */
    private void applySystemVolume() {
        for (var mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                var mixer = AudioSystem.getMixer(mixerInfo);
                mixer.open();
                var lines = mixer.getSourceLineInfo();
                if (lines.length == 0) continue;
                SourceDataLine line = (SourceDataLine) mixer.getLine(lines[0]);
                if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) continue;
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue((float) (Math.log(volume) / Math.log(10.0) * 20.0));
                return;
            } catch (Exception _) {
            }
        }
    }
}
