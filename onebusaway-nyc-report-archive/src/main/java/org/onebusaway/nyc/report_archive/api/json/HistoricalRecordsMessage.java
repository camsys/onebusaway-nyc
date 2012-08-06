package org.onebusaway.nyc.report_archive.api.json;

import java.util.List;

import org.onebusaway.nyc.report_archive.result.HistoricalRecord;

/**
 * Holds historical records
 * @author abelsare
 *
 */
public class HistoricalRecordsMessage {

	private List<HistoricalRecord> records;
	private String status;

	/**
	 * @param records the records to set
	 */
	public void setRecords(List<HistoricalRecord> records) {
		this.records = records;
	}
	
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	
}
