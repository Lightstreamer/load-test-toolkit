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

    String[] stringkeys_sc2 = { "ltest-[key=Apple]", "ltest-[key=Banana]",
            "ltest-[key=Orange]", "ltest-[key=Grape]",
            "ltest-[key=Pineapple]", "ltest-[key=Strawberry]", "ltest-[key=Watermelon]",
            "ltest-[key=Mango]",
            "ltest-[key=Kiwi]", "ltest-[key=Lemon]", "ltest-[key=Peach]",
            "ltest-[key=Cherry]", "ltest-[key=Blueberry]",
            "ltest-[key=Raspberry]", "ltest-[key=Blackberry]", "ltest-[key=Coconut]",
            "ltest-[key=Pomegranate]",
            "ltest-[key=Cantaloupe]", "ltest-[key=Apricot]", "ltest-[key=Fig]",
            "ltest-[key=Plum]", "ltest-[key=Pear]",
            "ltest-[key=Avocado]", "ltest-[key=Lychee]", "ltest-[key=Guava]",
            "ltest-[key=Dragonfruit]",
            "ltest-[key=Passionfruit]", "ltest-[key=Papaya]", "ltest-[key=Melon]",
            "ltest-[key=Lime]",
            "ltest-[key=Nectarine]", "ltest-[key=Persimmon]", "ltest-[key=Starfruit]",
            "ltest-[key=Tangerine]",
            "ltest-[key=Durian]", "ltest-[key=Kumquat]", "ltest-[key=Cranberry]",
            "ltest-[key=Rambutan]",
            "ltest-[key=Mangosteen]", "ltest-[key=Jackfruit]" };

    String[] stringkeys_sc3 = { "jsontest-[key=James]", "jsontest-[key=John]",
            "jsontest-[key=Robert]", "jsontest-[key=Michael]",
            "jsontest-[key=William]", "jsontest-[key=David]", "jsontest-[key=Richard]",
            "jsontest-[key=Joseph]",
            "jsontest-[key=Charles]", "jsontest-[key=Thomas]", "jsontest-[key=Daniel]",
            "jsontest-[key=Matthew]", "jsontest-[key=Christopher]",
            "jsontest-[key=George]", "jsontest-[key=Brian]", "jsontest-[key=Edward]",
            "jsontest-[key=Ronald]",
            "jsontest-[key=Anthony]", "jsontest-[key=Kevin]", "jsontest-[key=Jason]",
            "jsontest-[key=Gary]", "jsontest-[key=Timothy]",
            "jsontest-[key=Jose]", "jsontest-[key=Larry]", "jsontest-[key=Jeffrey]",
            "jsontest-[key=Frank]",
            "jsontest-[key=Scott]", "jsontest-[key=Eric]", "jsontest-[key=Stephen]",
            "jsontest-[key=Andrew]",
            "jsontest-[key=Raymond]", "jsontest-[key=Gregory]", "jsontest-[key=Joshua]",
            "jsontest-[key=Jerry]",
            "jsontest-[key=Dennis]", "jsontest-[key=Walter]", "jsontest-[key=Patrick]",
            "jsontest-[key=Peter]",
            "jsontest-[key=Harold]", "jsontest-[key=Douglas]" };

    String[] stringkeys_sc3jp = { "ltest-[key=James]", "ltest-[key=John]",
            "ltest-[key=Robert]", "ltest-[key=Michael]",
            "ltest-[key=William]", "ltest-[key=David]", "ltest-[key=Richard]",
            "ltest-[key=Joseph]",
            "ltest-[key=Charles]", "ltest-[key=Thomas]", "ltest-[key=Daniel]",
            "ltest-[key=Matthew]", "ltest-[key=Christopher]",
            "ltest-[key=George]", "ltest-[key=Brian]", "ltest-[key=Edward]",
            "ltest-[key=Ronald]",
            "ltest-[key=Anthony]", "ltest-[key=Kevin]", "ltest-[key=Jason]",
            "ltest-[key=Gary]", "ltest-[key=Timothy]",
            "ltest-[key=Jose]", "ltest-[key=Larry]", "ltest-[key=Jeffrey]",
            "ltest-[key=Frank]",
            "ltest-[key=Scott]", "ltest-[key=Eric]", "ltest-[key=Stephen]",
            "ltest-[key=Andrew]",
            "ltest-[key=Raymond]", "ltest-[key=Gregory]", "ltest-[key=Joshua]",
            "ltest-[key=Jerry]",
            "ltest-[key=Dennis]", "ltest-[key=Walter]", "ltest-[key=Patrick]",
            "ltest-[key=Peter]",
            "ltest-[key=Harold]", "ltest-[key=Douglas]" };

    String[] ___strings = { "jsontest-[sndValue=Apple]", "jsontest-[sndValue=Banana]", "jsontest-[sndValue=Orange]",
            "jsontest-[sndValue=Grape]", "jsontest-[sndValue=Pineapple]", "jsontest-[sndValue=Strawberry]",
            "jsontest-[sndValue=Watermelon]", "jsontest-[sndValue=Mango]", "jsontest-[sndValue=Kiwi]",
            "jsontest-[sndValue=Lemon]",
            "jsontest-[sndValue=Peach]", "jsontest-[sndValue=Cherry]", "jsontest-[sndValue=Blueberry]",
            "jsontest-[sndValue=Raspberry]",
            "jsontest-[sndValue=Blackberry]", "jsontest-[sndValue=Coconut]", "jsontest-[sndValue=Pomegranate]",
            "jsontest-[sndValue=Cantaloupe]",
            "jsontest-[sndValue=Apricot]", "jsontest-[sndValue=Fig]", "jsontest-[sndValue=Plum]",
            "jsontest-[sndValue=Pear]",
            "jsontest-[sndValue=Avocado]", "jsontest-[sndValue=Lychee]", "jsontest-[sndValue=Guava]",
            "jsontest-[sndValue=Dragonfruit]",
            "jsontest-[sndValue=Passionfruit]", "jsontest-[sndValue=Papaya]", "jsontest-[sndValue=Melon]",
            "jsontest-[sndValue=Lime]",
            "jsontest-[sndValue=Nectarine]", "jsontest-[sndValue=Persimmon]", "jsontest-[sndValue=Starfruit]",
            "jsontest-[sndValue=Tangerine]",
            "jsontest-[sndValue=Durian]", "jsontest-[sndValue=Kumquat]", "jsontest-[sndValue=Cranberry]",
            "jsontest-[sndValue=Rambutan]",
            "jsontest-[sndValue=Mangosteen]", "jsontest-[sndValue=Jackfruit]" };

    String item_sc1 = "ltest";

    // we extract info from conf and prepare these 5 variables
    private int firstItem;
    private int lastItem;
    private int numItems;
    private Random random = new Random();
    
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
        table.addListener(new TableListener(this, id, lsClient, table, statsManager, conf));
        
        if (_logUpdates.isDebugEnabled()) {
            _logUpdates.debug("Subscribing to items for session " + id + "("+table.getItemGroup()+")...");
        }
        lsClient.subscribe(table);
    }
    
    
    private Subscription configureTable(int id) {
        // String group = null;
        // if (conf.itemRandomExtraction) {
        // group = chooseItemsNumberAtRandom(id);
        // } else {
        // group = chooseItemsNumberAtSameDistance(id);
        // }

        Subscription table = new Subscription(conf.subscriptionMode);

        if ((conf.scenarioLKC.equals("1")) || (conf.scenarioLKC.equals("3jp"))) {
            String[] fields = { "key", "value", };
            table.setFields(fields);

            table.setDataAdapter("QuickStart");
        } else if (conf.scenarioLKC.equals("2")) {
            String[] fields = { "key", "timestamp", "sndValue", "intNum", "ts", "partition" };
            table.setFields(fields);

            table.setDataAdapter("JsonStart-k");
        } else {
            String[] fields = { "key", "firstText", "secondText", "thirdText",
                    "fourthText", "firstnumber",
                    "secondNumber", "thirdNumber", "fourthNumber", "hobbies", "timestamp" };
            table.setFields(fields);

            table.setDataAdapter("JsonStart-k");
        }

        if (conf.scenarioLKC.equals("1")) {
            table.setItemGroup(item_sc1);
        } else if (conf.scenarioLKC.equals("3jp")) {
            int index = random.nextInt(stringkeys_sc3jp.length);
            table.setItemGroup(stringkeys_sc3jp[index]);
        } else if ((conf.scenarioLKC.equals("2"))) {
            int index = random.nextInt(stringkeys_sc2.length);
            table.setItemGroup(stringkeys_sc2[index]);
        } else if ((conf.scenarioLKC.equals("3"))) {
            int index = random.nextInt(stringkeys_sc3.length);
            table.setItemGroup(stringkeys_sc3[index]);
        }

        table.setRequestedSnapshot("no");

        // if (conf.dataAdapterName != null) {
        // table.setDataAdapter(conf.dataAdapterName);
        // }

        // if (conf.unfilteredSubscription) {
        // table.setRequestedMaxFrequency("unfiltered");

        // } else {
        // if (conf.resamplingFrequency > 0) {
        // table.setRequestedMaxFrequency("" + conf.resamplingFrequency);
        // }
        // if (conf.resamplingBufferSize > 0) {
        // table.setRequestedBufferSize("" + conf.resamplingBufferSize);
        // }
        // }
        
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
