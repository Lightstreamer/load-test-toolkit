package com.lightstreamer.oneway_client.netty;

import java.util.concurrent.ConcurrentHashMap;

import com.lightstreamer.oneway_client.LightstreamerClient;

/**
 * Maps host addresses to the next ports to use to make connections.
 *  
 * @author Alessandro Carioni
 * @since August 2018
 */
public class MultiPortMap {
    
    private final ConcurrentHashMap<String, ModularCounter> map = new ConcurrentHashMap<>();
    private final int nPorts;
    
    /**
     * Creates a port mapper.
     * 
     * @param nPorts number of ports on which an instance server is listening to.
     */
    public MultiPortMap(int nPorts) {
        this.nPorts = nPorts;
    }

    /**
     * Returns the next port to which connects.
     * 
     * @param host host address
     * @param basePort port specified in {@link LightstreamerClient} constructor.
     */
    public int getActualPort(String host, int basePort) {
        return basePort + map.computeIfAbsent(host, (key) -> new ModularCounter(nPorts)).next();
    }
    
}
