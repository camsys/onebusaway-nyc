package org.onebusaway.nyc.transit_data_manager.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.onebusaway.nyc.transit_data_manager.config.AllLowerWithDashesNamingStrategy;

/**
 * Utility to providers jackson object mapper.
 * @author abelsare
 *
 */
public class ObjectMapperProvider {
	
	/**
	 * Creates a jackson object mapper with lower and dashes naming strategy
	 * @return jackson object mapper
	 */
	public static ObjectMapper getObjectMapper() {
		 // this code was taken from
	    // http://wiki.fasterxml.com/JacksonHowToCustomSerializers
	    // Basically it sets up a serializer for null values, so that they are
	    // mapped to an empty string.

	    ObjectMapper m = new ObjectMapper();
	    
	    StdSerializerProvider sp = new StdSerializerProvider();
	    m.setSerializerProvider(sp);
	    
	    PropertyNamingStrategy pns = new AllLowerWithDashesNamingStrategy();
	    m.setPropertyNamingStrategy(pns);

	    return m;
	}

}
