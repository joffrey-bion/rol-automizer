package com.jbion.riseoflords;

import java.io.IOException;

import com.jbion.riseoflords.config.Config;
import com.jbion.riseoflords.config.Config.BadConfigException;
import com.jbion.riseoflords.util.Log;

public class Main {
    
    private static final boolean DEBUG = false;

    private static final String TAG = Main.class.getSimpleName();
        
    private static final String PROP_FILE = DEBUG ? "/internal-bot.properties" : "/bot.properties";

    public static void main(String[] args) {
        System.out.println();
        
        Config config;
        try {
            config = Config.load(PROP_FILE);
        } catch (IOException | BadConfigException e) {
            Log.get().e(TAG, "Error reading config file: ", e.getMessage());
            return;
        }
        Log.get().i(TAG, config);
        System.out.println();
        new Sequencer(config).start();
    }
}
