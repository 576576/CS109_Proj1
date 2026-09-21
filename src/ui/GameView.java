package ui;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import model.Board;
import player.MusicLibrary;
import player.MusicPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * 对局界面：左菜单放状态与对局操作，右菜单放存档与退出，中间棋盘完全居中。
 * 左右菜单各有背景面板，是两块整块的菜单，而不是散落的按钮。
 * 状态信息单独包在一个内嵌卡片里；Load/Save 合成一个背靠背的分段按钮。
 */
public class GameView extends BorderPane implements GameScreen {

    private static final int ONE_CHESS_SIZE = 72;
    private static final double MENU_WIDTH = 172;

    private final GameSettings settings;
    private final Theme theme;
    private final Match3App app;
    private final BoardView board;
    private final Label[] status = new Label[4];
    private final VBox leftMenu = new VBox(8);
    private final VBox rightMenu = new VBox(8);
    private final List<MFXButton> buttons = new ArrayList<>();
    private final MusicPlayer musicPlayer = new MusicPlayer();
    private final Thread musicThread;
    private final MusicLibrary music;

    private GameController controller;

    private VBox statusCard;
    private Label statusHeading;
    private HBox loadSaveBox;
    private Region loadSaveDivider;
    private final StackPane[] loadSaveHalves = new StackPane[2];
    private MFXButton autoButton;
    private MFXButton confirmButton;
    private boolean autoOn;
    private boolean confirmOn;
    private final Map<MFXButton, String> buttonKeys = new LinkedHashMap<>();
    private final Map<StackPane, String> halfKeys = new LinkedHashMap<>();

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
        status[0].setText(I18n.tr("game.difficulty") + "  " + difficulty);
        status[1].setText(I18n.tr("game.score") + "  " + score);
        status[2].setText(I18n.tr("game.steps") + "  " + steps);
        status[3].setText(I18n.tr("game.time") + "  " + time);
    }

    /** 切换语言后原地换文案，不用重建整局。 */
    public void retranslate() {
        statusHeading.setText(I18n.tr("game.status"));
        for (var entry : buttonKeys.entrySet()) entry.getKey().setText(I18n.tr(entry.getValue()));
        for (var entry : halfKeys.entrySet()) {
            for (Node child : entry.getKey().getChildren()) {
                if (child instanceof Label label) label.setText(I18n.tr(entry.getValue()));
            }
        }
        if (controller != null) controller.refreshStatus();
    }

    @Override
    public void finish() {
        musicPlayer.stop();
        app.showMenu();
    }

    /** 一块菜单面板：有背景、圆角，按钮在里面等宽铺满。 */
    private void styleMenu(VBox menu) {
        menu.setPadding(new Insets(12));
        menu.setAlignment(Pos.TOP_CENTER);
        menu.setFillWidth(true);
        menu.setStyle("-fx-background-color: " + Theme.hex(theme.surface())
                + "; -fx-background-radius: 18;");
    }

    private void buildStatus() {
        statusCard = new VBox(6);
        statusCard.setPadding(new Insets(10));
        statusCard.setStyle("-fx-background-color: " + Theme.hex(theme.surfaceVariant())
                + "; -fx-background-radius: 12;");
        statusHeading = new Label(I18n.tr("game.status"));
        statusHeading.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant())
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        statusCard.getChildren().add(statusHeading);
        for (int i = 0; i < status.length; i++) {
            status[i] = new Label("-");
            status[i].setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");
            statusCard.getChildren().add(status[i]);
        }
        leftMenu.getChildren().add(statusCard);
    }

    private void buildLocalControls() {
        leftMenu.getChildren().addAll(
                action("game.hint", e -> controller.hint()),
                action("game.shuffle", e -> controller.onPlayerShuffle()),
                autoToggle(),
                confirmToggle(),
                action("game.confirmSwap", e -> controller.onPlayerSwapChess()),
                action("game.nextStep", e -> controller.nextStep()));
        rightMenu.getChildren().addAll(
                action("game.startNew", e -> controller.initialize()),
                loadSaveSplit(),
                action("menu.settings", e -> app.showSettings(settings)),
                action("game.returnTitle", e -> controller.terminate()),
                action("menu.exit", e -> app.exit()));
    }

    private void buildOnlineControls() {
        leftMenu.getChildren().add(action("game.shuffle", e -> controller.onPlayerShuffle()));
        rightMenu.getChildren().addAll(
                action("game.confirmSwap", e -> controller.onPlayerSwapChess()),
                action("game.nextStep", e -> controller.nextStep()),
                action("menu.settings", e -> app.showSettings(settings)),
                action("game.disconnect", e -> controller.terminate()),
                action("menu.exit", e -> app.exit()));
    }

    /** Auto Play 开关：开=实心主色，关=描边幽灵态，靠颜色而非文字表示状态。 */
    private MFXButton autoToggle() {
        MFXButton button = new MFXButton(I18n.tr("game.autoPlay"));
        autoButton = button;
        buttonKeys.put(button, "game.autoPlay");
        Styles.toggle(button, theme, false);
        button.setOnAction(e -> {
            autoOn = !controller.isAutoMode();
            controller.setAutoMode(autoOn);
            Styles.toggle(button, theme, autoOn);
        });
        return button;
    }

    /** Auto Confirm 开关：同样用颜色差异表示状态。 */
    private MFXButton confirmToggle() {
        MFXButton button = new MFXButton(I18n.tr("game.autoConfirm"));
        confirmButton = button;
        buttonKeys.put(button, "game.autoConfirm");
        Styles.toggle(button, theme, false);
        button.setOnAction(e -> {
            confirmOn = !controller.isAutoConfirm();
            controller.setAutoConfirm(confirmOn);
            Styles.toggle(button, theme, confirmOn);
        });
        return button;
    }

    /**
     * Load/Save 合成一个圆角分段按钮：左半 Load、右半 Save，中间一条竖线当分隔。
     * 两半各自高亮、各自响应点击，视觉上是一个整体而不是两个按钮。
     */
    private HBox loadSaveSplit() {
        loadSaveBox = new HBox();
        loadSaveBox.setMaxWidth(Double.MAX_VALUE);
        loadSaveBox.setAlignment(Pos.CENTER);
        loadSaveBox.setStyle("-fx-background-color: " + Theme.hex(theme.primaryContainer())
                + "; -fx-background-radius: 16; -fx-padding: 0;");

        loadSaveHalves[0] = splitHalf("game.load", e -> app.loadGame(controller));
        loadSaveHalves[1] = splitHalf("game.save", e -> app.saveGame(controller));

        loadSaveDivider = new Region();
        loadSaveDivider.setPrefWidth(1.5);
        loadSaveDivider.setStyle("-fx-background-color: " + Theme.hex(theme.onPrimaryContainer())
                + "; -fx-opacity: 0.35;");

        HBox.setHgrow(loadSaveHalves[0], Priority.ALWAYS);
        HBox.setHgrow(loadSaveHalves[1], Priority.ALWAYS);
        loadSaveBox.getChildren().addAll(loadSaveHalves[0], loadSaveDivider, loadSaveHalves[1]);
        return loadSaveBox;
    }

    private StackPane splitHalf(String key, javafx.event.EventHandler<MouseEvent> handler) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setPrefHeight(36);
        pane.setCursor(Cursor.HAND);
        halfKeys.put(pane, key);
        Label label = new Label(I18n.tr(key));
        label.setStyle("-fx-text-fill: " + Theme.hex(theme.onPrimaryContainer()) + "; -fx-font-size: 13px;");
        pane.getChildren().add(label);
        pane.setOnMouseEntered(e -> pane.setStyle("-fx-background-color: " + Theme.hexA(theme.onPrimaryContainer(), 0.14) + ";"));
        pane.setOnMouseExited(e -> pane.setStyle(""));
        pane.setOnMouseClicked(handler);
        return pane;
    }

    /** 按钮文案一律走 i18n key，切换语言时靠 buttonKeys 原地重刷。 */
    private MFXButton action(String key, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MFXButton button = new MFXButton(I18n.tr(key));
        buttonKeys.put(button, key);
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

    /** 主题变了重刷一遍颜色。 */
    public void paint() {
        if (theme.background() != null) {
            setStyle("-fx-background-color: transparent;");
        } else {
            setStyle("-fx-background-color: " + Theme.hex(theme.surface()) + ";");
        }
        styleMenu(leftMenu);
        styleMenu(rightMenu);
        if (statusCard != null) {
            statusCard.setStyle("-fx-background-color: " + Theme.hex(theme.surfaceVariant()) + "; -fx-background-radius: 12;");
        }
        if (statusHeading != null) {
            statusHeading.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant()) + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        }
        for (Label label : status) {
            label.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 15px;");
        }
        Styles.refresh(buttons, theme);
        if (autoButton != null) Styles.toggle(autoButton, theme, autoOn);
        if (confirmButton != null) Styles.toggle(confirmButton, theme, confirmOn);
        repaintLoadSave();
    }

    private void repaintLoadSave() {
        if (loadSaveBox == null) return;
        loadSaveBox.setStyle("-fx-background-color: " + Theme.hex(theme.primaryContainer())
                + "; -fx-background-radius: 16; -fx-padding: 0;");
        if (loadSaveDivider != null) {
            loadSaveDivider.setStyle("-fx-background-color: " + Theme.hex(theme.onPrimaryContainer()) + "; -fx-opacity: 0.35;");
        }
        for (StackPane half : loadSaveHalves) {
            if (half == null) continue;
            half.setStyle("");
            for (Node child : half.getChildren()) {
                if (child instanceof Label label) {
                    label.setStyle("-fx-text-fill: " + Theme.hex(theme.onPrimaryContainer()) + "; -fx-font-size: 13px;");
                }
            }
        }
    }

}
