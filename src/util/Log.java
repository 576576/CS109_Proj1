package util;

import java.util.logging.Level;
import java.util.logging.Logger;

/** 全项目唯一的调试输出出口。 */
public final class Log {
    private static final Logger LOGGER = Logger.getLogger("match3");

    private Log() {
    }

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }
}
