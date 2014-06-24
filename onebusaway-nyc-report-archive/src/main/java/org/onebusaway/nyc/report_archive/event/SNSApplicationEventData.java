package org.onebusaway.nyc.report_archive.event;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;

import com.amazonaws.services.sns.AmazonSNSClient;

/**
 * Holds {@link SNSApplicationEvent} data required for event listeners
 * @author abelsare
 *
 */
public class SNSApplicationEventData {

	private CcLocationReportRecord record;
	private AmazonSNSClient snsClient;
	private String emergencyTopicArn;
	private String nonEmergencyTopicArn;

	/**
	 * @return the record
	 */
	public CcLocationReportRecord getRecord() {
		return record;
	}
	/**
	 * @param record the record to set
	 */
	public void setRecord(CcLocationReportRecord record) {
		this.record = record;
	}
	/**
	 * @return the snsClient
	 */
	public AmazonSNSClient getSnsClient() {
		return snsClient;
	}
	/**
	 * @param snsClient the snsClient to set
	 */
	public void setSnsClient(AmazonSNSClient snsClient) {
		this.snsClient = snsClient;
	}
	/**
	 * @return the emergencyTopicArn
	 */
	public String getEmergencyTopicArn() {
		return emergencyTopicArn;
	}
	/**
	 * @param emergencyTopicArn the emergencyTopicArn to set
	 */
	public void setEmergencyTopicArn(String emergencyTopicArn) {
		this.emergencyTopicArn = emergencyTopicArn;
	}
	/**
	 * @return the nonEmergencyTopicArn
	 */
	public String getNonEmergencyTopicArn() {
		return nonEmergencyTopicArn;
	}
	/**
	 * @param nonEmergencyTopicArn the nonEmergencyTopicArn to set
	 */
	public void setNonEmergencyTopicArn(String nonEmergencyTopicArn) {
		this.nonEmergencyTopicArn = nonEmergencyTopicArn;
	}
	
}
