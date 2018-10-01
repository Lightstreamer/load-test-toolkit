package com.lightstreamer.load_test.commons;

import java.util.Map;

import org.apache.log4j.Logger;

public class ConfigurationReader {
    
    protected static Logger _log = Logger.getLogger(Constants.CONFIGURATION_LOGGER);
    
    public static final short INT = 1;
    public static final short STRING = 2;
    public static final short DOUBLE = 3;
    public static final short LONG = 4;
    public static final short BOOL = 5;
    
    
    protected void readConfiguration(Map<String,String> params, 
            Field[] toReadParams) throws Exception {
        
        for (int i=0; i<toReadParams.length; i++) {
            this.readParam(toReadParams[i], params);
        }
    }
    
    protected String getString(Field field) {
        try {
            java.lang.reflect.Field fieldToRead = this.getClass().getDeclaredField(field.name);
            

            
            switch(field.type) {
                case INT: 
                    Integer intVal = fieldToRead.getInt(this);
                    if (intVal == null || intVal == -1) {
                        return null;
                    }
                    return String.valueOf(intVal);
                case STRING:
                    String strVal = (String) fieldToRead.get(this);
                    if (strVal == null) {
                        return null;
                    }
                    return strVal;
                case DOUBLE:
                    Double doubleVal = fieldToRead.getDouble(this);
                    if (doubleVal == null || doubleVal == -1) {
                        return null;
                    }
                    return String.valueOf(doubleVal);
                case LONG:
                    Long longVal = fieldToRead.getLong(this);
                    if (longVal == null || longVal == -1) {
                        return null;
                    }
                    return String.valueOf(longVal);
                case BOOL:    
                    if (fieldToRead.getBoolean(this)) {
                        return "true";
                    } else {
                        return "false";
                    }
            }
            
            return null;
            
            
            
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        
        return "";
    }
    
    private void readParam(Field paramToRead, Map<String,String> params) throws Exception {
       
        java.lang.reflect.Field fieldToRead = this.getClass().getDeclaredField(paramToRead.name);
        String readStr;

        switch(paramToRead.type) {
            case INT: 
                Integer readInt = XmlUtils.getPositiveIntParameter(params, paramToRead.name, paramToRead.mandatory);
                if (readInt != null) {
                    fieldToRead.setInt(this, readInt.intValue());
                    _log.info(paramToRead.name + " read: " + readInt);
                }
                break;
            
            case STRING:
                readStr = XmlUtils.getStringParameter(params, paramToRead.name, paramToRead.mandatory);
                if (readStr != null) {
                    fieldToRead.set(this, readStr);
                    _log.info(paramToRead.name + " read: " + readStr);
                }
                break;
                
            case DOUBLE:
                Double readDouble = XmlUtils.getPositiveDoubleParameter(params, paramToRead.name, paramToRead.mandatory);
                if (readDouble != null) {
                    fieldToRead.setDouble(this,readDouble.doubleValue());
                    _log.info(paramToRead.name + " read: " + readDouble);
                }
                break;
            case LONG:
                Long readLong = XmlUtils.getPositiveLongParameter(params, paramToRead.name, paramToRead.mandatory);
                if (readLong != null) {
                    fieldToRead.setLong(this, readLong.longValue());
                    _log.info(paramToRead.name + " read: " + readLong);
                }
                break;
                
            case BOOL:
                readStr = XmlUtils.getStringParameter(params, paramToRead.name, paramToRead.mandatory);
                if (readStr != null) {
                    fieldToRead.setBoolean(this, readStr.equalsIgnoreCase("true"));
                    _log.info(paramToRead.name + " read: " + readStr);
                }
                break;
                
                
            default:
                throw new Exception("Unexpected Parameter type");
        }
       
    }
    
    public static class Field {
        public String name;
        public short type;
        public boolean mandatory;
        
        public Field(String name, short type, boolean mandatory) {
            this.name = name;
            this.type = type;
            this.mandatory = mandatory;
        }
    }
    
}
