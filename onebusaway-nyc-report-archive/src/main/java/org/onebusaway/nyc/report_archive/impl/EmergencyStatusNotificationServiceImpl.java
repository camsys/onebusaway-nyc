package org.onebusaway.nyc.report_archive.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.report_archive.event.SNSApplicationEventData;
import org.onebusaway.nyc.report_archive.event.handlers.SNSApplicationEventPublisher;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.EmergencyStatusNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.ServletContextAware;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Default implementation of {@link EmergencyStatusNotificationService}
 * @author abelsare
 *
 */
public class EmergencyStatusNotificationServiceImpl implements EmergencyStatusNotificationService, ServletContextAware, ApplicationListener<ContextRefreshedEvent> {

  protected static Logger _log = LoggerFactory.getLogger(EmergencyStatusNotificationServiceImpl.class);
  
	private SNSApplicationEventPublisher snsEventPublisher;
	private AmazonSNSClient snsClient;
	private String emergencyTopicArn;
	private String nonEmergencyTopicArn;
	private boolean sendNotification;
	private boolean initialized = false;
	
	private Map<Integer, Boolean> emergencyState = Collections.synchronizedMap(new HashMap<Integer, Boolean>());

	private static Logger log = LoggerFactory.getLogger(EmergencyStatusNotificationServiceImpl.class);
	
	
	
	@Override
	public void process(CcLocationReportRecord record) {
		//Dont bother doing anything if sendNotification is turned off
		if(sendNotification && initialized) {
			Boolean emergencyStatusChange = Boolean.FALSE;
			Boolean isEmergency = Boolean.FALSE;
			Integer vehicleId = record.getVehicleId();
			String emergencyCode = record.getEmergencyCode();

			if(StringUtils.isNotBlank(emergencyCode) && StringUtils.equals(emergencyCode, "1")) {
				isEmergency = Boolean.TRUE;
			}

			if(emergencyState.containsKey(vehicleId)) {
				//Check if emergency status has changed 
				Boolean existingEmergency = emergencyState.get(vehicleId);
				if(!(existingEmergency.equals(isEmergency))) {
					emergencyStatusChange = Boolean.TRUE;
					//Add the record with current emergency code
					emergencyState.put(vehicleId, isEmergency);
				}
			} else {
				emergencyState.put(vehicleId, isEmergency);
				if(isEmergency) {
					emergencyStatusChange = Boolean.TRUE;
				}
			}

			//Publish sns application event if emergency state has changed
			if(emergencyStatusChange) {
				SNSApplicationEventData eventData = buildEventData(record);
				snsEventPublisher.setData(eventData);
				snsEventPublisher.run();
			}
		} else if (!initialized) {
		  // this will be caught on the next message after the context has initialized
		  _log.debug("dropping emergency status notification (" + record.getVehicleId() + ")=" + record.getEmergencyCode() + " as service is not ready");
		}
	}
	
	private SNSApplicationEventData buildEventData(CcLocationReportRecord record) {
		SNSApplicationEventData eventData = new SNSApplicationEventData();
		
		eventData.setRecord(record);
		eventData.setSnsClient(snsClient);
		eventData.setNonEmergencyTopicArn(nonEmergencyTopicArn);
		eventData.setEmergencyTopicArn(emergencyTopicArn);
		
		return eventData;
	}
	
	@Override
	public void setServletContext(ServletContext servletContext) {
		if(servletContext != null) {
			String sendSnsNotification = servletContext.getInitParameter("sns.sendNotification");
			log.info("Servler context provided sns.sendNotification: {}", sendSnsNotification);
			
			if(StringUtils.isNotBlank(sendSnsNotification)) {
				sendNotification = Boolean.parseBoolean(sendSnsNotification);
			} else {
				log.error("sns.sendNotification not set in servlet context");
			}
			
			String user = servletContext.getInitParameter("sns.user");
			log.info("Servlet context provided sns.user : {}", user);
			
			String password = servletContext.getInitParameter("sns.password");
			log.info("Servlet context provided sns.password : {}", password);
			
			if(StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
				snsClient = new AmazonSNSClient(new BasicAWSCredentials(user, password));
			} else {
				log.error("Cannot create Amazon SNS client. Either user name or" +
						"password is not set.");
			}
			
			String emergencyTopic = servletContext.getInitParameter("sns.emergencyTopicArn");
			log.info("Servlet context provided sns.emergencyTopicArn : {}", emergencyTopic);

			if(StringUtils.isNotBlank(emergencyTopic)) {
				emergencyTopicArn = emergencyTopic;
			} else {
				log.error("sns.emergencyTopicArn not set in the context.");
			}
			
			String nonEmergencyTopic = servletContext.getInitParameter("sns.nonEmergencyTopicArn");
			log.info("Servlet context provided sns.nonEmergencyTopicArn : {}", nonEmergencyTopic);

			if(StringUtils.isNotBlank(nonEmergencyTopic)) {
				nonEmergencyTopicArn = nonEmergencyTopic;
			} else {
				log.error("sns.nonEmergencyTopicArn not set in the context.");
			}
			
		}
	}

	/**
	 * @param snsEventPublisher the snsEventPublisher to set
	 */
	@Autowired
	public void setSnsEventPublisher(SNSApplicationEventPublisher snsEventPublisher) {
		this.snsEventPublisher = snsEventPublisher;
	}

	//Use only for unit test
	protected void setEmergencyState(Map<Integer, Boolean> emergencyState) {
		this.emergencyState = emergencyState;
	}

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    // don't attempt to send event messages until context is initialized; deadlock will ensue
    _log.warn("Emergency Status Initialized");
    this.initialized = true;
    
  }

}
