package ui;

/** 控制器对局面的全部要求：一块棋盘、四个状态位、以及收局时回到哪里。 */
public interface GameScreen {

    BoardView board();

    /** 难度 / 得分 / 步数 / 时间，四行状态。 */
    void setStatus(String difficulty, String score, String steps, String time);

    /** 收局。是否重开一局由界面自己按设置决定，控制器不关心。 */
    void finish();
}
