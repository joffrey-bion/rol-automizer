package com.jbion.riseoflords.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jbion.riseoflords.Main;
import com.jbion.riseoflords.util.Log;

public class Config {

    private static final String TAG = Config.class.getSimpleName();

    public static class BadConfigException extends Exception {

        public BadConfigException() {
            super();
        }

        public BadConfigException(String message) {
            super(message);
        }

        public BadConfigException(Throwable cause) {
            super(cause);
        }
    }

    private Account account;
    private PlayerFilter filter;
    private AttackParams params;
    private int nbOfAttacks;
    private long timeBetweenAttacks;

    public static Config loadFromResource(String filename) throws IOException, BadConfigException {
        InputStream input = Main.class.getResourceAsStream(filename);
        if (input == null) {
            throw new FileNotFoundException("file " + filename + " not found as resource");
        }
        return load(input);
    }

    public static Config loadFromFile(String filename) throws IOException, BadConfigException {
        return load(new FileInputStream(filename));
    }

    public static Config load(InputStream configFile) throws IOException, BadConfigException {
        Properties prop = new Properties();
        prop.load(configFile);

        Config config = new Config();
        String login = getMandatoryProperty(prop, "account.login");
        String password = getMandatoryProperty(prop, "account.password");
        config.account = new Account(login, password);

        int minRank = getIntProperty(prop, "filter.minRank", 5000);
        int maxRank = getIntProperty(prop, "filter.maxRank", 6000);
        int minGold = getIntProperty(prop, "filter.minGold", 450000);
        config.filter = new PlayerFilter(minRank, maxRank, minGold);

        int maxTurns = getIntProperty(prop, "attack.maxTurns", 20);
        int storingFrequency = getIntProperty(prop, "attack.storingFrequency", 2);
        int repairFrequency = getIntProperty(prop, "attack.repairFrequency", 5);
        config.params = new AttackParams(maxTurns, repairFrequency, storingFrequency);

        config.timeBetweenAttacks = getIntProperty(prop, "sequence.hoursBetweenAttacks", 1) * 3600 * 1000;
        config.nbOfAttacks = getIntProperty(prop, "sequence.nbOfAttacks", 1);

        return config;
    }

    private static String getMandatoryProperty(Properties prop, String key) throws BadConfigException {
        String strValue = prop.getProperty(key);
        if (strValue == null) {
            throw new BadConfigException("missing key '" + key + "', can't continue");
        }
        if (strValue.length() == 0) {
            throw new BadConfigException("no value for '" + key + "', can't continue");
        }
        return strValue;
    }

    private static int getIntProperty(Properties prop, String key, int defaultValue) throws BadConfigException {
        String strValue = prop.getProperty(key);
        if (strValue == null) {
            Log.get().w(TAG, "missing key '", key, "', using default value (", defaultValue, ")");
            return defaultValue;
        }
        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException e) {
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

    public long getTimeBetweenAttacks() {
        return timeBetweenAttacks;
    }

    @Override
    public String toString() {
        return account + "\n" + filter + "\n" + params;
    }
}
