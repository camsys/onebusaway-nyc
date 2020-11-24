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

package org.onebusaway.nyc.transit_data_manager.logging;

import java.io.IOException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;
import org.onebusaway.nyc.transit_data_manager.persistence.service.SystemLogPersistenceService;
import org.onebusaway.nyc.transit_data_manager.util.DateUtility;
import org.onebusaway.nyc.transit_data_manager.util.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * System wide resource to provide logging service. When requested, it performs logging of caller actions
 * to the persistent storage.
 * @author abelsare
 *
 */
@Path("/log")
@Component
@Scope("request")
public class SystemLogResource {
	
	private ObjectMapper mapper;
	
	private static final Logger log = LoggerFactory.getLogger(SystemLogResource.class);
	
	@Autowired
	private SystemLogPersistenceService _systemLogService;
	
	public SystemLogResource() {
		mapper = ObjectMapperProvider.getObjectMapper();
	}
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String log(String messageJSON) throws JsonGenerationException, JsonMappingException, IOException {
		
		log.info("Starting to log message");
		LogMessage logMessage = null;
		Message message = new Message();
		
		try {
			
			logMessage = mapper.readValue(messageJSON, LogMessage.class);
			
			SystemLogRecord logRecord = buildSystemLogRecord(logMessage);
			
			_systemLogService.saveLogRecord(logRecord);
			
			message.setStatus("OK");
			
		} catch(JsonParseException parseException) {
			message.setStatus("ERROR");
			message.setMessageText("Error parsing message JSON content");
			parseException.printStackTrace();
		} catch(JsonMappingException mappingException) {
			message.setStatus("ERROR");
			message.setMessageText("Error mapping JSON string to object");
			mappingException.printStackTrace();
		} catch(IOException e) {
			message.setStatus("ERROR");
			message.setMessageText("I/O error parsing message JSON content");
			e.printStackTrace();
		} catch(HibernateException hibernateException) {
			message.setStatus("ERROR");
			message.setMessageText("Error saving SystemLogRecord");
			hibernateException.printStackTrace();
		}
		
		log.info("Returning response text");
		return mapper.writeValueAsString(message);
	}
	
	private SystemLogRecord buildSystemLogRecord(LogMessage logMessage) {
		SystemLogRecord logRecord = new SystemLogRecord();
		
		logRecord.setComponent(logMessage.getComponent());
		logRecord.setMessage(logMessage.getMessage());
		logRecord.setMessageDate(getDate());
		logRecord.setPriority(logMessage.getPriority());
		
		return logRecord;
	}
	
	private Date getDate() {
		return DateUtility.getTodaysDateTime();
	}

}
