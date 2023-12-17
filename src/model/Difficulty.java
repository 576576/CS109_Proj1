package model;

public enum Difficulty {
    EASY(30,-1,-1),NORMAL(100,30,180),HARD(180,46,180);
    private final int goal,stepLimit,timeLimit;
    Difficulty(int goal,int stepLimit,int timeLimit){
        this.goal=goal;
        this.stepLimit=stepLimit;
        this.timeLimit=timeLimit;
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
