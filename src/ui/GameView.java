package ui;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Board;
import player.MusicLibrary;
import player.MusicPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 对局界面：左菜单放状态与对局操作，右菜单放存档与退出，中间棋盘完全居中。
 * 左右菜单各有背景面板，是两块整块的菜单，而不是散落的按钮。
 */
public class GameView extends BorderPane implements GameScreen {

    private static final int ONE_CHESS_SIZE = 72;
    private static final double MENU_WIDTH = 200;

    private final GameSettings settings;
    private final Theme theme;
    private final Match3App app;
    private final BoardView board;
    private final Label[] status = new Label[4];
    private final VBox leftMenu = new VBox(10);
    private final VBox rightMenu = new VBox(10);
    private final List<MFXButton> buttons = new ArrayList<>();
    private final MusicPlayer musicPlayer = new MusicPlayer();
    private final Thread musicThread;
    private final MusicLibrary music;

    private GameController controller;

    public GameView(GameSettings settings, Theme theme, Match3App app) {
        this.settings = settings;
        this.theme = theme;
        this.app = app;
        this.board = new BoardView(ONE_CHESS_SIZE, Board.DEFAULT_SIZE, Board.DEFAULT_SIZE);
        this.music = app.musicLibrary();
        this.musicThread = new Thread(this::playMusicInLoop, "music");

        board.setTheme(theme);
        setPadding(new Insets(12));

        // 左右等宽，棋盘才在窗口里真正居中
        leftMenu.setPrefWidth(MENU_WIDTH);
        rightMenu.setPrefWidth(MENU_WIDTH);
        styleMenu(leftMenu);
        styleMenu(rightMenu);

        setLeft(leftMenu);
        setRight(rightMenu);
        // BorderPane 默认把 center 拉满剩余空间，Canvas 会贴在左上角；
        // 限制棋盘为首选尺寸后它才真正按 576x576 在中间居中
        board.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setCenter(board);
        BorderPane.setAlignment(board, Pos.CENTER);
        BorderPane.setMargin(board, new Insets(0, 10, 0, 10));

        buildStatus();
        if (settings.playMode().isOnline()) buildOnlineControls();
        else buildLocalControls();
        paint();
    }

    @Override
    public BoardView board() {
        return board;
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public GameController controller() {
        return controller;
    }

    /** 控制器装好之后由 Match3App 调用：联机先握手，读档先读档，然后起计时与音乐。 */
    public void beginPlay() {
        switch (settings.playMode()) {
            case HOST -> controller.onPlayerHostGame();
            case JOIN -> controller.onPlayerJoinGame();
            case LOAD_LOCAL -> controller.loadFromFile(settings.saveFile());
            case NEW_LOCAL, NONE -> { }
        }
        controller.refreshStatus();
        controller.startTimer();
        musicThread.setDaemon(true);
        musicThread.start();
    }

    @Override
    public void setStatus(String difficulty, String score, String steps, String time) {
        status[0].setText("Difficulty  " + difficulty);
        status[1].setText("Score  " + score);
        status[2].setText("Steps  " + steps);
        status[3].setText("Time  " + time);
    }

    @Override
    public void finish() {
        musicPlayer.stop();
        if (settings.autoRestart()) app.showSetup(settings);
        else app.showMenu();
    }

    /** 一块菜单面板：有背景、圆角、标题感的间距。 */
    private void styleMenu(VBox menu) {
        menu.setPadding(new Insets(14));
        menu.setAlignment(Pos.TOP_CENTER);
        menu.setFillWidth(true);
        menu.setStyle(cardStyle(theme.surfaceVariant()));
    }

    private void buildStatus() {
        Label heading = new Label("Status");
        heading.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant())
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        leftMenu.getChildren().add(heading);
        for (int i = 0; i < status.length; i++) {
            status[i] = new Label("-");
            status[i].setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");
            leftMenu.getChildren().add(status[i]);
        }
    }

    private void buildLocalControls() {
        leftMenu.getChildren().addAll(
                action("Hint", e -> controller.hint()),
                action("Shuffle", e -> controller.onPlayerShuffle()),
                autoToggle(),
                confirmToggle(),
                action("Confirm Swap", e -> controller.onPlayerSwapChess()),
                action("Next Step", e -> controller.nextStep()));
        rightMenu.getChildren().addAll(
                action("Start New", e -> controller.initialize()),
                action("Load", e -> app.loadGame(controller)),
                action("Save", e -> app.saveGame(controller)),
                action("Settings", e -> app.showSettings(settings)),
                action("Return Title", e -> controller.terminate()),
                action("Exit", e -> app.exit()));
    }

    private void buildOnlineControls() {
        leftMenu.getChildren().add(action("Shuffle", e -> controller.onPlayerShuffle()));
        rightMenu.getChildren().addAll(
                action("Confirm Swap", e -> controller.onPlayerSwapChess()),
                action("Next Step", e -> controller.nextStep()),
                action("Settings", e -> app.showSettings(settings)),
                action("Disconnect", e -> controller.terminate()),
                action("Exit", e -> app.exit()));
    }

    /** Auto 开关按钮：文案跟着状态走。 */
    private MFXButton autoToggle() {
        MFXButton button = new MFXButton("Auto: OFF");
        Styles.button(button, theme);
        buttons.add(button);
        button.setOnAction(e -> {
            boolean on = !controller.isAutoMode();
            controller.setAutoMode(on);
            button.setText("Auto: " + (on ? "ON" : "OFF"));
        });
        return button;
    }

    /** 手点确认 / 自动确认：开着时选好两枚就自动走完这一步。 */
    private MFXButton confirmToggle() {
        MFXButton button = new MFXButton("Confirm: Hand");
        Styles.button(button, theme);
        buttons.add(button);
        button.setOnAction(e -> {
            boolean on = !controller.isAutoConfirm();
            controller.setAutoConfirm(on);
            button.setText("Confirm: " + (on ? "Auto" : "Hand"));
        });
        return button;
    }

    private MFXButton action(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MFXButton button = new MFXButton(text);
        Styles.button(button, theme);
        buttons.add(button);
        button.setOnAction(handler);
        return button;
    }

    private void playMusicInLoop() {
        var files = music.files();
        if (files.isEmpty()) return;
        int index = RandomGenerator.getDefault().nextInt(files.size());
        while (controller.isAlive()) {
            musicPlayer.play(files.get(index));
            index = (index + 1) % files.size();
        }
    }

    private String cardStyle(javafx.scene.paint.Color color) {
        return "-fx-background-color: " + Theme.hex(color) + "; -fx-background-radius: 18;";
    }

    /** 主题变了重刷一遍颜色。 */
    public void paint() {
        if (theme.background() != null) {
            setStyle("-fx-background-color: transparent;");
        } else {
            setStyle("-fx-background-color: " + Theme.hex(theme.surface()) + ";");
        }
        styleMenu(leftMenu);
        styleMenu(rightMenu);
        for (Label label : status) {
            label.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");
        }
        Styles.refresh(buttons, theme);
    }

}
