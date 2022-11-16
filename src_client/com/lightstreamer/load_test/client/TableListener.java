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

import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.client.utils.BaseSubscriptionListener;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.load_test.commons.TimeConversion;
import com.lightstreamer.oneway_client.ItemUpdate;
import com.lightstreamer.oneway_client.LightstreamerClient;
import com.lightstreamer.oneway_client.Subscription;

class TableListener extends BaseSubscriptionListener {
    
    private final SessionsHandler sessionsHandler;
    private final LightstreamerClient lsClient;
    private final Subscription table;
    /**
     * When the flag is true, the client tries to optimize the reading by not decoding SSL data.
     */
    private final boolean speedUpReading;
    
    private static final String LATENCY_ERROR = "Latency report is active but the server is not sending timestamps. Please correct the client log configuration or the adapters.xml configuration file on the server";
    private static Logger _logUpdates = Logger.getLogger(Constants.UPDATES_LOGGER);
    private static Logger _logLatencies = Logger.getLogger(Constants.LATENCY_LOGGER);
    
    //for this logger to work the statsManager must be != null (i.e. latency_statistics must be at least at INFO level) 
    private static Logger _logUpdates2 = Logger.getLogger(Constants.TIMESTAMPS_LOGGER);
    private static StringBuffer timestampsString;
    private static long lastLog;
    
    static {
        if (!_logUpdates.isTraceEnabled()) {
            _logUpdates = null;
        }
        if (!_logUpdates2.isDebugEnabled()) {
            _logUpdates2 = null;
        } else {
            lastLog = TimeConversion.getTimeMillis();
            timestampsString = new StringBuffer();
        }
    }
    
    private StatisticsManager statsManager;
    private int sessionId = 0;
    StringBuffer update;

    public TableListener(SessionsHandler sessionsHandler, int id, LightstreamerClient lsClient, Subscription table, StatisticsManager statsManager, boolean speedUpReading) {
        this.sessionsHandler = sessionsHandler;
        this.lsClient = lsClient;
        this.table = table;
        this.statsManager = statsManager;
        this.sessionId = id;
        this.speedUpReading = speedUpReading;
        if(_logUpdates!=null) {
            update = new StringBuffer();
        }
    }

    @Override
    public void onSubscriptionError(int code, String message) {
        this.sessionsHandler.abortSession(sessionId,lsClient,new RuntimeException(code + " " + message),SessionsHandler.FAILED_SUBSCRIPTION);
    }
    
    @Override
    public void onSubscription() {
        if (speedUpReading) {
            lsClient.speedUpReading();
        }
        this.sessionsHandler.batchLogger.onSubscription();
        if (this.sessionsHandler._logUpdates.isDebugEnabled()) {
            this.sessionsHandler._logUpdates.debug("Items subscribed for session " + sessionId + "("+table.getItemGroup()+")");
        }
    }
    
    @Override
    public void onItemUpdate(ItemUpdate values) {
        long localTime = TimeConversion.getTimeMillis();
        int itemPos = values.getItemPos();
        
        if (_logUpdates != null) {
            update.setLength(0);   
            
            update.append("Session ");
            update.append(this.sessionId);
            update.append(" item ");
            update.append(itemPos);
            
            for (Entry<Integer, String> el : values.getFieldsByPosition().entrySet()) {
                update.append("\nfield ");
                update.append(el.getKey());
                update.append(": ");
                update.append(el.getValue());
            }
            
            _logUpdates.trace(update);
        }
        
        if (statsManager != null) {
            long simulatorTime=0;
            try {            
                //the first Constants.SIZE_OF_TIMESTAMP_IN_BYTES characters in the Constants.SIMULATOR_TIMESTAMP_FIELD_INDEX field is my timestamp
//                String lastValue = values.getNewValue(Constants.SIMULATOR_TIMESTAMP_FIELD_INDEX).substring(0,Constants.SIZE_OF_TIMESTAMP_IN_BYTES);
                String lastValue = values.getValue(Constants.SIMULATOR_TIMESTAMP_FIELD_INDEX).substring(0,Constants.SIZE_OF_TIMESTAMP_IN_BYTES);
                simulatorTime = Long.valueOf(lastValue); 
            } catch(NumberFormatException nfe) {
                _logLatencies.error(LATENCY_ERROR,nfe);
                return;
            } catch(NullPointerException npe) {
                _logLatencies.error(LATENCY_ERROR,npe);
                return;
            } catch(IndexOutOfBoundsException iob) {
                _logLatencies.error(LATENCY_ERROR,iob);
                return;
            } 
            
            int delay = (int) (localTime-simulatorTime); 
            this.statsManager.onData(delay);
            
            if (_logUpdates2 != null) {
                if (localTime-lastLog > 100) {
                    synchronized(timestampsString) {
                        if (localTime-lastLog > 100) {
                            lastLog = localTime;
                            
                            timestampsString.setLength(0);
                            
                            timestampsString.append(localTime);
                            timestampsString.append(" - ");
                            timestampsString.append(simulatorTime);
                            timestampsString.append(" = ");
                            timestampsString.append(delay);
                            timestampsString.append(" | ");
                            
                            timestampsString.append("Session ");
                            timestampsString.append(this.sessionId);
                            timestampsString.append(" item ");
                            timestampsString.append(itemPos);
                            
                            _logUpdates2.debug(timestampsString);
                        }
                    }
                }
                
            }
            
        }
    }
    
    @Override
    public void onItemLostUpdates(String itemName, int item, int lostUpdates) {
        if (_logUpdates != null) {
            _logUpdates.trace("session" + this.sessionId + " has lost " + lostUpdates + " updates for " + item);
        }
    }
    
    @Override
    public void onEndOfSnapshot(String itemName, int item) {
        if (_logUpdates != null) {
            _logUpdates.trace("End of snapshot for " + item);
        }
    }
    
    @Override
    public void onUnsubscription() {
        if (_logUpdates != null) {
            _logUpdates.trace("Unsubscr table for session" + this.sessionId);
        }
    }
}