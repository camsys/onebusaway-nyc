package org.onebusaway.nyc.transit_data_manager.util;

import org.onebusaway.nyc.transit_data_manager.config.AllLowerWithDashesNamingStrategy;
import org.onebusaway.nyc.transit_data_manager.config.NullSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

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

	    ObjectMapper mapper = new ObjectMapper();
	    
	    mapper.registerModule(new AfterburnerModule());	    
	    mapper.getSerializerProvider().setNullValueSerializer(new NullSerializer());
	    
	    PropertyNamingStrategy pns = new AllLowerWithDashesNamingStrategy();
	    mapper.setPropertyNamingStrategy(pns);

	    return mapper;
	}
}


