package com.lightstreamer.oneway_client.netty;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A thread-safe counter counting from 0 to a given number.
 */
@ThreadSafe
public class ModularCounter {
    
    private int cnt;
    private int max;

    /**
     * Creates a counter counting form {@code 0} to {@code period - 1}.
     */
    public ModularCounter(int period) {
        this.max = period - 1;
        this.cnt = 0;
    }
    
    public synchronized int next() {
        int next = cnt;
        cnt = (cnt == max ? 0 : cnt + 1);
        return next;
    }
    
}
