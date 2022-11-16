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


package com.lightstreamer.oneway_client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemUpdate {

    private final List<String> values;
    private final Subscription sub;
    private final int item;

    public ItemUpdate(int item, List<String> values, Subscription sub) {
        this.item = item;
        this.values = values;
        this.sub = sub;
    }

    public String getValue(int index) {
        return values.get(index - 1);
    }
    
    public String getValue(String item) {
        int index = sub.getFieldDesc().nameMap.get(item);
        return getValue(index + 1);
    }

    public int getItemPos() {
        return item;
    }

    public Map<Integer, String> getFieldsByPosition() {
        HashMap<Integer, String> m = new HashMap<>();
        for (int i = 0, len = values.size(); i < len; i++) {
            m.put(i + 1, values.get(i));
        }
        return m;
    }

}
