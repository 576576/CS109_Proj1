package ui;

import config.GameSettings;
import config.PlayMode;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRadioButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.Difficulty;
import model.DifficultyPreset;

import java.io.File;

import util.Log;

/** 开局前的难度 / 联机角色 / 读档选择。 */
public class SetupView extends VBox {

    private final GameSettings settings;
    private final Theme theme;
    private final Match3App app;

    private final MFXTextField goalField = new MFXTextField(String.valueOf(DifficultyPreset.EASY.difficulty().goal()));
    private final MFXTextField stepField = new MFXTextField(String.valueOf(DifficultyPreset.EASY.difficulty().stepLimit()));
    private final MFXTextField timeField = new MFXTextField(String.valueOf(DifficultyPreset.EASY.difficulty().timeLimit()));

    public SetupView(GameSettings settings, Theme theme, Match3App app) {
        this.settings = settings;
        this.theme = theme;
        this.app = app;

        setAlignment(Pos.CENTER);
        setSpacing(14);
        setPadding(new Insets(32));
        setFillWidth(false);

        Label title = new Label("Select a difficulty");
        title.setStyle("-fx-text-fill: " + Theme.hex(theme.primary()) + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        getChildren().add(title);

        getChildren().add(presetRow());
        if (settings.playMode().isOnline()) getChildren().add(roleRow());
        else getChildren().addAll(customRow(), fileRow());

        var start = new MFXButton("Start");
        Styles.primary(start, theme);
        start.setMinWidth(220);
        start.setOnAction(e -> app.showGame(settings));
        getChildren().add(start);

        var back = new MFXButton("Back");
        Styles.button(back, theme);
        back.setMinWidth(220);
        back.setOnAction(e -> app.showMenu());
        getChildren().add(back);
    }

    private HBox presetRow() {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        for (DifficultyPreset preset : DifficultyPreset.values()) {
            MFXRadioButton button = new MFXRadioButton(preset.difficulty().name());
            button.setToggleGroup(group);
            button.setSelected(preset == DifficultyPreset.EASY);
            button.setOnAction(e -> settings.setDifficulty(preset.difficulty()));
            row.getChildren().add(button);
        }
        return row;
    }

    private HBox customRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        goalField.setPromptText("Goal");
        stepField.setPromptText("Steps (-1 = ∞)");
        timeField.setPromptText("Time (-1 = ∞)");
        var apply = new MFXButton("Use custom");
        Styles.button(apply, theme);
        apply.setOnAction(e -> applyCustom());
        row.getChildren().addAll(goalField, stepField, timeField, apply);
        return row;
    }

    private void applyCustom() {
        try {
            Difficulty created = new Difficulty(
                    Integer.parseInt(goalField.getText().trim()),
                    Integer.parseInt(stepField.getText().trim()),
                    Integer.parseInt(timeField.getText().trim()));
            settings.setDifficulty(created);
            Log.info("Difficulty Selected: " + created.name());
        } catch (NumberFormatException _) {
            ui.Dialogs.warn("Illegal difficulty, please check again!");
        }
    }

    private HBox roleRow() {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        var host = new MFXRadioButton("Host Game");
        host.setToggleGroup(group);
        host.setOnAction(e -> settings.setPlayMode(PlayMode.HOST));
        var join = new MFXRadioButton("Join Game");
        join.setToggleGroup(group);
        join.setSelected(true);
        join.setOnAction(e -> settings.setPlayMode(PlayMode.JOIN));
        row.getChildren().addAll(host, join);
        return row;
    }

    private HBox fileRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER);
        var load = new MFXButton("Load a saved game");
        Styles.button(load, theme);
        load.setOnAction(e -> chooseSave());
        row.getChildren().add(load);
        return row;
    }

    private void chooseSave() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open saved game");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Saved game", "*.txt"));
        File file = chooser.showOpenDialog(app.stage());
        if (file == null) return;
        settings.setSaveFile(file);
        settings.setPlayMode(PlayMode.LOAD_LOCAL);
    }
}
