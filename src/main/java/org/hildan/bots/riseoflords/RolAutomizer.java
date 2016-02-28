package org.hildan.bots.riseoflords;

import java.io.Console;
import java.io.IOException;
import java.time.Duration;

import org.hildan.bots.riseoflords.config.Config;
import org.hildan.bots.riseoflords.config.Config.BadConfigException;
import org.hildan.bots.riseoflords.sequencing.LoginException;
import org.hildan.bots.riseoflords.sequencing.Sequence;
import org.hildan.bots.riseoflords.util.Log;

public class RolAutomizer {

    private static final String TAG = RolAutomizer.class.getSimpleName();

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (LoginException e) {
            Log.get().e(TAG, "\nLogin failed for user ", e.getUsername());
        } catch (Exception e) {
            Log.get().e(TAG, "\nUNCAUGHT EXCEPTION: ", e.getMessage());
            e.printStackTrace(System.err);
        }
        waitForEnter(null);
    }

    public static void launch(String[] args) throws LoginException {
        if (args.length == 0) {
            System.out.println("No config file provided: you must provide a .rol file to open.");
            System.out.println();
            System.out.println("More info at https://github.com/joffrey-bion/rol-automizer");
            return;
        }
        final String filename = args[0];

        Config config;
        try {
            config = Config.loadFromFile(filename);
        } catch (final BadConfigException e) {
            Log.get().e(TAG, "Error reading config file: ", e.getMessage());
            return;
        } catch (final IOException e) {
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

        final Sequence sequence = new Sequence(config);
        for (int i = 0; config.unlimitedAttacks() || i < config.getNbOfAttacks(); i++) {
            Log.get().title(TAG, String.format("ATTACK SESSION %d/%d", i + 1, config.getNbOfAttacks()));
            sequence.start();
            if (config.unlimitedAttacks() || i + 1 < config.getNbOfAttacks()) {
                // more attacks are waiting
                System.out.println();
                waitForNextAttack(config.getTimeBetweenAttacks());
            }
        }
        System.out.println();
        Log.get().i(TAG, "End of attacks.");
    }

    public static void waitForEnter(String message, Object... args) {
        final Console c = System.console();
        if (c != null) {
            if (message != null) {
                c.format(message, args);
            } else {
                c.format("\nPress ENTER to exit.\n");
            }
            c.readLine();
        }
    }

    private static void waitForNextAttack(Duration duration) {
        try {
            long millis = duration.toMillis() % 1000;
            Thread.sleep(millis);
            duration = duration.minusMillis(millis);

            while (!duration.isZero() && !duration.isNegative()) {
                Thread.sleep(1000);
                duration = duration.minusSeconds(1);
                printDuration(duration);
            }
            System.out.print("\r");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printDuration(Duration d) {
        final long hours = d.toHours();
        final long minutes = d.minusHours(hours).toMinutes();
        final long seconds = d.minusHours(hours).minusMinutes(minutes).toMillis() / 1000;
        System.out.print("\r");
        System.out.print(String.format("   Next attack session in %s%02d:%02d...", (hours > 0 ? hours + ":" : ""),
                minutes, seconds));
    }
}
