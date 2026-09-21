package config;

import model.Difficulty;
import model.DifficultyPreset;

import java.io.File;

/**
 * 开局前在菜单里做出的全部选择。
 * 原先这些字段是 view 包里几个 public static，被 controller 静态导入后直接赋值，
 * 于是模型/控制器的一举一动都依赖着界面类的静态状态。现在由菜单持有并显式传下去。
 */
public class GameSettings {

    private Difficulty difficulty = DifficultyPreset.EASY.difficulty();
    private PlayMode playMode = PlayMode.NONE;
    private boolean verboseDialogs;
    private boolean autoRestart = true;
    private File saveFile;
    /** 加入游戏时对手房间的地址，从主界面内联文本框取得，不弹窗询问。 */
    private String joinAddress = "";

    public Difficulty difficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public PlayMode playMode() {
        return playMode;
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
    }

    /** 是否每一步都弹提示框。关掉后只在胜负这种关键时刻弹。 */
    public boolean verboseDialogs() {
        return verboseDialogs;
    }

    public void setVerboseDialogs(boolean verboseDialogs) {
        this.verboseDialogs = verboseDialogs;
    }

    public boolean autoRestart() {
        return autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    /** 读档模式下玩家选中的存档；没选则为 null。 */
    public File saveFile() {
        return saveFile;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    /** 加入游戏时对手房间的地址。 */
    public String joinAddress() {
        return joinAddress;
    }

    public void setJoinAddress(String joinAddress) {
        this.joinAddress = joinAddress;
    }
}
