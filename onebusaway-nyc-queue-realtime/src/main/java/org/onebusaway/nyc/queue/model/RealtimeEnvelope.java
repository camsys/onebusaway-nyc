package org.onebusaway.nyc.queue.model;

import java.io.Serializable;
import tcip_final_3_0_5_1.CcLocationReport;

/**
 * JSON wrapper for realtime bus data.
 */
public class RealtimeEnvelope implements Serializable {

    private String UUID;
    private long timeReceived;
    private CcLocationReport ccLocationReport;

    public String getUUID() {
	return UUID;
    }
    
    public void setUUID(String uuid) {
	this.UUID = uuid;
    }

    public long getTimeReceived() {
	return timeReceived;
    }

    public void setTimeReceived(long timeReceived) {
	this.timeReceived = timeReceived;
    }

    public CcLocationReport getCcLocationReport() {
	return ccLocationReport;
    }

    public void setCcLocationReport(CcLocationReport ccLocationReport) {
	this.ccLocationReport = ccLocationReport;
    }
}