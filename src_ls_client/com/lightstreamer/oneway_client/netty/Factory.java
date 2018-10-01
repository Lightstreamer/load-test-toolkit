package com.lightstreamer.oneway_client.netty;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.lightstreamer.oneway_client.ClientListener;
import com.lightstreamer.oneway_client.SubscriptionListener;

/**
 * Singleton factory.
 * 
 * @author Alessandro Carioni
 * @since August 2018
 */
public abstract class Factory {
    
    public abstract ConnectionManager getConnectionManager();
    
    public abstract MultiPortMap getMultiPortMap();
    
    public abstract ModularCounter getInstanceCounter();
    
    public abstract Executor getListenerExecutor();
    
    private static Factory defaultFactory;
    
    public static synchronized Factory getDefaultFactory() {
        if (defaultFactory == null) {
            defaultFactory = new DefaultFactory(1, 1, new ConnectionManager(false, -1, true), Executors.newFixedThreadPool(1));
        }
        return defaultFactory;
    }
    
    /**
     * TEST ONLY
     */
    public static synchronized void setDefaultFactory(Factory factory) {
        defaultFactory = factory;
    }
    
    /**
     * TEST ONLY
     */
    public static synchronized void configure(int minCreatePool, int nPorts) {
        configure(false, -1, minCreatePool, nPorts, false, -1, -1, 1, true);
    }
    
    /**
     * Configure the factory.
     * 
     * @param ignoreData when true, the client skips messages on the data connection
     * @param nettyThreads the number of threads allocated to Netty
     * @param minCreatePool the minimum number of sockets used to send create_session requests. 
     * If the number is more than one, the client rotates the sockets.
     * @param nPorts number of the ports on which each instance server is listening to.
     * If the number is more than one, the client sends bind_session requests on each port at turn starting from
     * the port specified in LightstreamerClient constructor.
     * @param nio if true, the client uses an experimental NIO transport.
     * @param nioThreads the number of threads to allocate to create NIO sockets (used only if nio flag is true).
     * @param selectorThreads number of threads to allocate to read from NIO selectors (used only if nio flag is true).
     * @param nListenerThreads the number of threads to allocate to the executor firing the methods of 
     * {@link ClientListener} and {@link SubscriptionListener}.
     * @param {@code true} sockets selection will be LIFO, if {@code false} FIFO
     */
    public static synchronized void configure(
            boolean ignoreData, 
            int nettyThreads, 
            int minCreatePool, 
            int nPorts, 
            boolean nio, 
            int nioThreads,
            int selectorThreads, 
            int nListenerThreads,
            boolean lastRecentUsed) {
        Logger.info("Reconfigure factory settings");
        Logger.info("Param ignoreData: " + ignoreData);
        Logger.info("Param nettyThreads: " + nettyThreads);
        Logger.info("Param minCreatePool: " + minCreatePool);
        Logger.info("Param nPorts: " + nPorts);
        Logger.info("Param useNio: " + nio);
        Logger.info("Param nioThreads: " + nioThreads);
        Logger.info("Param selectorThreads: " + selectorThreads);
        Logger.info("Param listenerThreads: " + nListenerThreads);
        defaultFactory = new DefaultFactory(
                minCreatePool, 
                nPorts, 
                (nio ? new NioConnectionManager(nioThreads, selectorThreads) : new ConnectionManager(ignoreData, nettyThreads, lastRecentUsed)),
                Executors.newFixedThreadPool(nListenerThreads));
    }
    
    public static class DefaultFactory extends Factory {
        
        final ConnectionManager connectionManager;
        final MultiPortMap multiPortMap;
        final ModularCounter instanceCounter;
        final Executor listenerExecutor;
        
        public DefaultFactory(int nInstances, int nPorts, ConnectionManager cm, Executor listenerExecutor) {
            this.connectionManager = cm;
            this.multiPortMap = new MultiPortMap(nPorts);
            this.instanceCounter = new ModularCounter(nInstances);
            this.listenerExecutor = listenerExecutor;
        }
        
        @Override
        public ConnectionManager getConnectionManager() {
            return connectionManager;
        }
        
        @Override
        public MultiPortMap getMultiPortMap() {
            return multiPortMap;
        }
        
        @Override
        public ModularCounter getInstanceCounter() {
            return instanceCounter;
        }

        @Override
        public Executor getListenerExecutor() {
            return listenerExecutor;
        }
    }
    
}
