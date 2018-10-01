package com.lightstreamer.oneway_client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;

import com.lightstreamer.oneway_client.netty.Factory;

public class Subscription {

    private final ArrayList<SubscriptionListener> listeners = new ArrayList<>(1);
    private final Object lock = new Object();
    private final String mode;
    private String[] items;
    private NameDesc itemDesc;
    private String[] fields;
    private NameDesc fieldDesc;
    private String snapshot;
    private String dataAdapter;
    private String group;
    private String schema;
    private String frequency;
    private String buffer;
    private int totalItems;
    private int totalFields;
    private final Executor executor = Factory.getDefaultFactory().getListenerExecutor();
    
    public Subscription(String mode) {
        this.mode = mode;
    }
    
    public void addListener(SubscriptionListener clientListener) {
        synchronized (listeners) {
            listeners.add(clientListener);
        }
    }
    
    public void fireOnSubscription() {
        synchronized (listeners) {
            for (SubscriptionListener listener : listeners) {
                executor.execute(listener::onSubscription);
            }
        }
    }
    
    public void fireOnItemUpdate(ItemUpdate itemUpdate) {
        synchronized (listeners) {
            for (SubscriptionListener listener : listeners) {
                executor.execute(() -> listener.onItemUpdate(itemUpdate));
            }
        }
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setItems(String[] items) {
        synchronized (lock) {
            this.items = items;
            this.itemDesc = new NameDesc(items);
        }
    }
    
    public String[] getItems() {
        synchronized (lock) {
            return items;
        }
    }
    
    public NameDesc getItemDesc() {
        synchronized (lock) {
            return itemDesc;
        }
    }

    public void setFields(String[] fields) {
        synchronized (lock) {
            this.fields = fields;
            this.fieldDesc = new NameDesc(fields);
        }
    }
    
    public String[] getFields() {
        synchronized (lock) {
            return fields;
        }
    }
    
    public NameDesc getFieldDesc() {
        synchronized (lock) {
            return fieldDesc;
        }
    }

    public void setRequestedSnapshot(String snap) {
        synchronized (lock) {
            this.snapshot = snap;
        }
    }
    
    public String getRequestedSnapshot() {
        synchronized (lock) {
            return snapshot;
        }
    }

    public void setDataAdapter(String dataAdapterName) {
        synchronized (lock) {
            this.dataAdapter = dataAdapterName;
        }
    }
    
    public String getDataAdapter() {
        synchronized (lock) {
            return dataAdapter;
        }
    }

    public void setItemGroup(String group) {
        synchronized (lock) {
            this.group = group;
            this.itemDesc = new NameDesc(group);
        }
    }
    
    public String getItemGroup() {
        synchronized (lock) {            
            return group;
        }
    }

    public void setFieldSchema(String schemaName) {
        synchronized (lock) {
            this.schema = schemaName;
            this.fieldDesc = new NameDesc(schemaName);
        }
    }
    
    public String getFieldSchema() {
        synchronized (lock) {
            return schema;
        }
    }

    public void setRequestedMaxFrequency(String freq) {
        synchronized (lock) {
            this.frequency = freq;
        }
    }
    
    public String getRequestedMaxFrequency() {
        synchronized (lock) {
            return frequency;
        }
    }

    public void setRequestedBufferSize(String size) {
        synchronized (lock) {
            this.buffer = size;
        }
    }
    
    public String getRequestedBufferSize() {
        synchronized (lock) {
            return buffer;
        }
    }

    public void setTotalItems(int totalItems) {
        synchronized (lock) {
            this.totalItems = totalItems;
        }
    }

    public void setTotalFields(int totalFields) {
        synchronized (lock) {
            this.totalFields = totalFields;
        }
    }
    
    public static class NameDesc {
        
        public final HashMap<String, Integer> nameMap = new HashMap<>();
        public final String names;
        
        NameDesc(String names) {
            this(names.split(" "));
        }
        
        NameDesc(String[] names) {
            String tmp = null;
            for (int i = 0; i < names.length; i++) {
                if (i == 0) {
                    tmp = names[i];
                } else {
                    tmp += " " + names[i];
                }
                nameMap.put(names[i], i);
            }
            this.names = tmp;
        }
    }

}
