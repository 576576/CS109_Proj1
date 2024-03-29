package model;

import java.util.Objects;

public class Difficulty {
    private final int goal,stepLimit,timeLimit;
    private String name;
    public Difficulty(int goal, int stepLimit, int timeLimit, String name){
        this.goal=goal;
        this.stepLimit=stepLimit;
        this.timeLimit=timeLimit;
        if (this.equals(DifficultyPreset.EASY)) this.name = DifficultyPreset.EASY.name();
        else if (this.equals(DifficultyPreset.NORMAL)) this.name = DifficultyPreset.NORMAL.name();
        else if (this.equals(DifficultyPreset.HARD)) this.name = DifficultyPreset.HARD.name();
        else this.name=name;

    }
    public Difficulty(int goal, int stepLimit, int timeLimit){
        this(goal,stepLimit,timeLimit,"CUSTOM");
    }
    public Difficulty(DifficultyPreset dPreset){
        this.goal=dPreset.goal;
        this.stepLimit=dPreset.stepLimit;
        this.timeLimit=dPreset.timeLimit;
        name=dPreset.name();
    }
    public int getGoal() {
        return goal;
    }

    public int getStepLimit() {
        return stepLimit;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public String getName() {
        return name;
    }

    public String getDifficultyInfo(){
        return String.format("%d %d %d",goal,stepLimit,timeLimit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Difficulty that = (Difficulty) o;
        return goal == that.goal && stepLimit == that.stepLimit && timeLimit == that.timeLimit;
    }
    public boolean equals(DifficultyPreset dPreset){
        return this.equals(new Difficulty(dPreset));
    }

    @Override
    public int hashCode() {
        return Objects.hash(goal, stepLimit, timeLimit);
    }
}
