package org.onebusaway.nyc.report_archive.api.json;

import org.onebusaway.nyc.report_archive.model.NycCancelledTripRecord;

import java.util.List;

public class HistoricalCancelledTripRecordsMessage {

    private List<NycCancelledTripRecord> records;
    private String status;

    /**
     * @param records the records to set
     */
    public void setRecords(List<NycCancelledTripRecord> records) {
        this.records = records;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

}

