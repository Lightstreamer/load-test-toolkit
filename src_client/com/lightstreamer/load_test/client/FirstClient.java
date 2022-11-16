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

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.lightstreamer.load_test.client.utils.BaseClientListener;
import com.lightstreamer.load_test.client.utils.BaseSubscriptionListener;
import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.oneway_client.ItemUpdate;
import com.lightstreamer.oneway_client.LightstreamerClient;
import com.lightstreamer.oneway_client.Subscription;

public class FirstClient {
    
    private static String[] group = {"CONFIGURATION"};
    private String[] schema;

    HashMap<String,String> readConf = new HashMap<String,String>();
    Logger _log = Logger.getLogger(Constants.CONFIGURATION_LOGGER);
    
    public FirstClient() {
    }
    
    public HashMap<String, String> readRemoteConf(ClientConfiguration firstClientConf) {     
        schema = ClientConfiguration.getServerSentParametersList();
        
        String serverUrl = firstClientConf.protocol + firstClientConf.host + ":" + firstClientConf.port;
        LightstreamerClient client = new LightstreamerClient(serverUrl, firstClientConf.adapterSetName);
        client.connectionOptions.setForcedTransport("WS-STREAMING");
        client.addListener(new BaseClientListener() {
            @Override
            public void onServerError(int errorCode, String errorMessage) {
                _log.error("Connection failed: " + errorCode + " " + errorMessage);
            }
        });
        
        _log.debug("Connecting to server to retrieve configuration...");
        client.connect();
        
        Subscription sub = new Subscription("MERGE");
        sub.setItems(group);
        sub.setFields(schema);
        sub.setRequestedSnapshot("yes");
        if (firstClientConf.dataAdapterName != null) {
            sub.setDataAdapter(firstClientConf.dataAdapterName);
        }
        sub.addListener(new BaseSubscriptionListener() {
            @Override
            public void onSubscriptionError(int code, String message) {
                _log.error("Subscription failed: " + code + " " + message);
            }
            
            @Override
            public void onItemUpdate(ItemUpdate itemUpdate) {
                synchronized(readConf) {
                    for (int i= 0; i < schema.length; i++) {
                        readConf.put(schema[i], itemUpdate.getValue(schema[i]));
                    }
                    /* let the method readRemoteConf return */
                    readConf.notifyAll();
                }
            }
        });
        
        _log.debug("Subscribing configuration item...");
        client.subscribe(sub);
        
        /* block the method until the first update with configuration parameters arrives */
        synchronized (readConf) {
            while (readConf.isEmpty()) {
                try {
                    readConf.wait();
                } catch (InterruptedException e) {
                    _log.error("Unexpected exception", e);
                }
            }
            client.disconnect();
        }
        return readConf;
    }
    
}
