/*
 *  Copyright (c) Lightstreamer Srl
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.lightstreamer.load_test.client;


import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.client.utils.BaseClientListener;
import com.lightstreamer.load_test.client.utils.Utils;
import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.oneway_client.LightstreamerClient;
import com.lightstreamer.oneway_client.Subscription;


/**
 * This class opens N connections to a Lightstreamer Server and performs
 * a table subscription per each one. It begins to receive updates from the server and 
 * disconnects after some time. 
 */
public class SessionsHandler {

    Logger _logUpdates = Logger.getLogger(Constants.UPDATES_LOGGER);
     
    ClientConfiguration conf;
    private StatisticsManager statsManager = null; 

    // we extract info from conf and prepare these 5 variables
    private int firstItem;
    private int lastItem;
    private int numItems;
    private Random random = new Random();
    
    // Map to track current subscriptions for each session to enable proper unsubscribe
    private java.util.Map<Integer, Subscription> currentSubscriptions = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Symbol lists loaded from configuration (fallback to hard-coded if not configured)
    private final String[] firstList;
    private final String[] secondList;

    // private static final String[] FIRST_LIST = {
    //     "item1", "item3", "item5", "item7", "item9", "item11", "item13", "item15", "item17", "item19"
    // };

    // private static final String[] SECOND_LIST = {
    //     "item2", "item4", "item6", "item8", "item10", "item12", "item14", "item16", "item18", "item20"
    // };
    
    private static final boolean FAILED_SESSION = true;
    static final boolean FAILED_SUBSCRIPTION = false;
    
    BatchLogger batchLogger;
        
    public SessionsHandler(ClientConfiguration conf) {
      this.conf = conf;

      if (Logger.getLogger(Constants.LATENCY_LOGGER).isInfoEnabled()) { 
          this.statsManager = new StatisticsManager();
      }
      
      this.batchLogger = new BatchLogger(conf);
      batchLogger.start();

      this.lastItem = conf.lastItemAvailable > 0 ? conf.lastItemAvailable : conf.numberOfItems;
      this.firstItem = conf.firstItemAvailable > 0 ? conf.firstItemAvailable : 1;
      this.numItems = this.lastItem - this.firstItem + 1;
      
      // Initialize symbol lists from configuration or use defaults
      if (conf.firstList != null && !conf.firstList.trim().isEmpty()) {
          this.firstList = conf.firstList.split(",");
          // Trim whitespace from each item
          for (int i = 0; i < this.firstList.length; i++) {
              this.firstList[i] = this.firstList[i].trim();
          }
      } else {
          // Fallback to hard-coded first list
          this.firstList = new String[]{
              "1INCHUSD", "APTUSD", "AUDCAD", "AUDNZD", "BTGUSD", "CHFSGD", "CHZUSD", "CRVUSD", 
              "ENJUSD", "EURAUD", "EURCAD", "EURHKD", "EURHUF", "EURNOK", "EURNZD", "EURSGD", 
              "EURTRY", "EURZAR", "GBPAUD", "GBPCAD", "GBPNOK", "GBPNZD", "GBPSGD", "GER40Cash", 
              "GOLD", "IMXUSD", "JP225Cash", "NGASCash", "NZDCAD", "OPUSD", "UK100Cash", 
              "UNIUSD", "US30Cash", "US500Cash", "USDZAR", "XPTUSD", "ZECUSD"
          };
      }
      
      if (conf.secondList != null && !conf.secondList.trim().isEmpty()) {
          this.secondList = conf.secondList.split(",");
          // Trim whitespace from each item
          for (int i = 0; i < this.secondList.length; i++) {
              this.secondList[i] = this.secondList[i].trim();
          }
      } else {
          // Fallback to hard-coded second list
          this.secondList = new String[]{
              "EURUSD", "USDJPY", "GBPUSD", "USDCAD", "AUDUSD", "EURJPY", 
              "GBPJPY", "USDCHF", "EURGBP", "AUDJPY"
          };
      }
      
      if(_logUpdates.isDebugEnabled()) {
          _logUpdates.debug("Initialized with firstList: " + this.firstList.length + " items, secondList: " + this.secondList.length + " items");
      }
    }
    
    
    
    public void start() {
        long delaySessionStartMillis = conf.delaySessionStartMillis;
        for (int i = 0; i < conf.numberOfSessions; i++) {
            startSession(i+1);
            Utils.sleep(delaySessionStartMillis);
        }

        Utils.sleep(conf.sessionDurationSeconds * 1000);
        batchLogger.flush();
        _logUpdates.info("All sessions terminated. Exiting...");
    }
    
    void abortSession(int id,LightstreamerClient lsClient, Exception e, boolean whatFailed) {
        lsClient.disconnect();
        if (whatFailed == FAILED_SESSION) {
            if(_logUpdates.isDebugEnabled()) {
                _logUpdates.debug("Session creation " + id + " failed",e);
            }
            this.batchLogger.onFailedSession();
        } else /*if (whatFailed == FAILED_SUBSCRIPTION) */ {
            if(_logUpdates.isDebugEnabled()) {
                _logUpdates.debug("Subscription failed for session " + id + "; session aborted",e);
            }
            this.batchLogger.onFailedSubscription();
        }
    }
    
    private void startSession(final int id) {
        String serverAddress = conf.protocol + conf.host + ":" + conf.port;
        String adapterSetName = conf.adapterSetName;
        final LightstreamerClient lsClient = new LightstreamerClient(serverAddress, adapterSetName);
        if (conf.protocol.equalsIgnoreCase("ws://") || conf.protocol.equalsIgnoreCase("wss://")) {
            lsClient.connectionOptions.setForcedTransport("WS-STREAMING");
        } else {
            assert conf.protocol.equalsIgnoreCase("http://") || conf.protocol.equalsIgnoreCase("https://");
            lsClient.connectionOptions.setForcedTransport("HTTP-STREAMING");
        }
        
        lsClient.addListener(new BaseClientListener() {
            @Override
            public void onServerError(int errorCode, String errorMessage) {
                abortSession(id,lsClient,new RuntimeException(errorCode + " " + errorMessage),FAILED_SESSION);
            }
            
            @Override
            public void onStatusChange(String status) {
                switch (status) {
                case "CONNECTED:HTTP-STREAMING":
                case "CONNECTED:WS-STREAMING":
                    batchLogger.onNewSession();
                    if(_logUpdates.isDebugEnabled()) {
                        _logUpdates.debug("Session " + id + " created");
                    }
                    // Immediately subscribe to first list when connected
                    subscribeToSymbolList(lsClient, id, true);
                    
                    // Setup timer to switch to second list every 15 seconds if enabled by configuration
                    if (conf.enableSymbolListSwitching) {
                        setupSymbolListSwitching(lsClient, id);
                    }
                    break;
                }
            }
        });
        
        if(_logUpdates.isDebugEnabled()) {
            _logUpdates.debug("Creating session " + id + "...");
        }
        lsClient.connect();
    }
    
    private void subscribeToSymbolList(LightstreamerClient lsClient, int sessionId, boolean useFirstList) {
        // Choose which symbol list to use from configuration
        String[] symbolList = useFirstList ? this.firstList : this.secondList;
        String listName = useFirstList ? "FIRST_LIST" : "SECOND_LIST";
        
        // Use the symbol names directly from the list without serverId suffix
        String[] itemNames = symbolList;
        
        // Unsubscribe from any existing subscription first
        Subscription currentSubscription = currentSubscriptions.get(sessionId);
        if (currentSubscription != null) {
            if (_logUpdates.isDebugEnabled()) {
                _logUpdates.debug("Session " + sessionId + " unsubscribing from previous subscription");
            }
            try {
                lsClient.unsubscribe(currentSubscription);
            } catch (Exception e) {
                if (_logUpdates.isDebugEnabled()) {
                    _logUpdates.debug("Error unsubscribing from previous subscription for session " + sessionId, e);
                }
            }
        }
        
        Subscription subscription = new Subscription(conf.subscriptionMode);
        subscription.setFields(conf.listOfFields.split(","));
        subscription.setDataAdapter(conf.dataAdapterName != null ? conf.dataAdapterName : "DEFAULT");
        subscription.setItems(itemNames); // Subscribe to ALL items in the list
        subscription.setRequestedSnapshot("no");
        
        if (conf.unfilteredSubscription) {
            subscription.setRequestedMaxFrequency("unfiltered");
        }
        
        subscription.addListener(new TableListener(this, sessionId, lsClient, subscription, statsManager, conf));
        
        if (_logUpdates.isDebugEnabled()) {
            _logUpdates.debug("Session " + sessionId + " subscribing to " + listName + " - " + itemNames.length + " items");
        }
        
        lsClient.subscribe(subscription);
        
        // Store the new subscription for future unsubscribe operations
        currentSubscriptions.put(sessionId, subscription);
    }
    
    private void setupSymbolListSwitching(final LightstreamerClient lsClient, final int sessionId) {
        Timer timer = new Timer("SymbolSwitcher-" + sessionId, true);
        
        timer.scheduleAtFixedRate(new TimerTask() {
            private boolean useFirstList = false; // Start with false since we already subscribed to first list
            
            @Override
            public void run() {
                try {
                    useFirstList = !useFirstList;
                    subscribeToSymbolList(lsClient, sessionId, useFirstList);
                } catch (Exception e) {
                    if(_logUpdates.isDebugEnabled()) {
                        _logUpdates.debug("Error switching symbol list for session " + sessionId, e);
                    }
                }
            }
        }, conf.symbolListSwitchingPeriodMillis, conf.symbolListSwitchingPeriodMillis); // Use configured switching period
    }
    
    
    private Subscription configureTable(int id) {
        Subscription table = new Subscription(conf.subscriptionMode);

        table.setFields(conf.listOfFields.split(","));
        table.setDataAdapter(conf.dataAdapterName != null ? conf.dataAdapterName : "DEFAULT");

        // Choose numbers of items to subscribe depending on configuration 
        // choose randomly numberOfItems from the list of available items without repetitions
        String[] availableItems = conf.listOfItems.split(",");

        // Make sure we don't request more items than available
        int itemsToSelect = Math.min(conf.itemsPerSession, availableItems.length);
        String[] actualItems = new String[itemsToSelect];
        
        // Create a copy of the array to shuffle without modifying the original
        String[] shuffledItems = availableItems.clone();
        
        // Shuffle the array using Fisher-Yates shuffle algorithm
        for (int i = shuffledItems.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String temp = shuffledItems[i];
            shuffledItems[i] = shuffledItems[j];
            shuffledItems[j] = temp;
        }
        
        // Take the first itemsToSelect elements from the shuffled array
        for (int i = 0; i < itemsToSelect; i++) {
            actualItems[i] = shuffledItems[i];
        }
        
        table.setItems(actualItems);

        table.setRequestedSnapshot("no");

        if (conf.unfilteredSubscription) {
            table.setRequestedMaxFrequency("unfiltered");
        }        
       
        return table;
    }
    
    /*
     * Chooses items, at same distance, in the set of available items.
     * For example, with 500 available items and 5 items in schema name,
     * session with id "1" selects: 1, 101, 201, 301 and 401.
     * Distance is computed on number of items available and number of items
     * for each session, while first item number selected hinge upon id of
     * session. So, it is warranted that every item has been subscribed by
     * the same number of sessions "if and only if" the number of all sessions
     * by number of items of each session is a multiple of items available. 
     */
    private String chooseItemsNumberAtSameDistance(int id) {
        int itemNumber = ((id - 1) % numItems) + firstItem;
        // ASSERT (firstItem <= itemNumber < firstItem + numItems);
        String group = Constants.ITEM_PREFIX+itemNumber;
        
        for (int i=1; i<conf.itemsPerSession; i++) {
            // ASSERT (firstItem <= itemNumber < firstItem + numItems);
            itemNumber += numItems / conf.itemsPerSession;
            // ASSERT (firstItem <= itemNumber < firstItem + 2 * numItems);
            if (itemNumber > lastItem) {
                // ASSERT (firstItem + numItems <= itemNumber < firstItem + 2 * numItems);
                itemNumber -= numItems;
                // ASSERT (firstItem <= itemNumber < firstItem + numItems);
            } else {
                // ASSERT (firstItem <= itemNumber <= firstItem + numItems - 1);
            }
            // ASSERT (firstItem <= itemNumber < firstItem + numItems);
            group+= " " + Constants.ITEM_PREFIX+itemNumber;
        }
        return group;
    }
    
    /*
     * Chooses items entirely at random, in the set of available items.
     */
    private String chooseItemsNumberAtRandom(int id) {
        
        Random rnd = new Random();
        String group = "";
        for (int i=0; i<conf.itemsPerSession; i++) {
            int random = rnd.nextInt(numItems);
            int itemNumber = firstItem + random;
            group += (i>0?" ":"") + Constants.ITEM_PREFIX+itemNumber;
        }
        return group;
    }

}
