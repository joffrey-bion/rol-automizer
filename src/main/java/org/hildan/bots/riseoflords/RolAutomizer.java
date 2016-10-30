package org.hildan.bots.riseoflords;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import org.hildan.bots.riseoflords.config.Config;
import org.hildan.bots.riseoflords.config.Config.BadConfigException;
import org.hildan.bots.riseoflords.sequencing.LoginException;
import org.hildan.bots.riseoflords.sequencing.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RolAutomizer {

    private static final Logger logger = LoggerFactory.getLogger(RolAutomizer.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.error("No config file provided: you must provide a .rol file to open.\n"
                    + "More info at https://github.com/joffrey-bion/rol-automizer");
            return;
        }
        final String filename = args[0];

        Config config = loadConfig(filename);
        if (config == null) {
            return;
        }
        logger.info("Loaded config:\n{}", config.toString());

        final Sequence sequence = new Sequence(config);
        for (int i = 0; config.unlimitedAttacks() || i < config.getNbOfAttacks(); i++) {
            logger.info("Starting attack session {}/{}", i + 1, config.getNbOfAttacks());
            try {
                sequence.start();
            } catch (LoginException e) {
                logger.error("Login failed for user {}", e.getUsername());
            } catch (Exception e) {
                logger.error("UNCAUGHT EXCEPTION", e);
            }
            if (config.unlimitedAttacks() || i + 1 < config.getNbOfAttacks()) {
                // more attacks are waiting
                waitForNextAttack(config.getTimeBetweenAttacks());
            }
        }
        logger.info("End of attacks");
    }

    private static Config loadConfig(String filename) {
        try {
            return Config.loadFromFile(filename);
        } catch (FileNotFoundException e) {
            logger.error("Cannot find config file {}", filename);
        } catch (BadConfigException | IOException e) {
            logger.error("Error reading config file", e);
        }
        return null;
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
        System.out.print(
                String.format("   Next attack session in %s%02d:%02d...", (hours > 0 ? hours + ":" : ""), minutes,
                        seconds));
    }
}
