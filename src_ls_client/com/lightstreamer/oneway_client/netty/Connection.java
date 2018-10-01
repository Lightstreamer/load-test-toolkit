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
