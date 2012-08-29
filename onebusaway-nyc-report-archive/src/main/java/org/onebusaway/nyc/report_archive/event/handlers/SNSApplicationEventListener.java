package org.onebusaway.nyc.report_archive.event.handlers;

import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.report_archive.event.SNSApplicationEvent;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.ServletContextAware;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Listens to {@link SNSApplicationEvent} and sends notification to amazon sns service.
 * @author abelsare
 *
 */
public class SNSApplicationEventListener implements ApplicationListener<SNSApplicationEvent>, ServletContextAware {

	private AmazonSNSClient snsClient;
	private String topicArn;
	
	private static Logger log = LoggerFactory.getLogger(SNSApplicationEventListener.class);
	
	@Override
	public void onApplicationEvent(SNSApplicationEvent event) {
		String message = buildMessage(event.getData());
		Integer vehicleId = event.getData().getVehicleId();
		
		PublishRequest publishRequest = new PublishRequest(topicArn, message, "Emergency Status Notification - Bus Number: " +vehicleId);
		
		log.info("Publishing emergency notificaton for bus : {}", vehicleId);
		
		snsClient.publish(publishRequest);
	}
	
	private String buildMessage(CcLocationReportRecord record) {
		final String SPACE = " ";
		final String NEW_LINE = System.getProperty("line.separator") ;
		
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append("Emergency Status Change for bus number :").append(SPACE);
		messageBuilder.append(record.getVehicleId()).append(NEW_LINE).append(NEW_LINE);;
		messageBuilder.append("Details: ").append(NEW_LINE).append(NEW_LINE);
		messageBuilder.append("Emergency code:").append(SPACE);
		messageBuilder.append(record.getEmergencyCode()).append(NEW_LINE);
		messageBuilder.append("Latitude:").append(SPACE);
		messageBuilder.append(record.getLatitude()).append(NEW_LINE);
		messageBuilder.append("Longitude:").append(SPACE);
		messageBuilder.append(record.getLongitude()).append(NEW_LINE);
		
		return messageBuilder.toString();
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if(servletContext != null) {
			String user = servletContext.getInitParameter("sns.user");
			log.info("Servlet context provided sns.user : {}", user);
			
			String password = servletContext.getInitParameter("sns.password");
			log.info("Servlet context provided sns.password : {}", password);
			
			if(StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
				snsClient = new AmazonSNSClient(new BasicAWSCredentials(user, password));
			} else {
				throw new RuntimeException("Cannot create Amazon SNS client. Either user name or" +
						"password is not set.");
			}
			
			String topic = servletContext.getInitParameter("sns.topicArn");
			log.info("Servlet context provided sns.topicArn : {}", topic);

			if(StringUtils.isNotBlank(topic)) {
				topicArn = topic;
			} else {
				log.error("SNS Topic arn is not set in the servlet context.");
			}
		}
	}
	
	
}
