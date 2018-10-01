package com.lightstreamer.load_test.client.utils;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.commons.Constants;

public class Utils {
    static Logger _log = Logger.getLogger(Constants.CONFIGURATION_LOGGER);
    
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
