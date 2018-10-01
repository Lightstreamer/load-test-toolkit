package com.lightstreamer.load_test.commons;

public class Constants {
    public static final int SIMULATOR_TIMESTAMP_FIELD_INDEX = 1;
    
    public static final int FIRST_CUSTOM_FIELD_INDEX = 2;
    
    public static final int SIZE_OF_TIMESTAMP_IN_BYTES = 13;
    
    public static final String ITEM_PREFIX = "i";
    public static final String FIELD_PREFIX = "f";

    public static final String UPDATES_LOGGER = "com.lightstreamer.load_test.reports.session_events";
    public static final String LATENCY_LOGGER = "com.lightstreamer.load_test.reports.latency_reporting";
    public static final String CONFIGURATION_LOGGER = "com.lightstreamer.load_test.configuration";
    public static final String TIMESTAMPS_LOGGER = "com.lightstreamer.load_test.reports.timestamps"; 

    //Latency report constants
    public static final int MAX_LATENCY_MILLIS = 240000;
    public static final int LATENCY_GRAPH_COLUMNS = 8;
    public static final int LATENCY_REPORT_INTERVAL_MILLIS = 60000;

    
  
    
}
