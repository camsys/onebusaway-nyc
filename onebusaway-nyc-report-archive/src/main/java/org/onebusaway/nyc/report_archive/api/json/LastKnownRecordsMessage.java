package org.onebusaway.nyc.report_archive.api.json;

import java.util.List;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;

public class LastKnownRecordsMessage {
  private List<ArchivedInferredLocationRecord> records;
  private String status;
  
  public void setRecords(List<ArchivedInferredLocationRecord> records) {
    this.records = records;
  }
  public void setStatus(String status) {
    this.status = status;
  }
}
