package com.jbion.riseoflords;

import java.util.Random;

public class Sleeper {

    private final Random rand = new Random(System.currentTimeMillis());

    public static void sleep(String prefix, long millis) {
        try {
            System.out.println(prefix + "Sleeping " + millis + " ms...");
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void sleep(int minMillis, int maxMillis) {
        sleep("", minMillis, maxMillis);
    }

    public void sleep(String prefix, int minMillis, int maxMillis) {
        int duration = rand.nextInt(maxMillis - minMillis + 1) + minMillis;
        sleep(prefix, duration);
    }
}
