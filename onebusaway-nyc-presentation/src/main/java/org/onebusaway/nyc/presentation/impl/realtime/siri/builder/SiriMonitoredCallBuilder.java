/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.presentation.impl.realtime.siri.builder;

import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.StopPointRefStructure;

import java.math.BigInteger;
import java.util.Date;

public class SiriMonitoredCallBuilder {

    private BigInteger visitNumber = null;
    private StopPointRefStructure stopPointRef = null;
    private NaturalLanguageStringStructure stopPointName = null;
    private Date expectedArrivalTime = null;
    private Date expectedDepartureTime = null;
    private Date aimedArrivalTime = null;
    private ExtensionsStructure anyExtensions = null;

    public SiriMonitoredCallBuilder(){}

    public SiriMonitoredCallBuilder setVisitNumber(BigInteger visitNumber) {
        this.visitNumber = visitNumber;
        return this;
    }

    public SiriMonitoredCallBuilder setStopPointRef(StopPointRefStructure stopPointRef) {
        this.stopPointRef = stopPointRef;
        return this;
    }

    public SiriMonitoredCallBuilder setStopPointName(NaturalLanguageStringStructure stopPointName) {
        this.stopPointName = stopPointName;
        return this;
    }

    public SiriMonitoredCallBuilder setExpectedArrivalTime(Date expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
        return this;
    }

    public SiriMonitoredCallBuilder setExpectedDepartureTime(Date expectedDepartureTime) {
        this.expectedDepartureTime = expectedDepartureTime;
        return this;
    }

    public SiriMonitoredCallBuilder setAimedArrivalTime(Date aimedArrivalTime) {
        this.aimedArrivalTime = aimedArrivalTime;
        return this;
    }

    public SiriMonitoredCallBuilder setExtensions(ExtensionsStructure anyExtensions) {
        this.anyExtensions = anyExtensions;
        return this;
    }

    public MonitoredCallStructure build(){
        MonitoredCallStructure monitoredCall = new MonitoredCallStructure();
        monitoredCall.setVisitNumber(visitNumber);
        monitoredCall.setStopPointRef(stopPointRef);
        monitoredCall.setStopPointName(stopPointName);
        monitoredCall.setExpectedArrivalTime(expectedArrivalTime);
        monitoredCall.setExpectedDepartureTime(expectedDepartureTime);
        monitoredCall.setAimedArrivalTime(aimedArrivalTime);
        monitoredCall.setExtensions(anyExtensions);
        return monitoredCall;
    }
}
