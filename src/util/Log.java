package util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** 全项目唯一的调试输出出口。 */
public final class Log {

    /** 默认格式带 Locale 相关的月名和上下午（中文下是「9月21日」「下午」），固定成 Locale 无关的 ISO-8601。 */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);

    private static final Logger LOGGER = Logger.getLogger("match3");

    static {
        // ConsoleHandler 默认按平台编码写字（中文 Windows 是 GBK），终端按 UTF-8 解码即乱码。
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setFormatter(new LineFormatter());
                try {
                    handler.setEncoding("UTF-8");
                } catch (Exception ignored) {
                    // 编码不受支持就退回平台默认
                }
            }
        }
    }

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

    /** 一条一行：时间 级别: 消息，异常再附上堆栈。级别用 getName() 避免 getLocalizedName() 的中文。 */
    private static final class LineFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            String time = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(record.getMillis()), ZoneId.systemDefault()).format(STAMP);
            StringBuilder out = new StringBuilder()
                    .append(time).append(' ')
                    .append(record.getLevel().getName())
                    .append(": ")
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            if (record.getThrown() != null) {
                StringWriter trace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(trace));
                out.append(trace);
            }
            return out.toString();
        }
    }
}
