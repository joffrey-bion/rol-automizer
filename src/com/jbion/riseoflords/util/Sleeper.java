package com.jbion.riseoflords.util;

import java.util.Random;

public class Sleeper {
    
    private static final String TAG = Sleeper.class.getSimpleName();
    
    public static enum Speed {
        NORMAL(1000),
        SLOW(1500),
        REALLY_SLOW(2000);
        
        private final int factor;
        
        private Speed(int factor) {
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
    
    private void sleep(int millis) {
        try {
            int affectedMillis = speed.affect(millis);
            log.d(TAG, "    ...  ", affectedMillis, " ms  ...");
            Thread.sleep(affectedMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(int minMillis, int maxMillis) {
        int duration = rand.nextInt(maxMillis - minMillis + 1) + minMillis;
        sleep(duration);
    }
    
    public void actionInPage() {
        sleep(600, 1000);
    }
    
    public void changePage() {
        sleep(900, 1500);
    }
    
    public void changePageLong() {
        sleep(1500, 2000);
    }
    
    public void readPage() {
        sleep(1500, 3000);
    }
    
    public void pauseWhenSafe() {
        sleep(2000, 4000);
    }
    
    public void waitAfterLogin() {
        sleep(6000, 7000);
    }
}
