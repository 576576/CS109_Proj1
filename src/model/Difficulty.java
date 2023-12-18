package model;

public class Difficulty {
    private final int goal,stepLimit,timeLimit;
    Difficulty(int goal, int stepLimit, int timeLimit){
        this.goal=goal;
        this.stepLimit=stepLimit;
        this.timeLimit=timeLimit;
    }
    Difficulty(DifficultyPreset dPreset){
        this.goal=dPreset.goal;
        this.stepLimit=dPreset.stepLimit;
        this.timeLimit=dPreset.timeLimit;
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
    public String getDifficultyInfo(){
        return String.format("%d %d %d",goal,stepLimit,timeLimit);
    }
}
