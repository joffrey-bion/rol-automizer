package com.jbion.riseoflords.util;

public class Debug {
    /**
     * Used to simulate a slow WS access. Set it to {@code true} to make every
     * request slow.
     */
    private static final boolean TEST_SLOW_WS = false;
    /**
     * The delay in milliseconds added to the requests when {@link #TEST_SLOW_WS} is
     * {@code true}.
     */
    private static final long TEST_SLOW_WS_DELAY_MILLIS = 4000;

    /**
     * Used to simulate a slow database. Set it to {@code true} to make every query
     * slow.
     */
    private static final boolean TEST_SLOW_DB = false;
    /**
     * The delay in milliseconds added to the requests when {@link #TEST_SLOW_DB} is
     * {@code true}.
     */
    private static final long TEST_SLOW_DB_DELAY_MILLIS = 3000;

    /**
     * Go to sleep for some time if slow DB test is enabled.
     */
    public static void testSlowDB() {
        sleep(TEST_SLOW_DB, TEST_SLOW_DB_DELAY_MILLIS);
    }

    /**
     * Go to sleep for some time if slow WS test is enabled.
     */
    public static void testSlowWS() {
        sleep(TEST_SLOW_WS, TEST_SLOW_WS_DELAY_MILLIS);
    }

    /**
     * Go to sleep for the specified time.
     *
     * @param doSleep
     *            if {@code false}, nothing is done
     * @param sleepTimeMillis
     *            the time to sleep in milliseconds if the test is enabled
     */
    private static void sleep(boolean doSleep, long sleepTimeMillis) {
        if (doSleep) {
            try {
                Thread.sleep(sleepTimeMillis);
            } catch (final InterruptedException e) {
                // restore interrupted status
                Thread.currentThread().interrupt();
            }
        }
    }
}
