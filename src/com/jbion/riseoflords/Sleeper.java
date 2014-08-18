package com.jbion.riseoflords;

import java.util.Random;

import com.jbion.riseoflords.util.Log;

public class Sleeper {
    
    private static final String TAG = Sleeper.class.getSimpleName();

    private final Log log = Log.get();
    private final Random rand = new Random(System.currentTimeMillis());

    public void sleep(long millis) {
        try {
            log.d(TAG, "Sleeping ", millis, " ms...");
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sleep(int minMillis, int maxMillis) {
        int duration = rand.nextInt(maxMillis - minMillis + 1) + minMillis;
        sleep(duration);
    }
}
