package com.lightstreamer.load_test.simulator;

import java.util.HashMap;


/**
 * Used to receive data from the simulated broadcast feed in an
 * asynchronous way, through the onEvent method.
 */
public interface FeedListener {

    /**
     * Called by the feed for each update event occurrence.
     * If isSnapshot is true, then the event contains a full snapshot,
     * with the current values of all fields.
     */
    void onEvent(String itemName, HashMap<String,byte[]> currentValues);

}
