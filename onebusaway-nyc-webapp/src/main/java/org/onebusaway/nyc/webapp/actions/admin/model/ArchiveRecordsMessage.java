package org.onebusaway.nyc.webapp.actions.admin.model;

import java.util.List;

public class ArchiveRecordsMessage {
	private List<ArchiveRecord> records;
	private String status;
	
	public List<ArchiveRecord> getRecords() {
		return records;
	}
	public void setRecords(List<ArchiveRecord> records) {
		this.records = records;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
}
