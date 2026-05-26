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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lightstreamer.load_test.client.utils.BaseSubscriptionListener;
import com.lightstreamer.load_test.commons.ClientConfiguration;
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
    private static Logger _logUpdates = LogManager.getLogger(Constants.UPDATES_LOGGER);
    private static Logger _logLatencies = LogManager.getLogger(Constants.LATENCY_LOGGER);
    
    //for this logger to work the statsManager must be != null (i.e. latency_statistics must be at least at INFO level) 
    private static Logger _logUpdates2 = LogManager.getLogger(Constants.TIMESTAMPS_LOGGER);
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
    private ClientConfiguration conf;
    private String timestamp_field;
    /** Pre-compiled date formatter for non-epoch timestamps; null means only epoch ms is accepted. */
    private final DateTimeFormatter dateFormatter;
    /** Field names for protobuf google.protobuf.Timestamp (seconds + nanos); null when not configured. */
    private final String tsSecondsField;
    private final String tsNanosField;
    private int sessionId = 0;
    StringBuffer update;

    public TableListener(SessionsHandler sessionsHandler, int id, LightstreamerClient lsClient, Subscription table,
            StatisticsManager statsManager, ClientConfiguration conf) {
        this.sessionsHandler = sessionsHandler;
        this.lsClient = lsClient;
        this.table = table;
        this.statsManager = statsManager;
        this.sessionId = id;
        this.conf = conf;

        this.speedUpReading = conf.isSpeedUpReading();
        if(_logUpdates!=null) {
            update = new StringBuffer();
        }

        this.timestamp_field = conf.tsField4Latency;
        this.dateFormatter = (conf.tsDateFormat != null && !conf.tsDateFormat.isEmpty())
                ? DateTimeFormatter.ofPattern(conf.tsDateFormat)
                : null;
        this.tsSecondsField = conf.tsSecondsField;
        this.tsNanosField = conf.tsNanosField;
    }

    @Override
    public void onSubscriptionError(int code, String message) {
        _logUpdates.info("Warn - " + code + " - " + message);

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
                try {
                    // Protobuf Timestamp mode: two separate fields (seconds + nanos)
                    if (tsSecondsField != null && tsNanosField != null) {
                        String secondsStr = values.getValue(tsSecondsField);
                        String nanosStr = values.getValue(tsNanosField);

                        _logLatencies.debug("Raw protobuf Timestamp fields: seconds=" + secondsStr + ", nanos=" + nanosStr);

                        long seconds = Long.parseLong(secondsStr);
                        long nanos = Long.parseLong(nanosStr);
                        simulatorTime = seconds * 1000 + nanos / 1_000_000;
                    } else {
                        // Single-field mode
                        String timestampString = values.getValue(timestamp_field);

                        _logLatencies.debug("Raw field for latency as configured: " + timestampString);

                        // Se il timestamp è nel formato "timestamp=1759747213345", estraiamo solo la parte numerica
                        if (timestampString != null && timestampString.contains("=")) {
                            timestampString = timestampString.split("=")[1];
                        }
                    
                        // Parse according to configuration:
                        // - if tsDateFormat is set, use DateTimeFormatter directly
                        //   - try LocalDateTime first (full date+time formats)
                        //   - fall back to LocalTime combined with today's date (time-only formats like HH:mm:ss)
                        // - otherwise assume the value is an epoch timestamp (milliseconds)
                        if (dateFormatter != null) {
                            try {
                                simulatorTime = LocalDateTime.parse(timestampString, dateFormatter)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli();
                            } catch (DateTimeParseException dtpe) {
                                // Time-only format: combine with today's date
                                simulatorTime = LocalTime.parse(timestampString, dateFormatter)
                                        .atDate(LocalDate.now())
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli();
                            }
                        } else {
                            simulatorTime = Long.parseLong(timestampString);
                        }
                    }
                    long localTime = TimeConversion.getTimeMillis();

                    int delay = (int) (localTime - simulatorTime);
                    this.statsManager.onData(delay);

                    if (_logUpdates2 != null) {
                        if (localTime - lastLog > 100) {
                            synchronized (timestampsString) {
                                if (localTime - lastLog > 100) {
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
                } catch (DateTimeParseException e) {
                    e.printStackTrace();
                }
            } catch (NumberFormatException nfe) {
                _logLatencies.error(LATENCY_ERROR, nfe);
                return;
            } catch (NullPointerException npe) {
                _logLatencies.error(LATENCY_ERROR, npe);
                return;
            } catch (IndexOutOfBoundsException iob) {
                _logLatencies.error(LATENCY_ERROR, iob);
                return;
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