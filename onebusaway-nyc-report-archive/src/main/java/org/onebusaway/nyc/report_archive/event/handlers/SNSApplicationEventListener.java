package org.onebusaway.nyc.report_archive.event.handlers;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.event.SNSApplicationEvent;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Listens to {@link SNSApplicationEvent} and sends notification to amazon sns service.
 * @author abelsare
 *
 */
public class SNSApplicationEventListener implements ApplicationListener<SNSApplicationEvent> {

	private static Logger log = LoggerFactory.getLogger(SNSApplicationEventListener.class);
	
	private JsonTool jsonTool;
	
	@Override
	public void onApplicationEvent(SNSApplicationEvent event) {
		AmazonSNSClient snsClient = event.getData().getSnsClient();
		CcLocationReportRecord record = event.getData().getRecord();
		String emergencyTopicArn = event.getData().getEmergencyTopicArn();
		String nonEmergencyTopicArn = event.getData().getNonEmergencyTopicArn();
		
		Integer vehicleId = record.getVehicleId();
		String topicArn;
		String logMessage;

		//If we are here, emergency code value is sufficient to figure out the transition
		if(StringUtils.isBlank(record.getEmergencyCode())) {
			topicArn = nonEmergencyTopicArn;
			logMessage = "Publishing non emergency status transition notificaton for bus : {}";
		} else {
			topicArn = emergencyTopicArn;
			logMessage = "Publishing emergency status transition notificaton for bus : {}";
		}

		String message;
		try {
			message = buildMessage(record);
		} catch (IOException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		PublishRequest publishRequest = new PublishRequest(topicArn, message, "Emergency Status Change Notification - Bus Number: " +vehicleId);

		log.info(logMessage, vehicleId);

		snsClient.publish(publishRequest);
		
	}
	
	private String buildMessage(CcLocationReportRecord record) throws IOException{
		log.info("Serializing location record as json.");

		String outputJson = null;

		StringWriter writer = null;

		try {
			writer = new StringWriter();
			jsonTool.writeJson(writer, record);
			outputJson = writer.toString();
		} catch (IOException e) {
			throw new IOException("IOException while using jsonTool to write object as json.", e);
		} finally {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) { }
		}

		if (outputJson == null) throw new IOException("After using jsontool to write json, output was still null.");

		return outputJson;
	}

	/**
	 * Injects JsonTool
	 * @param jsonTool the jsonTool to set
	 */
	@Autowired
	public void setJsonTool(JsonTool jsonTool) {
		this.jsonTool = jsonTool;
	}

}
