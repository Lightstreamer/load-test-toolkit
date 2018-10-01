
package com.lightstreamer.load_test.adapter;


import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.lightstreamer.interfaces.data.DataProviderException;
import com.lightstreamer.interfaces.data.FailureException;
import com.lightstreamer.interfaces.data.ItemEventListener;
import com.lightstreamer.interfaces.data.SmartDataProvider;
import com.lightstreamer.interfaces.data.SubscriptionException;
import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.load_test.simulator.FeedListener;
import com.lightstreamer.load_test.simulator.FeedSimulator;
import com.lightstreamer.load_test.simulator.FeedSimulatorConfiguration;



/**
 * This Adapter accepts only item names that start with a special string prefix (that is set in configuration file)
 * or the special CONFIGURATION item. It receives updates
 * from a simulator and forwards them to Lightstreamer kernel. It discards events that don't pertain to currently
 * subscribed items.
 */
public class AdapterSimulator implements SmartDataProvider {

    private static Logger _log;
    
    private ClientConfiguration clientConf = new ClientConfiguration();

    private ItemEventListener listener = null;
    private final ConcurrentHashMap<String,Object> subscribedItems = new ConcurrentHashMap<String,Object>();
    
    private static final String CONFIGURATION_ITEM = "CONFIGURATION";

    
    /**
     * Initializes object with values that are set in adapters.xml configuration file. 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void init(Map paramsMap, File configDir) throws DataProviderException {
        File logConfigurationFile = new File(configDir.getAbsolutePath()+"/adapter_log_conf.xml");
        String logConfigFilePathStr = logConfigurationFile.getAbsolutePath();
        DOMConfigurator.configureAndWatch(logConfigFilePathStr);
        _log = Logger.getLogger(AdapterSimulator.class);
        
        
        final FeedSimulatorConfiguration simulatorConf;
        try {
            simulatorConf = new FeedSimulatorConfiguration(paramsMap);
            clientConf.readSeverSentClientConfiguration(paramsMap);
        } catch (Exception e) {
            throw new DataProviderException(e.getMessage());
        }
        
        checkConf(clientConf);
      
        final FeedSimulator feedSimulator = new FeedSimulator(simulatorConf);
        feedSimulator.setFeedListener(new LocalFeedListener());

        new Thread() {
            public void run() {
                _log.info("Init date: " + new Date());
                
                feedSimulator.start();
                                   
                _log.info(simulatorConf.numberOfItems + " items started.");
            }
        }.start();
        
    }

    public void setListener(ItemEventListener listener) {
        this.listener = listener;
    }

  
    public void subscribe(String itemName, boolean needsIterator) throws SubscriptionException, FailureException {
        //never called
    }

    /**
     * Manages a request to subscribe an item. It is necessary to use smart update.
     */
    public void subscribe(String itemName, Object itemHandle, boolean needsIterator) throws SubscriptionException, FailureException {
        _log.debug("Received request to subscribe " + itemName);
        
        if (itemName.equals(CONFIGURATION_ITEM)) {
            _log.debug("Configuration item subscribed");
            this.generateConfigurationUpdate(itemHandle);
            
        } else if (!itemName.startsWith(Constants.ITEM_PREFIX)) {
            _log.error("Unexpected item: " + itemName);
            throw new SubscriptionException("Unexpected item: " + itemName);
        } else {
            // Inserts item in the list of subscibed items.
            subscribedItems.put(itemName, itemHandle);
            _log.debug(itemName + " has been inserted in subscribed items list");
        }
    }
    
    private void generateConfigurationUpdate(Object itemHandle) {
        HashMap<String,String> clientConfiguration = this.clientConf.getServerSentClientConfigurationMap();
        this.listener.smartUpdate(itemHandle, clientConfiguration, true);
    }


    /**
     * Manages a request to unsubscribe an item.
     */
    public void unsubscribe(String itemName) throws SubscriptionException, FailureException {
        _log.debug("Received request to unsubscribe " + itemName);
        
        if (itemName.equals(CONFIGURATION_ITEM)) {
            //nothing to do, this item has only the snapshot
        } else if (!itemName.startsWith(Constants.ITEM_PREFIX)) {
            _log.error("Unexpected item: " + itemName);
            throw new SubscriptionException("Unexpected item: " + itemName);
        } else {
            // Removes item from the list of subscibed items.
            subscribedItems.remove(itemName);
            _log.debug(itemName + " has been removed from subscribed items list");
        }
    }

    public boolean isSnapshotAvailable(String itemName) throws SubscriptionException {
        return itemName.equals(CONFIGURATION_ITEM);
    }

    /*
     * Listener of events received from Simulator.
     */
    private class LocalFeedListener implements FeedListener {

        public void onEvent(String itemName, final HashMap<String,byte[]> currentValues) {
            //no need to clone the HashMap as the map is not stored and/or reused by the simulator
            
            // It discards updates about items that are not subscribed.
            if (!subscribedItems.containsKey(itemName)) {
                return;
            }
                
            Object itemHandle = subscribedItems.get(itemName);
            
            listener.smartUpdate(itemHandle, currentValues, false);
        }
        
    }
    
    private static void checkConf(ClientConfiguration clientConf) throws DataProviderException {
        if (!clientConf.subscriptionMode.equals("MERGE") && !clientConf.subscriptionMode.equals("DISTINCT") && !clientConf.subscriptionMode.equals("COMMAND") && !clientConf.subscriptionMode.equals("RAW")) {
            String err = clientConf.subscriptionMode + " is not a valid subscription mode, use MERGE COMMAND DISTINCT or RAW";
            _log.error(err);
            throw new DataProviderException(err);
        }
        
        if (clientConf.subscriptionMode.equals("RAW") && clientConf.unfilteredSubscription) {
            clientConf.unfilteredSubscription = false;
            _log.info("RAW mode is always unfiltered, no need to set unfilteredSubscription to true");
        }
        
    }
}
