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

import java.util.Date;
import java.util.List;
import java.util.Set;


public final class SiriMonitoredVehicleJourneyBuilder {

    private LineRefStructure lineRef = null;
    private OperatorRefStructure operatorRef = null;
    private DirectionRefStructure directionRef = null;
    private NaturalLanguageStringStructure publishedLineName = null;
    private JourneyPatternRefStructure shapeId = null;
    private NaturalLanguageStringStructure headsign = null;
    private VehicleRefStructure vehicleRef = null;
    private JourneyPlaceRefStructure origin = null;
    private DestinationRefStructure destination = null;
    private OccupancyEnumeration occupancy = null;
    private FramedVehicleJourneyRefStructure framedVehicleJourneyRef = null;
    private LocationStructure location = null;
    private BlockRefStructure blockRef = null;
    private NaturalLanguageStringStructure progressStatus = null;
    private ProgressRateEnumeration progressRate = null;
    private Boolean monitored = null;
    private Float bearing = null;
    private Date originAimedDepartureTime = null;
    private MonitoredCallStructure monitoredCall = null;
    private OnwardCallsStructure onwardCalls = null;
    List<SituationRefStructure> situationRef = null;
    private ExtensionsStructure anyExtensions = null;


    public SiriMonitoredVehicleJourneyBuilder(LineRefStructure lineRef,
                                              OperatorRefStructure operatorRef,
                                              DirectionRefStructure directionRef,
                                              NaturalLanguageStringStructure publishedLineName,
                                              JourneyPatternRefStructure shapeId,
                                              NaturalLanguageStringStructure headsign,
                                              VehicleRefStructure vehicleRef,
                                              JourneyPlaceRefStructure origin,
                                              DestinationRefStructure destination,
                                              OccupancyEnumeration occupancy,
                                              FramedVehicleJourneyRefStructure framedVehicleJourneyRef,
                                              LocationStructure location,
                                              BlockRefStructure blockRef,
                                              NaturalLanguageStringStructure progressStatus,
                                              ProgressRateEnumeration progressRate,
                                              Boolean monitored,
                                              Float bearing,
                                              Date originAimedDepartureTime,
                                              MonitoredCallStructure monitoredCall,
                                              OnwardCallsStructure onwardCalls,
                                              List<SituationRefStructure> situationRef){
        this.lineRef = lineRef;
        this.operatorRef = operatorRef;
        this.directionRef = directionRef;
        this.publishedLineName = publishedLineName;
        this.shapeId = shapeId;
        this.headsign = headsign;
        this.vehicleRef = vehicleRef;
        this.origin = origin;
        this.destination = destination;
        this.occupancy = occupancy;
        this.framedVehicleJourneyRef = framedVehicleJourneyRef;
        this.location = location;
        this.blockRef = blockRef;
        this.progressStatus = progressStatus;
        this.progressRate = progressRate;
        this.monitored = monitored;
        this.bearing = bearing;
        this.originAimedDepartureTime = originAimedDepartureTime;
        this.monitoredCall = monitoredCall;
        this.onwardCalls = onwardCalls;
        this.situationRef = situationRef;

    }

    public SiriMonitoredVehicleJourneyBuilder(){}

    public SiriMonitoredVehicleJourneyBuilder setLineRef(LineRefStructure lineRef) {
        this.lineRef = lineRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setOperatorRef(OperatorRefStructure operatorRef) {
        this.operatorRef = operatorRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setDirectionRef(DirectionRefStructure directionRef) {
        this.directionRef = directionRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setPublishedLineName(NaturalLanguageStringStructure publishedLineName) {
        this.publishedLineName = publishedLineName;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setJourneyPatternRef(JourneyPatternRefStructure shapeId) {
        this.shapeId = shapeId;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setDestinationName(NaturalLanguageStringStructure headsign) {
        this.headsign = headsign;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setVehicleRef(VehicleRefStructure vehicleRef) {
        this.vehicleRef = vehicleRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setOriginRef(JourneyPlaceRefStructure origin) {
        this.origin = origin;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setDestinationRef(DestinationRefStructure destination) {
        this.destination = destination;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setOccupancy(OccupancyEnumeration occupancy) {
        this.occupancy = occupancy;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setFramedVehicleJourneyRef(FramedVehicleJourneyRefStructure framedVehicleJourneyRef) {
        this.framedVehicleJourneyRef = framedVehicleJourneyRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setVehicleLocation(LocationStructure location) {
        this.location = location;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setBlockRef(BlockRefStructure blockRef) {
        this.blockRef = blockRef;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setProgressStatus(NaturalLanguageStringStructure progressStatus) {
        this.progressStatus = progressStatus;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setMonitored(Boolean monitored){
        this.monitored = monitored;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setProgressRate(ProgressRateEnumeration progressRate) {
        this.progressRate = progressRate;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setBearing(Float bearing) {
        this.bearing = bearing;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setOriginAimedDepartureTime(Date originAimedDepartureTime) {
        this.originAimedDepartureTime = originAimedDepartureTime;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setMonitoredCall(MonitoredCallStructure monitoredCall) {
        this.monitoredCall = monitoredCall;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setOnwardCalls(OnwardCallsStructure onwardCalls) {
        this.onwardCalls = onwardCalls;
        return this;
    }

    public SiriMonitoredVehicleJourneyBuilder setSituationRef(List<SituationRefStructure> situationRef) {
        this.situationRef = situationRef;
        return this;
    }

    public VehicleActivityStructure.MonitoredVehicleJourney buildMonitoredVehicleJourney(){
        VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = new VehicleActivityStructure.MonitoredVehicleJourney();
        monitoredVehicleJourney.setLineRef(lineRef);
        monitoredVehicleJourney.setOperatorRef(operatorRef);
        monitoredVehicleJourney.setDirectionRef(directionRef);
        monitoredVehicleJourney.setPublishedLineName(publishedLineName);
        monitoredVehicleJourney.setJourneyPatternRef(shapeId);
        monitoredVehicleJourney.setDestinationName(headsign);
        monitoredVehicleJourney.setVehicleRef(vehicleRef);
        monitoredVehicleJourney.setMonitored(monitored);
        monitoredVehicleJourney.setBearing(bearing);
        monitoredVehicleJourney.setOccupancy(occupancy);
        monitoredVehicleJourney.setFramedVehicleJourneyRef(framedVehicleJourneyRef);
        monitoredVehicleJourney.setVehicleLocation(location);
        monitoredVehicleJourney.setProgressStatus(progressStatus);
        monitoredVehicleJourney.setBlockRef(blockRef);
        monitoredVehicleJourney.setOriginAimedDepartureTime(originAimedDepartureTime);
        monitoredVehicleJourney.setOriginRef(origin);
        monitoredVehicleJourney.setDestinationRef(destination);
        monitoredVehicleJourney.setProgressRate(progressRate);
        monitoredVehicleJourney.setMonitoredCall(monitoredCall);
        monitoredVehicleJourney.setOnwardCalls(onwardCalls);
        if(situationRef != null){
            monitoredVehicleJourney.getSituationRef().addAll(situationRef);
        }
//        monitoredVehicleJourney.setExtensions(anyExtensions);
        return monitoredVehicleJourney;
    }

    public MonitoredVehicleJourneyStructure buildMonitoredVehicleJourneyStructure(){
        MonitoredVehicleJourneyStructure monitoredVehicleJourney = new MonitoredVehicleJourneyStructure();
        monitoredVehicleJourney.setLineRef(lineRef);
        monitoredVehicleJourney.setOperatorRef(operatorRef);
        monitoredVehicleJourney.setDirectionRef(directionRef);
        monitoredVehicleJourney.setPublishedLineName(publishedLineName);
        monitoredVehicleJourney.setJourneyPatternRef(shapeId);
        monitoredVehicleJourney.setDestinationName(headsign);
        monitoredVehicleJourney.setVehicleRef(vehicleRef);
        monitoredVehicleJourney.setMonitored(monitored);
        monitoredVehicleJourney.setBearing(bearing);
        monitoredVehicleJourney.setOccupancy(occupancy);
        monitoredVehicleJourney.setFramedVehicleJourneyRef(framedVehicleJourneyRef);
        monitoredVehicleJourney.setVehicleLocation(location);
        monitoredVehicleJourney.setProgressStatus(progressStatus);
        monitoredVehicleJourney.setBlockRef(blockRef);
        monitoredVehicleJourney.setOriginAimedDepartureTime(originAimedDepartureTime);
        monitoredVehicleJourney.setOriginRef(origin);
        monitoredVehicleJourney.setDestinationRef(destination);
        monitoredVehicleJourney.setProgressRate(progressRate);
        monitoredVehicleJourney.setMonitoredCall(monitoredCall);
        monitoredVehicleJourney.setOnwardCalls(onwardCalls);
        if(situationRef != null){
            monitoredVehicleJourney.getSituationRef().addAll(situationRef);
        }
        monitoredCall.setExtensions(anyExtensions);
        return monitoredVehicleJourney;
    }
}
