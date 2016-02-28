package org.hildan.bots.riseoflords.util;

public class Log {

    private static final boolean DISPLAY_LEVEL = false;
    private static final boolean DISPLAY_TAG = false;
    private static final Level LEVEL = Level.INFO;

    private static final String INDENT = "   ";

    private static final Log log = new Log();

    public static Log get() {
        return log;
    }

    private enum Mode {
        CONSOLE,
        FILE
    }

    private enum Level {
        WTF("WTF", true),
        ERROR("E", true),
        WARN("W", true),
        INFO("I", false),
        DEBUG("D", false),
        VERBOSE("V", false);

        private final String shortName;
        private final boolean isError;

        Level(String shortName, boolean isError) {
            this.shortName = shortName;
            this.isError = isError;
        }

        public String getShortName() {
            return shortName;
        }

        public boolean isError() {
            return isError;
        }
    }

    private int logIndent = 0;

    public void indent() {
        logIndent++;
    }

    public void deindent(int i) {
        logIndent -= i;
    }

    private String indent(String msg) {
        String res = "";
        for (int i = 0; i < logIndent; i++) {
            res += INDENT;
        }
        return res + msg;
    }

    private void log(Mode mode, Level level, String tag, Object... message) {
        if (LEVEL.compareTo(level) < 0) {
            return;
        }
        if (mode == Mode.FILE) {
            return; // not handled yet
        }
        final StringBuilder sb = new StringBuilder();
        if (DISPLAY_LEVEL) {
            sb.append(level.getShortName()).append(" ");
        }
        if (DISPLAY_TAG) {
            sb.append("[").append(tag).append("] ");
        }
        for (final Object msg : message) {
            sb.append(msg);
        }
        final String msg = indent(sb.toString());
        if (level.isError()) {
            System.err.println(msg);
        } else {
            System.out.println(msg);
        }
    }

    public void title(String tag, String message) {
        System.out.println();
        i(tag, "*** ", message, " ***");
        System.out.println();
    }

    public void v(String tag, Object... message) {
        log(Mode.CONSOLE, Level.VERBOSE, tag, message);
    }

    public void d(String tag, Object... message) {
        log(Mode.CONSOLE, Level.DEBUG, tag, message);
    }

    public void i(String tag, Object... message) {
        log(Mode.CONSOLE, Level.INFO, tag, message);
    }

    public void w(String tag, Object... message) {
        log(Mode.CONSOLE, Level.WARN, tag, message);
    }

    public void e(String tag, Object... message) {
        log(Mode.CONSOLE, Level.ERROR, tag, message);
    }

    public void wtf(String tag, Object... message) {
        log(Mode.CONSOLE, Level.WTF, tag, message);
    }
}
