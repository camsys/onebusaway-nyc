package org.onebusaway.nyc.transit_data_manager.logging;

import java.io.IOException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.SessionFactory;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.Message;
import org.onebusaway.nyc.transit_data_manager.util.DateUtility;
import org.onebusaway.nyc.transit_data_manager.util.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

	private SessionFactory _sessionFactory;
	
	private static final Logger log = LoggerFactory.getLogger(SystemLogResource.class);
	
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
			
			_sessionFactory.getCurrentSession().saveOrUpdate(logRecord);
			
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

	@Autowired
	@Qualifier("archiveSessionFactory")
	public void setSessionFactory(SessionFactory sessionFactory) {
		_sessionFactory = sessionFactory;
	}

}
