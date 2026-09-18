package util;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 定位 resource 目录。三种运行形态各有一条路：
 * 开发时它就在工作目录下；打成 exe 后 jpackage 把 resource 和 jar 放在同一个 app 目录里；
 * 单独拿着胖 jar 从别的目录启动时，resource 只存在于 jar 内部，要先摊到缓存目录。
 */
public final class ResourceRoot {

    private static final String NAME = "resource";

    private ResourceRoot() {
    }

    /** resource 目录；三条路都走不通时退回到相对路径，让调用方拿到一个说得通的错误。 */
    public static Path root() {
        for (Path candidate : candidates()) {
            if (candidate != null && Files.isDirectory(candidate)) return candidate;
        }
        return Path.of(NAME);
    }

    /** resource 下的某个相对路径，例如 "texture/chess/0.png"。 */
    public static Path path(String relative) {
        return root().resolve(relative);
    }

    public static String pathText(String relative) {
        return path(relative).toString();
    }

    private static List<Path> candidates() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(NAME));
        Path codeLocation = codeLocationOf();
        if (codeLocation == null) return candidates;
        Path base = Files.isRegularFile(codeLocation) ? codeLocation.getParent() : codeLocation;
        candidates.add(base.resolve(NAME));
        candidates.add(unpackFrom(codeLocation));
        return candidates;
    }

    /** 把 jar 里的 resource 摊到缓存目录；已经摊过就直接用。 */
    private static Path unpackFrom(Path jar) {
        if (!Files.isRegularFile(jar)) return null;
        try {
            Path cache = cacheDirFor(jar);
            Path target = cache.resolve(NAME);
            if (Files.isDirectory(target)) return target;
            Files.createDirectories(cache);
            try (FileSystem jarFs = FileSystems.newFileSystem(jar)) {
                Path source = jarFs.getPath("/" + NAME);
                if (!Files.isDirectory(source)) return null;
                try (Stream<Path> entries = Files.walk(source)) {
                    for (Path entry : entries.toList()) {
                        Path destination = cache.resolve(entry.toString().substring(1));
                        if (Files.isDirectory(entry)) Files.createDirectories(destination);
                        else Files.copy(entry, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return target;
        } catch (IOException | RuntimeException e) {
            Log.warn("Cannot unpack bundled resources: " + e);
            return null;
        }
    }

    /** 缓存目录带上 jar 的大小和修改时间，jar 换了就重新摊一次。 */
    private static Path cacheDirFor(Path jar) throws IOException {
        String stamp = Files.size(jar) + "-" + Files.getLastModifiedTime(jar).toMillis();
        return Path.of(System.getProperty("java.io.tmpdir"), "match3-resources", stamp);
    }

    private static Path codeLocationOf() {
        try {
            var source = ResourceRoot.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) return null;
            return Path.of(source.getLocation().toURI());
        } catch (Exception _) {
            return null;
        }
    }
}
