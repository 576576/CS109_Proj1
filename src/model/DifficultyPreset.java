package model;

/** 三档预设难度。数值含义见 {@link Difficulty}。 */
public enum DifficultyPreset {
    EASY(30, -1, -1),
    NORMAL(50, 30, 180),
    HARD(180, 46, 180);

    private final int goal;
    private final int stepLimit;
    private final int timeLimit;

    DifficultyPreset(int goal, int stepLimit, int timeLimit) {
        this.goal = goal;
        this.stepLimit = stepLimit;
        this.timeLimit = timeLimit;
    }

    public Difficulty difficulty() {
        return new Difficulty(name(), goal, stepLimit, timeLimit);
    }

    /** 旧存档只写了三个数值，这里据此找回预设名，找不回就按自定义处理。 */
    public static Difficulty matching(int goal, int stepLimit, int timeLimit) {
        for (var preset : values()) {
            var difficulty = preset.difficulty();
            if (difficulty.goal() == goal && difficulty.stepLimit() == stepLimit && difficulty.timeLimit() == timeLimit) {
                return difficulty;
            }
        }
        return new Difficulty(goal, stepLimit, timeLimit);
    }
}
