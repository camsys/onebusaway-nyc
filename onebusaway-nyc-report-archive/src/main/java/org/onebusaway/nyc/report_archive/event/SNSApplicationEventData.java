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
