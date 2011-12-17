package org.onebusaway.nyc.report_archive.api.json;

import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;

public class LastKnownRecordMessage {

	private CcAndInferredLocationRecord record;
	private String status;

	public void setRecord(CcAndInferredLocationRecord record) {
		this.record = record;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
