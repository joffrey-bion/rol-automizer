package com.jbion.riseoflords.config;

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

    public static Config load(String filename) throws IOException, BadConfigException {
        Properties prop = new Properties();
        InputStream input = Main.class.getResourceAsStream(filename);
        if (input == null) {
            throw new FileNotFoundException("config file " + filename + " not found");
        }
        prop.load(input);

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
        
        return config;
    }

    private static String getMandatoryProperty(Properties prop, String key) throws BadConfigException {
        String strValue = prop.getProperty(key);
        if (strValue == null) {
            throw new BadConfigException("missing key '" + key + "' in config file, can't continue");
        }
        return strValue;
    }

    private static int getIntProperty(Properties prop, String key, int defaultValue) throws BadConfigException {
        String strValue = prop.getProperty(key);
        if (strValue == null) {
            Log.get().w(TAG, "missing key '", key, "' in config file, using default (", defaultValue, ")");
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

    @Override
    public String toString() {
        return account + "\n" + filter + "\n" + params;
    }
}
