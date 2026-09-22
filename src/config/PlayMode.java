package config;

/** 一局游戏从哪儿起步。 */
public enum PlayMode {
    /** 还没选模式。 */
    NONE,
    /** 本地开新局。 */
    NEW_LOCAL,
    /** 本地读档。 */
    LOAD_LOCAL,
    /** 建房等对手。 */
    HOST,
    /** 加入对手的房间。 */
    JOIN;

    public boolean isOnline() {
        return this == HOST || this == JOIN;
    }
}
