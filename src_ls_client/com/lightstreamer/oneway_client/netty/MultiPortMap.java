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
