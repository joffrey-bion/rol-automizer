package org.hildan.bots.riseoflords.util;

import java.util.Random;

public class Sleeper {

    private static final String TAG = Sleeper.class.getSimpleName();

    public enum Speed {
        INHUMAN(400),
        FAST(700),
        NORMAL(1000),
        SLOW(1500),
        REALLY_SLOW(2000);

        private final int factor;

        Speed(int factor) {
            this.factor = factor;
        }

        public int affect(int millis) {
            return millis * factor / 1000;
        }
    }

    private final Log log = Log.get();
    private final Random rand = new Random(System.currentTimeMillis());
    private final Speed speed;

    public Sleeper(Speed speed) {
        this.speed = speed;
    }

    private void sleep(int millis, boolean scaleDuration) {
        try {
            final int affectedMillis = scaleDuration ? speed.affect(millis) : millis;
            log.d(TAG, "    ...  ", affectedMillis, " ms  ...");
            Thread.sleep(affectedMillis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(int minMillis, int maxMillis, boolean scaleDuration) {
        final int duration = rand.nextInt(maxMillis - minMillis + 1) + minMillis;
        sleep(duration, scaleDuration);
    }

    private void sleep(int minMillis, int maxMillis) {
        sleep(minMillis, maxMillis, true);
    }

    public void actionInPage() {
        sleep(600, 1000);
    }

    public void changePage() {
        sleep(900, 1500);
    }

    public void changePageLong() {
        sleep(1000, 2000);
    }

    public void readPage() {
        sleep(1200, 2500);
    }

    public void pauseWhenSafe() {
        sleep(2000, 3000);
    }

    public void waitAfterLogin() {
        sleep(6000, 7000, false);
    }
}
