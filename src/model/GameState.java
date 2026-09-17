package model;

/** 一局游戏的完整状态：计分、计时、步数、难度与棋盘快照。 */
public record GameState(int score, int timeLeft, int stepLeft, Difficulty difficulty, BoardSnapshot board) {
}
