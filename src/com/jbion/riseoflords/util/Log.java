package com.jbion.riseoflords.util;

public class Log {

    private static final boolean DISPLAY_LEVEL = false;
    private static final boolean DISPLAY_TAG = false;
    private static final Level LEVEL = Level.INFO;

    private static final String INDENT = "   ";

    private static final Log log = new Log();

    public static Log get() {
        return log;
    }

    public static enum Level {
        WTF("WTF", true),
        ERROR("E", true),
        WARN("W", true),
        INFO("I", false),
        DEBUG("D", false),
        VERBOSE("V", false);

        private final String shortName;
        private final boolean isError;

        private Level(String shortName, boolean isError) {
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

    public void log(Level level, String tag, Object... message) {
        if (LEVEL.compareTo(level) < 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (DISPLAY_LEVEL) {
            sb.append(level.getShortName()).append(" ");
        }
        if (DISPLAY_TAG) {
            sb.append("[").append(tag).append("] ");
        }
        for (Object msg : message) {
            sb.append(msg);
        }
        String msg = indent(sb.toString());
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
        log(Level.VERBOSE, tag, message);
    }

    public void d(String tag, Object... message) {
        log(Level.DEBUG, tag, message);
    }

    public void i(String tag, Object... message) {
        log(Level.INFO, tag, message);
    }

    public void w(String tag, Object... message) {
        log(Level.WARN, tag, message);
    }

    public void e(String tag, Object... message) {
        log(Level.ERROR, tag, message);
    }

    public void wtf(String tag, Object... message) {
        log(Level.WTF, tag, message);
    }
}
