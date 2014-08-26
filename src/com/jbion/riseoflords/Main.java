package com.jbion.riseoflords;

import java.io.IOException;

import com.jbion.riseoflords.config.Config;
import com.jbion.riseoflords.config.Config.BadConfigException;
import com.jbion.riseoflords.util.Log;

public class Main {

    private static final String TAG = Main.class.getSimpleName();

    private static final boolean DEBUG = true;
    private static final String PROP_FILE = DEBUG ? "/internal-bot.properties" : "/bot.properties";

    private static final long ONE_SECOND_IN_MILLIS = 1000;
    private static final long ONE_MINUTE_IN_MILLIS = 60 * ONE_SECOND_IN_MILLIS;
    private static final long ONE_HOUR_IN_MILLIS = 60 * ONE_MINUTE_IN_MILLIS;

    public static void main(String[] args) {
        System.out.println();

        Config config;
        try {
            config = Config.load(PROP_FILE);
        } catch (IOException | BadConfigException e) {
            Log.get().e(TAG, "Error reading config file: ", e.getMessage());
            return;
        }
        Log.get().title(TAG, "CONFIG");
        Log.get().i(TAG, config);
        System.out.println();

        Sequence sequence = new Sequence(config);
        for (int i = 0; i < config.getNbOfAttacks(); i++) {
            Log.get().title(TAG, String.format("ATTACK SESSION %d/%d", i + 1, config.getNbOfAttacks()));
            sequence.start();
            if (sequence.getCurrentState().turns == 0) {
                Log.get().w(TAG, "No more turns to spend, next attacks aborted.");
                return;
            }
            if (i + 1 < config.getNbOfAttacks()) {
                // more attacks are waiting
                System.out.println();
                Log.get().title(TAG, "SLEEP");
                sleepWithIndications(config.getTimeBetweenAttacks());
            }
            System.out.println();
        }
        Log.get().i(TAG, "End of attacks.");
    }

    private static void sleepWithIndications(long durationInMillis) {
        long hours = durationInMillis / ONE_HOUR_IN_MILLIS;
        long minutes = (durationInMillis % ONE_HOUR_IN_MILLIS) / ONE_MINUTE_IN_MILLIS;
        long seconds = (durationInMillis % ONE_MINUTE_IN_MILLIS) / ONE_SECOND_IN_MILLIS;
        long millis = durationInMillis % ONE_SECOND_IN_MILLIS;
        try {
            // sleeping the first bit to round to the minute
            Thread.sleep(millis + ONE_SECOND_IN_MILLIS * seconds);
            Log.get()
                    .i(TAG,
                            String.format("Waiting for %d:%02d:%02d before the next attack session...", hours, minutes,
                                    seconds));
            Log.get().indent();
            long totalMinutes = minutes + 60 * hours;
            while (totalMinutes > 0) {
                Log.get().i(TAG, totalMinutes, " min left...");
                Thread.sleep(ONE_MINUTE_IN_MILLIS);
                totalMinutes--;
            }
            Log.get().deindent(1);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted. Session aborted.");
            return;
        }
    }
}
