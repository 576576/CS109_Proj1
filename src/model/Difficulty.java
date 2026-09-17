package model;

/**
 * 一局游戏的过关条件。步数或时间为 {@link #UNLIMITED} 时表示不作限制。
 */
public record Difficulty(String name, int goal, int stepLimit, int timeLimit) {
    public static final int UNLIMITED = -1;

    public Difficulty(int goal, int stepLimit, int timeLimit) {
        this("CUSTOM", goal, stepLimit, timeLimit);
    }

    public Difficulty {
        if (name == null || name.isBlank()) name = "CUSTOM";
    }

    public boolean hasStepLimit() {
        return stepLimit > UNLIMITED;
    }

    public boolean hasTimeLimit() {
        return timeLimit > UNLIMITED;
    }
}
