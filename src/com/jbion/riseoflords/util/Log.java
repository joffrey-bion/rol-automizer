package com.jbion.riseoflords.util;

public class Log {

    private static final Level LEVEL = Level.VERBOSE;

    public enum Level {
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

    public static void log(Level level, String tag, String... message) {
        if (LEVEL.compareTo(level) < 0) {
            return;
        }
        StringBuilder sb = new StringBuilder(level.getShortName());
        sb.append(" [").append(tag).append("]");
        for (String msg : message) {
            sb.append(msg);
        }
        if (level.isError()) {
            System.err.println(sb.toString());
        } else {
            System.out.println(sb.toString());
        }
    }

    public static void v(String tag, String... message) {
        log(Level.WARN, tag, message);
    }

    public static void d(String tag, String... message) {
        log(Level.DEBUG, tag, message);
    }

    public static void i(String tag, String... message) {
        log(Level.INFO, tag, message);
    }

    public static void w(String tag, String... message) {
        log(Level.WARN, tag, message);
    }

    public static void e(String tag, String... message) {
        log(Level.ERROR, tag, message);
    }

    public static void wtf(String tag, String... message) {
        log(Level.WTF, tag, message);
    }

}
