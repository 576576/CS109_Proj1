package model;

public enum DifficultyPreset {
    EASY(30,-1,-1),NORMAL(50,30,180),HARD(180,46,180);
    public final int goal,stepLimit,timeLimit;
    DifficultyPreset(int goal, int stepLimit, int timeLimit){
        this.goal=goal;
        this.stepLimit=stepLimit;
        this.timeLimit=timeLimit;
    }
}
