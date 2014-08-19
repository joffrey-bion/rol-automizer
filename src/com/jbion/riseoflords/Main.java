package com.jbion.riseoflords;

import java.io.IOException;

import com.jbion.riseoflords.config.Config;
import com.jbion.riseoflords.config.Config.BadConfigException;
import com.jbion.riseoflords.util.Log;

public class Main {

    private static final String TAG = Main.class.getSimpleName();

    public static void main(String[] args) {
        Config config;
        try {
            config = Config.load("bot.properties");
        } catch (IOException | BadConfigException e) {
            Log.get().e(TAG, "Error reading config file: ", e.getMessage());
            return;
        }
        Log.get().i(TAG, config);
        System.out.println();
        new Sequencer(config).start();
    }
}
