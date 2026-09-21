package ui;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Board;
import net.NetGame;
import player.MusicLibrary;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import util.ResourceRoot;

/** 整个应用只有一个窗口：菜单、开局设置、对局、设置都是换掉中间那块内容。 */
public class Match3App extends Application {

    private static final int WIDTH = 1180;
    private static final int HEIGHT = 820;

    private static Match3App instance;
    /** 探针要等到窗口真的起来才能接着操作。 */
    public static final CountDownLatch STARTED = new CountDownLatch(1);

    private Stage stage;
    private Scene scene;
    private StackPane root;
    private ImageView background;
    private Theme theme;
    private MusicLibrary musicLibrary;
    private GameSettings settings = new GameSettings();
    private GameView gameView;
    private Node currentView;

    public static Match3App get() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.stage = stage;

        // MaterialFX 把自己的样式表并进 user-agent，必须在建任何控件之前做
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        theme = new Theme();
        musicLibrary = new MusicLibrary();

        background = new ImageView();
        background.setPreserveRatio(false);
        root = new StackPane(background);
        scene = new Scene(root, WIDTH, HEIGHT);
        bindCover(background, root);

        applyTheme();
        showMenu();

        stage.setTitle("Match-3 CS109");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        setStageIcon(stage);
        stage.show();
        STARTED.countDown();
    }

    public Stage stage() {
        return stage;
    }

    public Scene scene() {
        return scene;
    }

    public Theme theme() {
        return theme;
    }

    public MusicLibrary musicLibrary() {
        return musicLibrary;
    }

    /** 当前对局的控制器；没有对局时为 null。 */
    public GameController controller() {
        return gameView == null ? null : gameView.controller();
    }

    public void showMenu() {
        settings = new GameSettings();
        gameView = null;
        setContent(new MenuView(settings, theme, this));
    }

    public void showSettings(GameSettings settings) {
        setContent(new SettingsView(settings == null ? this.settings : settings, theme, this));
    }

    public void showGame(GameSettings settings) {
        if (settings.playMode().isOnline()) {
            startOnline(settings);
            return;
        }
        if (settings.playMode() == PlayMode.LOAD_LOCAL && settings.saveFile() == null) {
            Dialogs.warn(I18n.tr("dlg.pickSave"));
            return;
        }
        GameView view = new GameView(settings, theme, this);
        GameController controller =
                new GameController(view.board(), new Board(), new NetGame(settings), settings, view);
        view.setController(controller);
        gameView = view;
        setContent(view);
        view.beginPlay();
    }

    /**
     * 联机（建房/加入）：连接在主界面上建立，所以先不切到棋盘——等待/连接窗以主界面为背景呈现；
     * 连上后才切到棋盘并开始对局，取消或失败则留在（或返回）主界面。
     */
    private void startOnline(GameSettings settings) {
        GameView view = new GameView(settings, theme, this);
        NetGame net = new NetGame(settings);
        GameController controller =
                new GameController(view.board(), new Board(), net, settings, view);
        view.setController(controller);

        Thread launch = new Thread(() -> {
            if (settings.playMode() == PlayMode.HOST) {
                Dialogs.waitWhile(
                        I18n.tr("dlg.waiting") + "\n" + I18n.tr("dlg.yourAddress") + NetGame.getPublicIP(),
                        net::prepareHost, net::cancelHost);
                if (!net.isConnected() && !net.hostCancelled()) {
                    Dialogs.warn(I18n.tr("msg.connFailed"));
                }
            } else {
                Dialogs.waitWhile(
                        I18n.tr("dlg.connecting"),
                        () -> net.prepareJoin(settings.joinAddress()), net::cancelJoin);
                if (!net.isConnected()) {
                    Dialogs.warn(I18n.tr("msg.noHost"));
                }
            }
            if (net.isConnected()) {
                Platform.runLater(() -> {
                    gameView = view;
                    setContent(view);
                    view.beginPlay();
                });
            } else {
                Platform.runLater(this::showMenu);
            }
        }, "online-launch");
        launch.setDaemon(true);
        launch.start();
    }

    /** 主题重新取色后：换壁纸、换样式表，正在下的那局也跟着重画。 */
    public void applyTheme() {
        background.setImage(theme.background());
        scene.getStylesheets().clear();
        String css = theme.stylesheet();
        if (css != null) scene.getStylesheets().add(css);
        root.setStyle("-fx-background-color: " + Theme.hex(theme.surface()) + ";");
        if (gameView != null) {
            gameView.paint();
            gameView.board().setTheme(theme);
        }
        // 卡片底色是构建时算好的内联 hex，不重建界面不会跟着换主题；对局界面走上面的 paint()，重建会丢棋局。
        if (currentView instanceof SettingsView) showSettings(settings);
        else if (currentView instanceof MenuView) setContent(new MenuView(settings, theme, this));
    }

    public void loadGame(GameController controller) {
        File file = chooseSave("file.openSaved");
        if (file != null) controller.loadFromFile(file);
    }

    public void saveGame(GameController controller) {
        File file = chooseSave("file.saveGame");
        if (file != null) controller.saveToFile(file);
    }

    public void exit() {
        Platform.exit();
    }

    /** 切换语言后重画当前界面：对局原地换文案，其余界面重建。 */
    public void setLocale(Locale locale) {
        I18n.setLocale(locale);
        if (currentView instanceof GameView game) game.retranslate();
        else if (currentView instanceof SettingsView) showSettings(settings);
        else if (currentView instanceof MenuView) showMenu();
    }

    private File chooseSave(String titleKey) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.tr(titleKey));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.tr("file.savedFilter"), "*.txt"));
        return chooser.showSaveDialog(stage);
    }

    private void setContent(Node content) {
        currentView = content;
        if (root.getChildren().size() > 1) root.getChildren().set(1, content);
        else root.getChildren().add(content);
    }

    /** 壁纸覆盖式铺满：按比例取宽高缩放比较大者，溢出窗口的部分裁掉，不拉伸变形。 */
    private void bindCover(ImageView view, Region container) {
        var dims = Bindings.createObjectBinding(() -> {
            Image image = view.getImage();
            double w = container.getWidth(), h = container.getHeight();
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || w <= 0 || h <= 0) {
                return new double[]{w, h};
            }
            double scale = Math.max(w / image.getWidth(), h / image.getHeight());
            return new double[]{image.getWidth() * scale, image.getHeight() * scale};
        }, view.imageProperty(), container.widthProperty(), container.heightProperty());
        view.fitWidthProperty().bind(Bindings.createDoubleBinding(() -> dims.get()[0], dims));
        view.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> dims.get()[1], dims));
    }

    private void setStageIcon(Stage stage) {
        try {
            File icon = ResourceRoot.path("icon/app.png").toFile();
            if (icon.exists()) stage.getIcons().add(new Image(icon.toURI().toString()));
        } catch (RuntimeException _) {
            // 没有图标文件就不设，不影响启动
        }
    }
}
