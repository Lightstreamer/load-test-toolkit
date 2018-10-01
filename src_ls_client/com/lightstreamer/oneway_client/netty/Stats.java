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
