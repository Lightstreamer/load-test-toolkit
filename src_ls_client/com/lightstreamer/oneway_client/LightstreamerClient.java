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


package com.lightstreamer.oneway_client;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import com.lightstreamer.oneway_client.netty.Factory;
import com.lightstreamer.oneway_client.netty.Session;

/**
 * A skeletal implementation of Java Client API (Unified).
 */
public class LightstreamerClient {

    public final String serverUrl;
    public final String adapterSet;
    private final Session session;
    private volatile String status = "DISCONNECTED";
    private final Executor executor = Factory.getDefaultFactory().getListenerExecutor();
    private final ArrayList<ClientListener> listeners = new ArrayList<>(1);
    
    public final ConnectionOptions connectionOptions = new ConnectionOptions();
    
    public final ConnectionDetails connectionDetails = new ConnectionDetails();
    
    public LightstreamerClient(String serverUrl, String adapterSetName) {
        this.serverUrl = serverUrl;
        this.adapterSet = adapterSetName;
        this.session = new Session(this);
    }
    
    public void connect() {
        session.connect();
    }

    public void subscribe(Subscription sub) {
        session.subscribe(sub);
    }

    public void disconnect() {
        session.disconnect();
    }
    
    public void addListener(ClientListener clientListener) {
        synchronized (listeners) {
            listeners.add(clientListener);
        }
    }
    
    public void sendMessage(String message) {
        session.sendMessage(message);
    }
    
    public String getStatus() {
        return status;
    }
    
    /**
     * Tries to speed up the reading of the stream connection by not encoding/decoding data 
     * (for example if encrypted via SSL).
     * It should be used only if configuration "ignoreData" is set to true.
     * <p>
     * WARNING<br>
     * When called, the client is unable to subscribe, send messages, receive events etc.
     */
    public void speedUpReading() {
        session.speedUpReading();
    }
    
    public void fireOnStatusChange(String status) {
        this.status = status;
        synchronized (listeners) {
            for (ClientListener listener : listeners) {
                executor.execute(() -> listener.onStatusChange(status));
            }
        }
    }
    
    public void fireOnServerError(int code, String error) {
        synchronized (listeners) {
            for (ClientListener listener : listeners) {
                executor.execute(() -> listener.onServerError(code, error));
            }
        }
    }
    
    public class ConnectionOptions {
        public volatile String transport = "WS-STREAMING";
        
        public void setForcedTransport(String transport) {
            this.transport = transport;
        }

        public String getForcedTransport() {
            return transport;
        }
    }
    
    public class ConnectionDetails {
        public volatile String user;
        public volatile String password;
        
        public void setUser(String user) {
            this.user = user;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }

}
