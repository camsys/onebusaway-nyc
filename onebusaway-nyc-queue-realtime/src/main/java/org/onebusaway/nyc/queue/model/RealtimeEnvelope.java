package org.onebusaway.nyc.queue.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import tcip_final_4_0_0.CcLocationReport;

/**
 * JSON wrapper for realtime bus data.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RealtimeEnvelop", propOrder = {
    "UUID",
    "timeReceived",
    "ccLocationReport"
		})
public class RealtimeEnvelope implements Serializable {

    @XmlElement(name = "UUID")
    private String UUID;
    @XmlElement(name = "timeReceived")
    private long timeReceived;
    @XmlElement(name = "CcLocationReport")
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