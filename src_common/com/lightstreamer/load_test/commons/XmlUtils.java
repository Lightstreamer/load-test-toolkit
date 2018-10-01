package com.lightstreamer.load_test.commons;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Utility class used to read values in an xml file.
 */
public class XmlUtils {

    /**
     * Builds a new document from a file.
     */
    public static Document newDocumentBuilder(String fileName) throws Exception {
        try {
            File cfgFile = new File(fileName);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc = parser.parse(cfgFile);
            return doc;
        } catch (FactoryConfigurationError fce) {
             throw new Exception(fce);
        } catch (ParserConfigurationException pce) {
            throw new Exception(pce);
        } catch (SAXException saxe) {
            throw new Exception(saxe);
        } catch (IOException ioe) {
            throw new Exception(ioe);
        }
    }
    
    /**
     * <pre>
     * 
     * Reads values in a tag, such as in the following example:
     * 
     *  Xml:                                                        |    Result:
     *                                                                 |
     *                                                                 |      key             value
     *     <nodeName attributeName="name1">value1</nodeName>            |     "name1"       "value1"
     *     <nodeName attributeName="name2">value2</nodeName>            |     "name2"       "value2"
     *     <nodeName attributeName="name3">value3</nodeName>            |     "name3"       "value3"
     *                                                                 |
     * </pre>
     */
    public static Map<String,String> getNodeValue4Attribute(Document doc, String nodeName, String attributeName) throws Exception {
        String defineTagErr = nodeName + " tag is not correctly defined";
        String defineAttribute4Tag = attributeName + " name of tag " + nodeName + " is not correctly defined";
        
        HashMap<String,String> result = new HashMap<String,String>();
        NodeList nodeList = doc.getElementsByTagName(nodeName);
        
        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getFirstChild() == null) throw new Exception(defineTagErr); 
            String category = node.getFirstChild().getNodeValue();
            
            NamedNodeMap attributes = node.getAttributes();
            if (attributes == null) throw new Exception(defineAttribute4Tag);
            
            Node attribute = attributes.getNamedItem(attributeName);
            if (attribute == null) throw new Exception(defineAttribute4Tag);
            
            String attributeValue = attribute.getNodeValue();
            if (attributeValue == null || attributeValue.length() == 0) throw new Exception(defineAttribute4Tag);
            
            result.put(attributeValue, category);
        }
        
        return result;
    }
    
    /**
     * Returns the value of a parameter previously read in a map (with the above method).
     */
    public static String getStringParameter(Map<String,String> params, String name, boolean mandatory) throws Exception {
        String value = params.get(name);
        if (value == null && mandatory) {
            throw new Exception("Can't find param " + name);
        }
        return value;
    }
    
    /**
     * Returns the value of an Integer parameter previously read in a map (with method "getNodeValue4Attribute(Document, String, String)" in parent
     * class).
     */
    public static Integer getPositiveIntParameter(Map<String,String> params, String name, boolean mandatory) throws Exception {
        String strParameter = XmlUtils.getStringParameter(params, name, mandatory);
        if (strParameter == null) {
            return null;
        }
        Integer intParameter = new Integer(strParameter);
        if (intParameter <= 0) {
            throw new Exception(name + " must be grater than zero");
        }
        return intParameter;
    }
    
    
    /**
     * Returns the value of a Long parameter previously read in a map (with method "getNodeValue4Attribute(Document, String, String)" in parent
     * class).
     */
    public static Long getPositiveLongParameter(Map<String,String> params, String name, boolean mandatory) throws Exception {
        String strParameter = XmlUtils.getStringParameter(params, name, mandatory);
        if (strParameter == null) {
            return null;
        }
        Long longParameter = new Long(strParameter);
        if (longParameter <= 0) {
            throw new Exception(name + " must be grater than zero");
        }
        return longParameter;
    }
    

    /**
     * Returns the value of a Double parameter previously read in a map (with method "getNodeValue4Attribute(Document, String, String)" in parent
     * class).
     */
    public static Double getPositiveDoubleParameter(Map<String,String> params, String name, boolean mandatory) throws Exception {
        String strParameter = XmlUtils.getStringParameter(params, name, mandatory);
        if (strParameter == null) {
            return null;
        }
        Double doubleParameter = new Double(strParameter);
        if (doubleParameter <= 0) {
            throw new Exception(name + " must be grater than zero");
        }
        return doubleParameter;
    }

}
