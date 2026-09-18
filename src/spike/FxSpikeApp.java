package spike;

import io.github.palexdev.mfxcomponents.controls.buttons.MFXButton;
import io.github.palexdev.mfxcomponents.theming.JavaFXThemes;
import io.github.palexdev.mfxcomponents.theming.MaterialThemes;
import io.github.palexdev.mfxcomponents.theming.UserAgentBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.glavo.monetfx.Brightness;
import org.glavo.monetfx.ColorScheme;

/** 验证 JavaFX + MaterialFX + MonetFX 在当前 JDK 上是否真的能跑起来。 */
public class FxSpikeApp extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws java.io.IOException {
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialThemes.INDIGO_DARK)
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        ColorScheme scheme = ColorScheme.fromSeed(Color.web("#5C6BC0"));
        System.out.println("monet primary  = " + scheme.getPrimary());
        System.out.println("monet onPrimary= " + scheme.getOnPrimary());
        System.out.println("monet surface  = " + scheme.getSurface());
        System.out.println("monet brightness = " + scheme.getBrightness());
        System.out.println("stylesheet bytes = " + scheme.toStyleSheet().length());

        ColorScheme dark = ColorScheme.newBuilder()
                .setPrimaryColorSeed(Color.web("#8B5CF6"))
                .setBrightness(Brightness.DARK)
                .build();
        System.out.println("dark scheme primary = " + dark.getPrimary());

        // 关键一步：Monet 从背景图取种子色，整套配色跟着背景走
        java.io.File wallpaper = new java.io.File("resource/texture/background/dark/Stars_wpe.png");
        ColorScheme monet = ColorScheme.fromImage(new javafx.scene.image.Image(wallpaper.toURI().toString()));
        System.out.println("monet from wallpaper primary = " + monet.getPrimary());

        // 把 Monet 的样式表挂进 Scene，-monet-* 变量才解析得出来
        java.nio.file.Path css = java.nio.file.Files.createTempFile("monet-", ".css");
        java.nio.file.Files.writeString(css, monet.toStyleSheet());

        MFXButton button = new MFXButton("MaterialFX button");
        VBox root = new VBox(16, button);
        root.setStyle("-fx-background-color: -monet-surface; -fx-padding: 32;");
        Scene scene = new Scene(root, 520, 320);
        scene.getStylesheets().add(css.toUri().toString());
        stage.setTitle("FX spike");
        stage.setScene(scene);
        stage.show();

        System.out.println("SPIKE OK: JavaFX " + System.getProperty("javafx.version")
                + " on JDK " + Runtime.version());
        ApplicationHolder.stage = stage;
    }

    /** 让外部能在几秒后关掉窗口，不用人去点。 */
    static final class ApplicationHolder {
        static Stage stage;
    }
}
