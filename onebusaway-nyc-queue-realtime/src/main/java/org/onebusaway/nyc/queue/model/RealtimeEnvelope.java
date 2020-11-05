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

package org.onebusaway.nyc.queue.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import tcip_final_3_0_5_1.CcLocationReport;

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