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
