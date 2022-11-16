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


package com.lightstreamer.oneway_client.netty;

import com.lightstreamer.oneway_client.SubscriptionListener;

/**
 * Very simple connection interface.
 */
public interface Connection {

    /**
     * Sends a message.
     */
    void sendMessage(String msg);
    
    /**
     * Sends a subscription request.
     * If configuration "ignoreData" is set to true, fires {@link SubscriptionListener#onSubscription()}.
     */
    void sendSubscription(String sub);
    
    /**
     * Closes the connection.
     */
    void close();

    /**
     * Tries to speed up the reading of the stream connection by not encoding/decoding data.
     * It should be used only if configuration "ignoreData" is set to true.
     * <p>
     * WARNING<br>
     * When called, the client is unable to subscribe, send messages, receive events etc.
     */
    void speedUpReading();
}
