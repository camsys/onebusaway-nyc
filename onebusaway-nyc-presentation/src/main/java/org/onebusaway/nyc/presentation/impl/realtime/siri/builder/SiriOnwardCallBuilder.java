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
import uk.org.siri.siri.*;

import java.math.BigInteger;
import java.util.Date;

public class SiriOnwardCallBuilder {

    private BigInteger visitNumber = null;
    private StopPointRefStructure stopPointRef = null;
    private NaturalLanguageStringStructure stopPointName = null;
    private Date expectedArrivalTime = null;
    private Date expectedDepartureTime = null;
    private Date aimedArrivalTime = null;
    private ExtensionsStructure anyExtensions = null;

    public SiriOnwardCallBuilder(){}

    public SiriOnwardCallBuilder setVisitNumber(BigInteger visitNumber) {
        this.visitNumber = visitNumber;
        return this;
    }

    public SiriOnwardCallBuilder setStopPointRef(StopPointRefStructure stopPointRef) {
        this.stopPointRef = stopPointRef;
        return this;
    }

    public SiriOnwardCallBuilder setStopPointName(NaturalLanguageStringStructure stopPointName) {
        this.stopPointName = stopPointName;
        return this;
    }

    public SiriOnwardCallBuilder setExpectedArrivalTime(Date expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
        return this;
    }

    public SiriOnwardCallBuilder setExpectedDepartureTime(Date expectedDepartureTime) {
        this.expectedDepartureTime = expectedDepartureTime;
        return this;
    }

    public SiriOnwardCallBuilder setAimedArrivalTime(Date aimedArrivalTime) {
        this.aimedArrivalTime = aimedArrivalTime;
        return this;
    }

    public SiriOnwardCallBuilder setExtensions(ExtensionsStructure anyExtensions) {
        this.anyExtensions = anyExtensions;
        return this;
    }

    public OnwardCallStructure build(){
        OnwardCallStructure onwardCallStructure = new OnwardCallStructure();
        onwardCallStructure.setVisitNumber(visitNumber);
        onwardCallStructure.setStopPointRef(stopPointRef);
        onwardCallStructure.setStopPointName(stopPointName);
        onwardCallStructure.setExpectedArrivalTime(expectedArrivalTime);
        onwardCallStructure.setExpectedDepartureTime(expectedDepartureTime);
        onwardCallStructure.setAimedArrivalTime(aimedArrivalTime);
        onwardCallStructure.setExtensions(anyExtensions);

        return onwardCallStructure;
    }
}
