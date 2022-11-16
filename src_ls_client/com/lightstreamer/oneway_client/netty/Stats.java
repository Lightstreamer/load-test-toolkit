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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Statistics about sessions.
 */
public class Stats {

    public static final LongAdder createPending = new LongAdder();
    public static final LongAdder createDone = new LongAdder();
    public static final AtomicLong maxCreateDelay = new AtomicLong();
    
    public static final LongAdder bindPending = new LongAdder();
    public static final LongAdder bindDone = new LongAdder();
    public static final AtomicLong maxBindDelay = new AtomicLong();
    
    public static final LongAdder subPending = new LongAdder();
    public static final LongAdder subDone = new LongAdder();
    public static final AtomicLong maxSubDelay = new AtomicLong();
    
    /**
     * Number of sockets closed due to connection problems.
     */
    public static final LongAdder connErrors = new LongAdder();
    public static final LongAdder socketErrors = new LongAdder();
    
    /**
     * The amount of bytes received by all the sessions.
     */
    public static final LongAdder bytesRead = new LongAdder();
    
    public static void notifyCreateDelay(long delay) {
        setMax(delay, maxCreateDelay);
    }
    
    public static void notifyBindDelay(long delay) {
        setMax(delay, maxBindDelay);
    }
    
    public static void notifySubDelay(long delay) {
        setMax(delay, maxSubDelay);
    }
    
    private static void setMax(long curr, AtomicLong max) {
        if (bindDone.intValue() > 200) {
            // skip the first sessions which discount Netty bootstrap
            synchronized (max) {
                max.set(Math.max(curr, max.longValue()));
            }
        }
    }
}
