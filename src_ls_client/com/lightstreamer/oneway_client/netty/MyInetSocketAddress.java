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

import java.net.InetSocketAddress;

public class MyInetSocketAddress {

    public final InetSocketAddress address;
    public final boolean ssl;
    public final int instanceId;

    public MyInetSocketAddress(String hostname, int port, boolean ssl, int instanceId) {
        this.address = new InetSocketAddress(hostname, port);
        this.ssl = ssl;
        this.instanceId = instanceId;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + instanceId;
        result = prime * result + (ssl ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MyInetSocketAddress other = (MyInetSocketAddress) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (instanceId != other.instanceId)
            return false;
        if (ssl != other.ssl)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MyInetSocketAddress [address=" + address + ", ssl=" + ssl + ", instanceId=" + instanceId + "]";
    }
    
}
