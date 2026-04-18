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
package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * SIRI extension on MonitoredCall and OnwardCall indicating the stop's detour
 * status relative to a GTFS-RT trip modification.
 *
 * isDetour = true  → stop is a detour stop (ADDED by the modification)
 * isDetour = false → stop is cancelled (REMOVED by the modification);
 *                    the call will have AimedArrivalTime but no ExpectedArrivalTime
 */
@XmlRootElement
public class SiriStopDetourStatusExtension {

    private boolean isDetour;

    @XmlElement(name = "IsDetour")
    public boolean isDetour() {
        return isDetour;
    }

    public void setDetour(boolean isDetour) {
        this.isDetour = isDetour;
    }
}
