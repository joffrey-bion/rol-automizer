package com.jbion.riseoflords;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;

import com.jbion.riseoflords.config.Config;
import com.jbion.riseoflords.config.Config.BadConfigException;
import com.jbion.riseoflords.util.Log;

public class Main {

    private static final String TAG = Main.class.getSimpleName();

    private static final String DEFAULT_PROP_FILE = "default.rol";

    private static final long ONE_SECOND_IN_MILLIS = 1000;
    private static final long ONE_MINUTE_IN_MILLIS = 60 * ONE_SECOND_IN_MILLIS;
    private static final long ONE_HOUR_IN_MILLIS = 60 * ONE_MINUTE_IN_MILLIS;

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            Log.get().e(TAG, "\nUNCAUGHT EXCEPTION: ", e.getMessage());
            e.printStackTrace(System.err);
        }
        waitForEnter(null);
    }

    public static void launch(String[] args) {
        String filename = args.length > 0 ? args[0] : DEFAULT_PROP_FILE;

        Config config;
        try {
            config = Config.loadFromFile(filename);
        } catch (BadConfigException e) {
            Log.get().e(TAG, "Error reading config file: ", e.getMessage());
            return;
        } catch (IOException e) {
            try {
                config = Config.loadFromResource(filename);
            } catch (IOException | BadConfigException e2) {
                Log.get().e(TAG, "Error reading config file: ", e.getMessage());
                return;
            }
        }
        Log.get().title(TAG, "CONFIG");
        Log.get().i(TAG, config);
        System.out.println();

        Sequence sequence = new Sequence(config);
        for (int i = 0; i < config.getNbOfAttacks(); i++) {
            Log.get().title(TAG, String.format("ATTACK SESSION %d/%d", i + 1, config.getNbOfAttacks()));
            sequence.start();
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

    public static void waitForEnter(String message, Object... args) {
        Console c = System.console();
        if (c != null) {
            if (message != null) {
                c.format(message, args);
            } else {
                c.format("\nPress ENTER to exit.\n");
            }
            c.readLine();
        }
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
