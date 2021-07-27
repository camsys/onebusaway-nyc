/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
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
	    
	    DefaultSerializerProvider.Impl sp = new DefaultSerializerProvider.Impl();
	    m.setSerializerProvider(sp);
	    
	    PropertyNamingStrategy pns = new AllLowerWithDashesNamingStrategy();
	    m.setPropertyNamingStrategy(pns);

	    return m;
	}

}
