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

package org.onebusaway.nyc.util.logging.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;
import org.onebusaway.nyc.util.logging.LoggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link LoggingService}
 * @author abelsare
 *
 */
public class LoggingServiceImpl implements LoggingService {

	private TransitDataManagerApiLibrary transitDataManagerApiLibrary;
	
	private static final Logger log = LoggerFactory.getLogger(LoggingServiceImpl.class);
	
	@Override
	public String log(String component, Level priority, String message) {
		log.info("Starting to log message : {}", message );
		
		if(StringUtils.isBlank(message)) {
			throw new IllegalArgumentException("Message to log cannot be blank");
		}
		
		String responseText = transitDataManagerApiLibrary.log("log", component,priority.intLevel(), message);
		
		log.info("Returning response from server");
		
		return responseText;
	}

	/**
	 * Injects {@link TransitDataManagerApiLibrary}
	 * @param transitDataManagerApiLibrary the transitDataManagerApiLibrary to set
	 */
	@Autowired
	public void setTransitDataManagerApiLibrary(
			TransitDataManagerApiLibrary transitDataManagerApiLibrary) {
		this.transitDataManagerApiLibrary = transitDataManagerApiLibrary;
	}

}
