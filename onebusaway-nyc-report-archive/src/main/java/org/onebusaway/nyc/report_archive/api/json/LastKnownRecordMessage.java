package org.onebusaway.nyc.report_archive.api.json;

import java.util.List;

import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;

/**
 * Holds last known records
 * @author abelsare
 *
 */
public class LastKnownRecordMessage {

	private List<CcAndInferredLocationRecord> records;
	private String status;

	public void setRecords(List<CcAndInferredLocationRecord> records) {
		this.records = records;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
