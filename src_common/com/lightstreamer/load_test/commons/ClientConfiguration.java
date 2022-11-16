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


package com.lightstreamer.load_test.commons;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class ClientConfiguration extends ConfigurationReader implements Cloneable {
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private static final Field[] serverSentParams = {
        new Field("numberOfItems",INT,true),
        new Field("numberOfFields",INT,true),
        new Field("subscriptionMode",STRING,true),
        new Field("unfilteredSubscription",BOOL,false),
        new Field("resamplingFrequency",DOUBLE,false),
        new Field("resamplingBufferSize",INT,false),
    };
    
    public int numberOfItems = -1;
    public int numberOfFields = -1;
    public String subscriptionMode = null;
    public boolean unfilteredSubscription = false; //optional
    public double resamplingFrequency = -1; //optional
    public int resamplingBufferSize = -1; //optional
    
    private static final Field[] clientParams = {
        new Field("protocol",STRING,true),
        new Field("host",STRING,true),
        new Field("port",INT,true),
        new Field("adapterSetName",STRING,true),
        new Field("dataAdapterName",STRING,false),
        new Field("numberOfSessions",INT,true),
        new Field("itemsPerSession",INT,true),
        new Field("itemRandomExtraction",BOOL,false),
        new Field("firstItemAvailable",INT,false),
        new Field("lastItemAvailable",INT,false),
        new Field("sessionDurationSeconds",INT,false),
        new Field("delaySessionStartMillis",LONG,false),
        new Field("sendBufferSize",STRING,false),
        new Field("receiveBufferSize",STRING,false),
        new Field("localIP",STRING,false),
        new Field("sessionCreationThreadPoolLength",INT,false),
        new Field("nettyThreads",INT,false),
        new Field("minCreatePool",INT,true),
        new Field("serverPorts",INT,true),
        new Field("listenerThreads",INT,true),
        new Field("useNio",BOOL,false),
        new Field("nioThreads",INT,false),
        new Field("selectorThreads",INT,false),
        new Field("speedUpReading",BOOL,false),
        new Field("ignoreData",BOOL,false)
    };
      
    //the rest
    public String protocol = null;
    public String host = null;
    public int port = -1;
    public String adapterSetName = null; 
    public String dataAdapterName = null; //optional

    public int numberOfSessions = -1;
    public int itemsPerSession = -1;
    public boolean itemRandomExtraction = false; //optional
    public int firstItemAvailable = -1; //optional
    public int lastItemAvailable = -1; //optional
    public int sessionDurationSeconds = -1; //optional
    public long delaySessionStartMillis = -1; //optional
    public String sendBufferSize = null; //optional
    public String receiveBufferSize = null; //optional
    public String localIP = null; //optional
    public int sessionCreationThreadPoolLength = -1;
    public int nettyThreads = -1;
    public int minCreatePool = -1;
    public int serverPorts = -1;
    public int listenerThreads = -1;
    public boolean useNio = false;
    public int nioThreads = -1;
    public int selectorThreads = -1;
  
  //generated stats-related values
    private boolean dataNeededForLog = false;
    boolean ignoreData = true;
    boolean speedUpReading = true;
    
    public void setIgnoreData(boolean speedUpReading) {
        this.speedUpReading = speedUpReading;
    }
    public void setSpeedUpReading(boolean ignoreData) {
        this.ignoreData = ignoreData;
    }
    public boolean isIgnoreData() {
        return ignoreData && ! dataNeededForLog;
    }
    public boolean isSpeedUpReading() {
        return speedUpReading && isIgnoreData();
    }
     
    public void readSeverSentClientConfiguration(Map<String,String> params) throws Exception {
        readConfiguration(params,serverSentParams);
    }
    
    public HashMap<String,String> getServerSentClientConfigurationMap() {
        HashMap<String,String> result = new HashMap<String,String>();
        
        for (int i=0; i<serverSentParams.length; i++) {
            result.put(serverSentParams[i].name, this.getString(serverSentParams[i]));
        }
        
        return result;
    }
    
    public static String[] getServerSentParametersList() {
        String[] completeList = new String[serverSentParams.length];
        
        
        for (int x=0; x<serverSentParams.length; x++) {
            completeList[x] = serverSentParams[x].name;
        }
        
        return completeList;
    }
    
    public void readClientConfiguration(Map<String, String> params) throws Exception {
        readConfiguration(params,clientParams);
    }
    
    public void generateDataNeededValue() {
        Logger updatesLogger = Logger.getLogger(Constants.UPDATES_LOGGER);
        Logger latencyLogger = Logger.getLogger(Constants.LATENCY_LOGGER);
        this.dataNeededForLog = updatesLogger.isDebugEnabled() || latencyLogger.isInfoEnabled();
        _log.info("updatesLogger.isDebugEnabled(): " + updatesLogger.isDebugEnabled());
        _log.info("latencyLogger.isInfoEnabled(): " + latencyLogger.isInfoEnabled());
        _log.info("ignoreData: " + isIgnoreData());
    }

}
