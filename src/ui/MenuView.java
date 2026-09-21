package ui;

import config.GameSettings;
import config.PlayMode;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Difficulty;
import model.DifficultyPreset;
import org.kordamp.ikonli.material2.Material2MZ;

import java.io.File;

/**
 * 主界面：把原来的菜单和开局设置合并成一个屏。
 * 上方是模式三选（单机 / 创建房间 / 加入游戏）外加右侧一个圆形感叹号样式的设置按钮；
 * 选择「加入游戏」时内联显示地址文本框（不弹窗）；
 * 下方是难度表格——竖标题为四种难度（容易→自定义），横标题为各项设定（目标/步数/限时），
 * 除「自定义」以外的设定项只读，只有自定义行可编辑。
 */
public class MenuView extends VBox {

    private enum Mode { SINGLE, HOST, JOIN }

    private static final double COL0 = 132;
    private static final double COL = 124;

    private final GameSettings settings;
    private final Theme theme;
    private final Match3App app;

    private final MFXButton singleBtn;
    private final MFXButton hostBtn;
    private final MFXButton joinBtn;
    private Mode mode = Mode.SINGLE;

    private final HBox addressBox;
    private final MFXTextField addressField = new MFXTextField();

    private final HBox[] rowBoxes = new HBox[4];
    private int selectedRow = 0;
    private final MFXTextField[] customFields = new MFXTextField[3];

    public MenuView(GameSettings settings, Theme theme, Match3App app) {
        this.settings = settings;
        this.theme = theme;
        this.app = app;

        setAlignment(Pos.CENTER);
        setSpacing(18);
        setFillWidth(false);
        // StackPane 会把子节点拉满整页；限成首选尺寸，scrim 卡才贴着内容而不是铺满窗口
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setStyle(Styles.scrimCard(theme));

        Label title = new Label(I18n.tr("menu.title"));
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 46px; -fx-font-weight: bold;");

        HBox modeRow = new HBox(12);
        modeRow.setAlignment(Pos.CENTER);
        singleBtn = modeButton(I18n.tr("menu.mode.single"));
        hostBtn = modeButton(I18n.tr("menu.mode.host"));
        joinBtn = modeButton(I18n.tr("menu.mode.join"));
        singleBtn.setOnAction(e -> selectMode(Mode.SINGLE));
        hostBtn.setOnAction(e -> selectMode(Mode.HOST));
        joinBtn.setOnAction(e -> selectMode(Mode.JOIN));
        modeRow.getChildren().addAll(singleBtn, hostBtn, joinBtn, settingsButton());

        addressField.setPromptText(I18n.tr("setup.address"));
        addressField.setPrefWidth(360);
        addressField.setFloatMode(FloatMode.DISABLED);
        addressField.setAlignment(Pos.CENTER);
        addressField.setStyle(addressField.getStyle() + " -fx-alignment: center;");
        addressBox = new HBox(10, addressField);
        addressBox.setAlignment(Pos.CENTER);
        addressBox.setVisible(false);
        addressBox.setManaged(false);

        Label section = new Label(I18n.tr("setup.section"));
        section.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant())
                + "; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 4 0 0 0;");

        VBox table = buildTable();

        var start = new MFXButton(I18n.tr("setup.start"));
        Styles.primary(start, theme);
        start.setMinWidth(220);
        start.setOnAction(e -> onStart());

        var load = new MFXButton(I18n.tr("setup.loadSaved"));
        Styles.button(load, theme);
        load.setMinWidth(160);
        load.setOnAction(e -> onLoad());
        var exit = new MFXButton(I18n.tr("menu.exit"));
        Styles.button(exit, theme);
        exit.setMinWidth(160);
        exit.setOnAction(e -> app.exit());
        HBox bottom = new HBox(12, load, exit);
        bottom.setAlignment(Pos.CENTER);

        getChildren().addAll(title, modeRow, addressBox, section, table, start, bottom);

        selectMode(Mode.SINGLE);
        selectRow(0);
    }

    /** 三选按钮：固定宽度，等宽并排。 */
    private MFXButton modeButton(String text) {
        MFXButton button = new MFXButton(text);
        button.setPrefWidth(150);
        button.setMinWidth(150);
        button.setMaxWidth(150);
        return button;
    }

    /** 右侧圆形感叹号样式的设置按钮。 */
    private MFXButton settingsButton() {
        MFXButton button = new MFXButton("", Icons.of(Material2MZ.PRIORITY_HIGH, theme.onPrimary(), 22));
        Styles.primary(button, theme);
        button.setStyle(button.getStyle()
                + " -fx-background-radius: 50%; -fx-min-width: 46; -fx-min-height: 46;"
                + " -fx-max-width: 46; -fx-max-height: 46; -fx-padding: 0;");
        button.setOnAction(e -> app.showSettings(settings));
        return button;
    }

    private void selectMode(Mode next) {
        mode = next;
        paintMode(singleBtn, next == Mode.SINGLE);
        paintMode(hostBtn, next == Mode.HOST);
        paintMode(joinBtn, next == Mode.JOIN);
        boolean join = next == Mode.JOIN;
        addressBox.setVisible(join);
        addressBox.setManaged(join);
    }

    private void paintMode(MFXButton button, boolean selected) {
        if (selected) {
            Styles.primary(button, theme);
        } else {
            button.setMaxWidth(150);
            button.setStyle("-fx-background-color: transparent;"
                    + "-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant()) + ";"
                    + "-fx-border-color: " + Theme.hex(theme.outline()) + ";"
                    + "-fx-border-width: 1.5;"
                    + "-fx-background-radius: 16; -fx-border-radius: 16;"
                    + "-fx-padding: 9 12 9 12; -fx-font-size: 13px; -fx-cursor: hand;");
        }
    }

    /** 难度表格：表头 + 三档预设（只读） + 自定义（可编辑）。 */
    private VBox buildTable() {
        VBox table = new VBox(6);
        table.setAlignment(Pos.CENTER);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER);
        header.getChildren().addAll(
                headerCell(I18n.tr("game.difficulty"), COL0),
                headerCell(I18n.tr("setup.goal"), COL),
                headerCell(I18n.tr("setup.steps"), COL),
                headerCell(I18n.tr("setup.time"), COL));
        table.getChildren().add(header);

        DifficultyPreset[] presets = DifficultyPreset.values();
        String[] names = {
                I18n.tr("setup.difficulty.easy"),
                I18n.tr("setup.difficulty.normal"),
                I18n.tr("setup.difficulty.hard")};
        for (int i = 0; i < presets.length; i++) {
            Difficulty d = presets[i].difficulty();
            HBox row = presetRow(names[i], d.goal(), d.stepLimit(), d.timeLimit());
            final int idx = i;
            row.setOnMouseClicked(e -> selectRow(idx));
            rowBoxes[i] = row;
            table.getChildren().add(row);
        }

        HBox custom = customRow();
        custom.setOnMouseClicked(e -> selectRow(3));
        rowBoxes[3] = custom;
        table.getChildren().add(custom);
        return table;
    }

    private HBox presetRow(String name, int goal, int step, int time) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8));
        row.getChildren().addAll(
                cell(label(name), COL0),
                cell(label(String.valueOf(goal)), COL),
                cell(label(String.valueOf(step)), COL),
                cell(label(String.valueOf(time)), COL));
        return row;
    }

    private HBox customRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8));
        Difficulty d = DifficultyPreset.EASY.difficulty();
        customFields[0] = field(String.valueOf(d.goal()));
        customFields[1] = field(String.valueOf(d.stepLimit()));
        customFields[2] = field(String.valueOf(d.timeLimit()));
        for (MFXTextField f : customFields) {
            f.textProperty().addListener((a, b, c) -> selectRow(3));
        }
        row.getChildren().addAll(
                cell(label(I18n.tr("setup.difficulty.custom")), COL0),
                cell(customFields[0], COL),
                cell(customFields[1], COL),
                cell(customFields[2], COL));
        return row;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurface()) + "; -fx-font-size: 14px;");
        return l;
    }

    private MFXTextField field(String text) {
        MFXTextField f = new MFXTextField(text);
        f.setFloatMode(FloatMode.DISABLED);
        f.setAlignment(Pos.CENTER);
        f.setStyle(f.getStyle() + " -fx-alignment: center;");
        return f;
    }

    private Node cell(Node content, double width) {
        StackPane pane = new StackPane(content);
        pane.setAlignment(Pos.CENTER);
        pane.setPrefWidth(width);
        pane.setMinWidth(width);
        pane.setMaxWidth(width);
        return pane;
    }

    private Node headerCell(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + Theme.hex(theme.onSurfaceVariant())
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        return cell(l, width);
    }

    private void selectRow(int idx) {
        selectedRow = idx;
        for (int i = 0; i < rowBoxes.length; i++) {
            boolean selected = i == idx;
            rowBoxes[i].setStyle(selected
                    ? "-fx-background-color: " + Theme.hex(theme.primaryContainer()) + "; -fx-background-radius: 12;"
                    : "-fx-background-color: " + Theme.hex(theme.surfaceVariant()) + "; -fx-background-radius: 12;");
        }
        boolean custom = idx == 3;
        for (MFXTextField f : customFields) f.setDisable(!custom);
    }

    private Difficulty currentDifficulty() {
        if (selectedRow < 3) return DifficultyPreset.values()[selectedRow].difficulty();
        try {
            return new Difficulty(
                    Integer.parseInt(customFields[0].getText().trim()),
                    Integer.parseInt(customFields[1].getText().trim()),
                    Integer.parseInt(customFields[2].getText().trim()));
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private void onStart() {
        Difficulty d = currentDifficulty();
        if (d == null) {
            ui.Dialogs.warn(I18n.tr("setup.badDifficulty"));
            return;
        }
        settings.setDifficulty(d);
        switch (mode) {
            case SINGLE -> settings.setPlayMode(PlayMode.NEW_LOCAL);
            case HOST -> settings.setPlayMode(PlayMode.HOST);
            case JOIN -> {
                String addr = addressField.getText().trim();
                if (addr.isEmpty()) {
                    ui.Dialogs.warn(I18n.tr("msg.invalidHost"));
                    return;
                }
                settings.setJoinAddress(addr);
                settings.setPlayMode(PlayMode.JOIN);
            }
        }
        app.showGame(settings);
    }

    private void onLoad() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.tr("file.openSaved"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.tr("file.savedFilter"), "*.txt"));
        File file = chooser.showOpenDialog(app.stage());
        if (file == null) return;
        settings.setSaveFile(file);
        settings.setPlayMode(PlayMode.LOAD_LOCAL);
        app.showGame(settings);
    }
}
