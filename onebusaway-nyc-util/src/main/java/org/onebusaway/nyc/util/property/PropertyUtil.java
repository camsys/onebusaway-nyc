package org.onebusaway.nyc.util.property;

public class PropertyUtil {
    public static String getProperty(String propertyKey){
        String property = System.getProperty(propertyKey);
        if(property==null){
            property = System.getenv(propertyKey);
        }
        return property;
    }
}
