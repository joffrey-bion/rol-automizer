package org.hildan.bots.riseoflords.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import org.hildan.bots.riseoflords.RolAutomizer;
import org.hildan.bots.riseoflords.util.Log;

public class Config {

    private static final String TAG = Config.class.getSimpleName();

    public static class BadConfigException extends Exception {

        BadConfigException(String message) {
            super(message);
        }
    }

    private Account account;
    private PlayerFilter filter;
    private AttackParams params;
    private int nbOfAttacks;
    private Duration timeBetweenAttacks;

    public static Config loadFromResource(String filename) throws IOException, BadConfigException {
        final InputStream input = RolAutomizer.class.getResourceAsStream(filename);
        if (input == null) {
            throw new FileNotFoundException("file " + filename + " not found as resource");
        }
        return load(input);
    }

    public static Config loadFromFile(String filename) throws IOException, BadConfigException {
        return load(new FileInputStream(filename));
    }

    private static Config load(InputStream configFile) throws IOException, BadConfigException {
        final Properties prop = new Properties();
        prop.load(configFile);

        final Config config = new Config();
        final String login = getMandatoryProperty(prop, "account.login");
        final String password = getMandatoryProperty(prop, "account.password");
        config.account = new Account(login, password);

        final int minRank = getIntProperty(prop, "filter.minRank", 5000);
        final int maxRank = getIntProperty(prop, "filter.maxRank", 6000);
        final int minGold = getIntProperty(prop, "filter.minGold", 450000);
        config.filter = new PlayerFilter(minRank, maxRank, minGold);

        final int maxTurns = getIntProperty(prop, "attack.maxTurns", 20);
        final int storagePeriod = getIntProperty(prop, "attack.storagePeriod", 2);
        final int repairPeriod = getIntProperty(prop, "attack.repairPeriod", 5);
        config.params = new AttackParams(maxTurns, repairPeriod, storagePeriod);

        config.timeBetweenAttacks = Duration.ofHours(getIntProperty(prop, "sequence.hoursBetweenAttacks", 1));
        config.nbOfAttacks = getIntProperty(prop, "sequence.nbOfAttacks", 1);

        return config;
    }

    private static String getMandatoryProperty(Properties prop, String key) throws BadConfigException {
        final String strValue = prop.getProperty(key);
        if (strValue == null) {
            throw new BadConfigException("missing key '" + key + "', can't continue");
        }
        if (strValue.length() == 0) {
            throw new BadConfigException("no value for '" + key + "', can't continue");
        }
        return strValue;
    }

    private static int getIntProperty(Properties prop, String key, int defaultValue) throws BadConfigException {
        final String strValue = prop.getProperty(key);
        if (strValue == null) {
            Log.get().w(TAG, "missing key '", key, "', using default value (", defaultValue, ")");
            return defaultValue;
        }
        try {
            return Integer.parseInt(strValue);
        } catch (final NumberFormatException e) {
            throw new BadConfigException("the value for key '" + key + "' must be an integer");
        }
    }

    public Account getAccount() {
        return account;
    }

    public PlayerFilter getPlayerFilter() {
        return filter;
    }

    public AttackParams getAttackParams() {
        return params;
    }

    public int getNbOfAttacks() {
        return nbOfAttacks;
    }

    public boolean unlimitedAttacks() {
        return nbOfAttacks == 0;
    }

    public Duration getTimeBetweenAttacks() {
        return timeBetweenAttacks;
    }

    @Override
    public String toString() {
        return account + "\n" + filter + "\n" + params;
    }
}
