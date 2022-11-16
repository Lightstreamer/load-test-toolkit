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
