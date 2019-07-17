package com.lightstreamer.oneway_client.netty;

import static com.lightstreamer.oneway_client.netty.Logger.logError;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.lightstreamer.oneway_client.ItemUpdate;
import com.lightstreamer.oneway_client.LightstreamerClient;
import com.lightstreamer.oneway_client.Subscription;

/**
 * Session state.
 * 
 * @author Alessandro Carioni
 * @since August 2018
 */
public class Session {
    
    private enum State {
        DISCONNECTED, CONNECTING, CONNECTED, BINDING, BOUND
    }
    
    public static final AtomicInteger nextSubId = new AtomicInteger();
    
    private static final RuntimeException BOGUS_EX = new RuntimeException("Bogus exception");
    
    private final Factory factory = Factory.getDefaultFactory();
    private final ConnectionManager connectionManager;
    private final MultiPortMap multiPortMap;
    private final CompletableFuture<Connection> streamFuture = new CompletableFuture<>();
    private volatile Subscription subscription;

    final LightstreamerClient client;
    final String adapterSet;
    final String protocol;
    final String host;
    final int port;
    final boolean ssl;
    final int instanceId;

    volatile String id;
    volatile String clinkHost;
    volatile int clinkPort;
    volatile boolean http;
    volatile State state = State.DISCONNECTED;
    
    volatile long createStartTime;
    volatile long bindStartTime;
    volatile long subStartTime;
    
    final CookieHelper localCookieHelper;
    
    public Session(LightstreamerClient client) {
        this.client = client;
        URI uri = LsUtils.uri(client.serverUrl);
        this.protocol = uri.getScheme();
        this.host = uri.getHost();
        this.port = LsUtils.port(uri);
        this.clinkHost = this.host;
        this.clinkPort = this.port;
        this.ssl = LsUtils.isSSL(uri);
        this.adapterSet = client.adapterSet;
        this.instanceId = factory.getInstanceCounter().next();
        this.connectionManager = factory.getConnectionManager();
        this.multiPortMap = factory.getMultiPortMap();
        this.localCookieHelper = new CookieHelper();
    }
    
    public CookieHelper getLocalCookieHelper() {
        return localCookieHelper;
    }
    
    /**
     * Connects to server.
     */
    public void connect() {
        state = State.CONNECTING;
        Stats.createPending.increment();
        createStartTime = System.currentTimeMillis();
        client.fireOnStatusChange("CONNECTING");
        http = client.connectionOptions.getForcedTransport().equals("HTTP-STREAMING");
        connectionManager.connect(this);
    }
    
    /**
     * Binds to the session.
     */
    public void bind() {
        state = State.BINDING;
        Stats.bindPending.increment();
        bindStartTime = System.currentTimeMillis();
        connectionManager.bind(http, this);
    }
    
    /**
     * Subscribes to items.
     */
    public void subscribe(final Subscription sub) {
        execWhenReady((Connection ch) -> {
            Stats.subPending.increment();
            subStartTime = System.currentTimeMillis();
            String subId = String.valueOf(nextSubId.incrementAndGet());
            subscription = sub;
            connectionManager.subscribe(ch, sub, subId, this);
        });
    }
    
    /**
     * Sends a message.
     */
    public void sendMessage(String message) {
        execWhenReady((Connection ch) -> connectionManager.sendMessage(ch, message, this));
    }
    
    /**
     * Disconnects from server.
     */
    public void disconnect() {
        if (state != State.DISCONNECTED) {
            state = State.DISCONNECTED;
            streamFuture.cancel(false);
            streamFuture.whenComplete((Connection ch, Throwable e) -> {
                if (ch != null) {
                    ch.close();
                }
            });
            client.fireOnStatusChange("DISCONNECTED");
        }
    }
    
    public void speedUpReading() {
        execWhenReady((Connection ch) -> ch.speedUpReading());
    }
    
    private void execWhenReady(Consumer<Connection> op) {
        if (streamFuture.isDone()) {
            try {
                Connection ch = streamFuture.get();
                op.accept(ch);
                
            } catch (Exception e) {
                onConnectionError(e);
            }
            
        } else {
            streamFuture.whenComplete((ch, e) -> {
                if (ch != null) {
                    op.accept(ch);
                    
                } else {
                    onConnectionError(e);
                }
            });
        }
    }
    
    /*
     * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     * 
     * Events coming from the stream connection.
     * 
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     */
    
    public void onConnectionError(Throwable e) {
        if (state != State.DISCONNECTED) {
            state = State.DISCONNECTED;
            Stats.connErrors.increment();
            logError(id, e);
            client.fireOnStatusChange("DISCONNECTED");
            client.fireOnServerError(-1, e.getMessage());
            streamFuture.completeExceptionally(e);
        }
    }
    
    public void onSessionError(int code, String error) {
        if (state != State.DISCONNECTED) {
            state = State.DISCONNECTED;
            Stats.socketErrors.increment();
            logError(id, "Error: " + code + " " + error);
            client.fireOnStatusChange("DISCONNECTED");
            client.fireOnServerError(code, error);
            streamFuture.completeExceptionally(BOGUS_EX);
        }
    }
    
    public void onSessionError(Exception e) {
        if (state != State.DISCONNECTED) {
            state = State.DISCONNECTED;
            Stats.socketErrors.increment();
            logError(id, e);
            client.fireOnStatusChange("DISCONNECTED");
            client.fireOnServerError(-1, e.getMessage());
            streamFuture.completeExceptionally(e);
        }
    }
    
    public void onConnected(String sessionId, String clink) {
        state = State.CONNECTED;
        Stats.createDone.increment();
        Stats.notifyCreateDelay(System.currentTimeMillis() - createStartTime);
        this.id = sessionId;
        if (clink != null && ! clink.equals(host)) {
            this.clinkHost = clink;
        }
        this.clinkPort = multiPortMap.getActualPort(clinkHost, port);
        client.fireOnStatusChange("CONNECTED:STREAM-SENSING");        
    }
    
    public void onBound(Connection ch, String status) {
        state = State.BOUND;
        Stats.bindDone.increment();
        Stats.notifyBindDelay(System.currentTimeMillis() - bindStartTime);
        client.fireOnStatusChange(status);
        streamFuture.complete(ch);
    }
    
    public void onSubscription(String subId, int totalItems, int totalFields) {
        Stats.subDone.increment();
        Stats.notifySubDelay(System.currentTimeMillis() - subStartTime);
        if (subscription != null) {            
            subscription.setTotalItems(totalItems);
            subscription.setTotalFields(totalFields);
            subscription.fireOnSubscription();
        }
    }

    public void onUpdate(String subId, int item, List<String> values) {
        subscription.fireOnItemUpdate(new ItemUpdate(item, values, subscription));
    }

}
