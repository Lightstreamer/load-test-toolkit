package com.lightstreamer.load_test.simulator;

import java.io.File;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.lightstreamer.load_test.commons.ConfigurationReader;
import com.lightstreamer.load_test.commons.XmlUtils;

public class FeedSimulatorConfiguration extends ConfigurationReader {
    
    public int numberOfItems = -1;
    public int numberOfFields = -1;
    public int bytesPerField = -1;
    public int itemBurst = -1;
    public int scheduledThreadPoolLength = -1;
    public int initWaitMillis = -1;
    public boolean injectTimestamps = false;
    public boolean useSchedulingTimestamps = false;
    public double updateIntervalMillis = -1;
    public double delayItemStartMillis = -1;
    
    private static final Field[] toReadParams = {
            new Field("numberOfItems",INT,true),
            new Field("numberOfFields",INT,true),
            new Field("bytesPerField",INT,true),
            new Field("itemBurst",INT,true),
            new Field("scheduledThreadPoolLength",INT,true),
            new Field("initWaitMillis",INT,true),
            new Field("injectTimestamps",BOOL,true),
            new Field("useSchedulingTimestamps",BOOL,false),
            new Field("updateIntervalMillis",DOUBLE,true),
            new Field("delayItemStartMillis",DOUBLE,true)
    };
    
    
    public FeedSimulatorConfiguration(String fileName) throws Exception {
        //Read simulation configuration file
        File configurationFile = new File(fileName);
        String configFilePathStr = configurationFile.getAbsolutePath();
        Document doc = null;
        
        doc =  XmlUtils.newDocumentBuilder(configFilePathStr);
        
        NodeList nodeList = doc.getElementsByTagName("data_provider");
        if (nodeList.getLength() != 1) {
            throw new Exception("Wrong configuration file");
        }
        
        Map<String,String> params = XmlUtils.getNodeValue4Attribute(doc, "param", "name");
        this.readConfiguration(params, toReadParams);
        
    }
    
    public FeedSimulatorConfiguration(Map<String,String> params) throws Exception {
        this.readConfiguration(params, toReadParams);
    }   
}
