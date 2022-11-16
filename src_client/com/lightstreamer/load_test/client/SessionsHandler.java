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

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.client.utils.BaseClientListener;
import com.lightstreamer.load_test.client.utils.Utils;
import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.oneway_client.LightstreamerClient;
import com.lightstreamer.oneway_client.Subscription;
import com.lightstreamer.oneway_client.netty.Factory;


/**
 * This class opens N connections to a Lightstreamer Server and performs
 * a table subscription per each one. It begins to receive updates from the server and 
 * disconnects after some time. 
 */
public class SessionsHandler {

    Logger _logUpdates = Logger.getLogger(Constants.UPDATES_LOGGER);
     
    ClientConfiguration conf;
    private StatisticsManager statsManager = null; 
    
    //we extract info from conf and prepare these 5 variables
    private String schemaName = "";
    private int firstItem;
    private int lastItem;
    private int numItems;
    
    private static final boolean FAILED_SESSION = true;
    static final boolean FAILED_SUBSCRIPTION = false;
    
    BatchLogger batchLogger;
        
    public SessionsHandler(ClientConfiguration conf) {
      this.conf = conf;
      
      for (int i=1; i<=conf.numberOfFields-1; i++) {
          schemaName += Constants.FIELD_PREFIX+i + " ";
      }
      schemaName += Constants.FIELD_PREFIX+conf.numberOfFields;
            
      if (Logger.getLogger(Constants.LATENCY_LOGGER).isInfoEnabled()) { 
          this.statsManager = new StatisticsManager();
      }
      
      this.batchLogger = new BatchLogger(conf);
      batchLogger.start();

      this.lastItem = conf.lastItemAvailable > 0 ? conf.lastItemAvailable : conf.numberOfItems;
      this.firstItem = conf.firstItemAvailable > 0 ? conf.firstItemAvailable : 1;
      this.numItems = this.lastItem - this.firstItem + 1;
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
                    break;
                }
            }
        });
        
        if(_logUpdates.isDebugEnabled()) {
            _logUpdates.debug("Creating session " + id + "...");
        }
        lsClient.connect();
        
        final Subscription table = configureTable(id);
        table.addListener(new TableListener(this, id, lsClient, table, statsManager, conf.isSpeedUpReading()));
        
        if (_logUpdates.isDebugEnabled()) {
            _logUpdates.debug("Subscribing to items for session " + id + "("+table.getItemGroup()+")...");
        }
        lsClient.subscribe(table);
    }
    
    
    private Subscription configureTable(int id) {
        String group = null;
        if (conf.itemRandomExtraction) {
            group = chooseItemsNumberAtRandom(id);
        } else {
            group = chooseItemsNumberAtSameDistance(id);
        }
        
        Subscription table = new Subscription(conf.subscriptionMode);
        table.setItemGroup(group);
        table.setFieldSchema(schemaName);
        table.setRequestedSnapshot("no");
        
        if (conf.dataAdapterName != null) {
            table.setDataAdapter(conf.dataAdapterName);
        }
        

        if (conf.unfilteredSubscription) {
            table.setRequestedMaxFrequency("unfiltered");
        
        } else {
            if (conf.resamplingFrequency > 0) {
                table.setRequestedMaxFrequency("" + conf.resamplingFrequency);
            }
            if (conf.resamplingBufferSize > 0) {
                table.setRequestedBufferSize("" + conf.resamplingBufferSize);
            }
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
