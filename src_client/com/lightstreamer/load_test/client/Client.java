package com.lightstreamer.load_test.client;


import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;

import com.lightstreamer.load_test.commons.ClientConfiguration;
import com.lightstreamer.load_test.commons.Constants;
import com.lightstreamer.load_test.commons.XmlUtils;
import com.lightstreamer.oneway_client.netty.Factory;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;



/**
 * This class provides a Client that can generate many simultaneous sessions
 * connected to a Lightstreamer Server. In details, for each session
 * it creates a new thread and all sessions are scheduled at same distance
 * (a time constant delay). The dequeueing of the data can be configured
 * to be in a single thread or in session-dedicated threads; no thread pool
 * based dequeueing is available.
 */
public class Client {
    
    private static Logger _logConf;
    
    /**
     * Reads params in configuration file and starts all sessions.
     * 
     * @param args Should specify the configuration files.
     */
    public static void main(String[] args) {
        try {
            doMain(args);
            System.exit(0);
            
        } catch (Throwable e) {
            _logConf.error("Fatal error", e);
            System.exit(1);
        }
    }
    
    private static void doMain(String[] args) {
        if (args.length < 2) {
            exit("Usage: [<log-config-file> <client-config-file> <client-config-file2> ...]\n\rDifferent client-config-file will be concatenated",1,null);
        }
        
        try {
            /* Configure log4j */
            DOMConfigurator.configureAndWatch(args[0]);
            /* Configure Netty logger to use log4j */
            InternalLoggerFactory.setDefaultFactory(Log4JLoggerFactory.INSTANCE);
            
        } catch (Exception e) {
            exit(e.getMessage(),2,e);
        }
        
        _logConf = Logger.getLogger(Constants.CONFIGURATION_LOGGER);
        
        _logConf.info("Reading local configuration...");
        Map<String,String> params = new HashMap<String,String>();
        for (int i=1; i<args.length; i++) {
            try {
                Document doc = XmlUtils.newDocumentBuilder(args[1]);
                params.putAll(XmlUtils.getNodeValue4Attribute(doc, "param", "name"));
            } catch(Exception e) {
                exit("Can't read configuration file " + args[i],3,e);
            }
        }
        
        ClientConfiguration fullConf = new ClientConfiguration();
        try {
            fullConf.readClientConfiguration(params);
        } catch(Exception e) {
            exit("Failed reading configuration",5,e);
        }
        fullConf.generateDataNeededValue();

        _logConf.info("Reading remote configuration...");

        FirstClient fc = new FirstClient();

        HashMap<String,String> remoteConf = null;
        try {
            remoteConf = /*CAN BLOCK*/ fc.readRemoteConf(fullConf);
        } catch(Exception e) {
            exit("Failed reading remote configuration",6,e);
        }
        if (remoteConf == null) {
            exit("Failed reading remote configuration",6,null);
        }
        _logConf.debug("Remote configuration: " + remoteConf);
        
        //remote conf overrides local conf 
        //(at the moment nothing is duplicated between the two so that no overriding can happen)
        try {
            fullConf.readSeverSentClientConfiguration(remoteConf);
        } catch(Exception e) {
            exit("Failed merging configurations",7,e);
        }
        
        checkConf(fullConf);
        
        // NB configure the static factory used by LightstreamerClients
        Factory.configure(
                // will ignore the data coming from the streaming connections when
                // 1) the parameter ignoreData in configuration.xml is set to true, and
                // 2) the logger "com.lightstreamer.load_test.reports.session_events" is not at debug level (or lower)
                //    and the logger "com.lightstreamer.load_test.reports.latency_reporting" is not at info level (or lower)
                fullConf.isIgnoreData(),
                fullConf.nettyThreads, 
                fullConf.minCreatePool, 
                fullConf.serverPorts, 
                false, // disable experimental NIO transport: use Netty instead 
                1, // used only if experimental NIO transport is enabled
                1, // used only if experimental NIO transport is enabled
                fullConf.listenerThreads,
                false /*select sockets using FIFO policy*/);
        SessionsHandler sh = new SessionsHandler(fullConf);
        
        sh.start();
    }
    
    
    private static void checkConf(ClientConfiguration fullConf) {
        if (fullConf.lastItemAvailable < fullConf.firstItemAvailable) {
            exit("lastItemAvailable must not be smaller than firstItemAvailable",23,null);
        }
        if (fullConf.firstItemAvailable > fullConf.numberOfItems) {
            exit("firstItemAvailable can't be greater than numberOfItemsAvailable (numberOfItemsAvailable: "+fullConf.numberOfItems+"; firstItemAvailable: " +fullConf.firstItemAvailable+")",24,null);
        }
        if (fullConf.lastItemAvailable > fullConf.numberOfItems) {
            exit("lastItemAvailable can't be greater than numberOfItemsAvailable (numberOfItemsAvailable: "+fullConf.numberOfItems+"; lastItemAvailable: " +fullConf.lastItemAvailable+")",25,null);
        }
        int actualAvailable = fullConf.lastItemAvailable>0 ? fullConf.lastItemAvailable-(fullConf.firstItemAvailable>0?fullConf.firstItemAvailable:1)+1 : fullConf.numberOfItems;
        if (!fullConf.itemRandomExtraction) {
            if (actualAvailable < fullConf.itemsPerSession) {
                exit("if itemRandomExtraction is disabled the itemsPerSession must be less than numberOfItemsAvailable (or, if set, less than the range firstItemAvailable/lastItemAvailable)",26,null);
            }
        }
        
    }


    private static void exit(String mex, int code, Exception e) {
        if (_logConf!=null) {
            if (e != null) {
                _logConf.fatal(mex,e);
            } else {
                _logConf.fatal(mex);
            }
        }
        System.out.println(mex);
        System.exit(code);
    }
    

}
