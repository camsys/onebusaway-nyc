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

package org.onebusaway.nyc.presentation.impl.realtime.siri;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportPredictionTimepointRecord;
import org.onebusaway.nyc.presentation.impl.realtime.siri.builder.SiriMonitoredCallBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriBuilderServiceHelper;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriMonitoredCallBuilderService;
import org.onebusaway.nyc.siri.support.SiriApcExtension;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.siri.siri.*;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

@Component
public class SiriMonitoredCallBuilderServiceImpl implements SiriMonitoredCallBuilderService {

    private ConfigurationService _configurationService;

    private NycTransitDataService _nycTransitDataService;

    private PresentationService _presentationService;

    private SiriBuilderServiceHelper _siriBuilderServiceHelper;

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @Autowired
    public void setNycTransitDataService(NycTransitDataService nycTransitDataService) {
        _nycTransitDataService = nycTransitDataService;
    }

    @Autowired
    public void setPresentationService(PresentationService presentationService) {
        _presentationService = presentationService;
    }

    @Autowired
    public void setSiriBuilderServiceHelper(SiriBuilderServiceHelper siriBuilderServiceHelper) {
        _siriBuilderServiceHelper = siriBuilderServiceHelper;
    }

    @Override
    public MonitoredCallStructure makeMonitoredCall(BlockInstanceBean blockInstance,
                                                    TripBean tripOnBlock,
                                                    TripStatusBean currentlyActiveTripOnBlock,
                                                    StopBean monitoredCallStopBean,
                                                    Map<String, SiriSupportPredictionTimepointRecord> stopLevelPredictions,
                                                    boolean showRawApc,
                                                    long responseTimestamp
                                                    ){
        SiriMonitoredCallBuilder siriMonitoredCallBuilder = new SiriMonitoredCallBuilder();

        List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

        Double distanceOfVehicleAlongBlock = null;
        int blockTripStopsAfterTheVehicle = 0;

        // Loop through all trips on block to find the stoptime info for trip in question
        // to populate monitored call
        for(int i = 0; i < blockTrips.size(); i++) {

            BlockTripBean blockTrip = blockTrips.get(i);

            // First get DistanceAlongBlock for currently active trip on block
            if(distanceOfVehicleAlongBlock == null){
                distanceOfVehicleAlongBlock = _siriBuilderServiceHelper.getDistanceOfVehicleAlongBlock(currentlyActiveTripOnBlock, blockTrip);
                if(distanceOfVehicleAlongBlock == null) {
                    continue;
                }
            }

            HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();
            boolean foundArrivalDepartureTripOnBlock = false;

            // Loop through all stop times until we reach the one for monitored call
            for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {
                int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStopTime().getStop());

                // block trip stops away--on this trip, only after we've passed the stop,
                // on future trips, count always.
                if(currentlyActiveTripOnBlock.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
                    if(stopTime.getDistanceAlongBlock() >= distanceOfVehicleAlongBlock) {
                        blockTripStopsAfterTheVehicle++;
                    } else {
                        // bus has passed this stop already--no need to go further
                        continue;
                    }

                    // future trip--bus hasn't reached this trip yet, so count all stops
                } else {
                    blockTripStopsAfterTheVehicle++;
                }

                // Check to see if blockTrip matches trip that we are searching
                // Only continue processing monitored call if blockTrip matches trip we are searching
                if(!foundArrivalDepartureTripOnBlock){
                    foundArrivalDepartureTripOnBlock = tripOnBlock.getId().equals(blockTrip.getTrip().getId());
                    if(!foundArrivalDepartureTripOnBlock){
                        break;
                    }
                    // Check to see if trip is in the future
                    else if(!currentlyActiveTripOnBlock.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
                        // Hide APC if trip is in the future
                        showRawApc = false;
                    }
                }

                // monitored call
                if(stopTime.getStopTime().getStop().getId().equals(monitoredCallStopBean.getId())) {
                    if(!_presentationService.isOnDetour(currentlyActiveTripOnBlock)) {

                        String stopPredictionKey = SiriSupportPredictionTimepointRecord
                                .convertTripAndStopToKey(blockTrip.getTrip().getId(), stopTime.getStopTime().getStop().getId());

                        return getMonitoredCallStructure(
                                stopTime.getStopTime().getStop(),
                                stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock(),
                                stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock,
                                visitNumber,
                                blockTripStopsAfterTheVehicle - 1,
                                stopLevelPredictions.get(stopPredictionKey),
                                currentlyActiveTripOnBlock.getVehicleId(),
                                showRawApc,
                                stopTime.getStopTime().getArrivalTime(),
                                responseTimestamp
                        );

                    }
                }
            }
        }

        return null;
    }

    private int getVisitNumber(HashMap<String, Integer> visitNumberForStop, StopBean stop) {
        int visitNumber;

        if (visitNumberForStop.containsKey(stop.getId())) {
            visitNumber = visitNumberForStop.get(stop.getId()) + 1;
        } else {
            visitNumber = 1;
        }

        visitNumberForStop.put(stop.getId(), visitNumber);

        return visitNumber;
    }

    private MonitoredCallStructure getMonitoredCallStructure(StopBean stopBean,
                                                             double distanceOfCallAlongTrip,
                                                             double distanceOfVehicleFromCall,
                                                             int visitNumber,
                                                             int index,
                                                             SiriSupportPredictionTimepointRecord prediction,
                                                             String vehicleId,
                                                             boolean showRawApc,
                                                             int arrivalTime,
                                                             long responseTimestamp
                                                             ) {

        SiriMonitoredCallBuilder siriMonitoredCallBuilder = new SiriMonitoredCallBuilder();
        siriMonitoredCallBuilder.setVisitNumber(BigInteger.valueOf(visitNumber));

        StopPointRefStructure stopPointRef = new StopPointRefStructure();
        stopPointRef.setValue(stopBean.getId());
        siriMonitoredCallBuilder.setStopPointRef(stopPointRef);

        NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
        stopPoint.setValue(stopBean.getName());
        siriMonitoredCallBuilder.setStopPointName(stopPoint);

        if(prediction != null) {
            fillExpectedArrivalDepartureTimes(siriMonitoredCallBuilder, prediction, responseTimestamp);
        }

        Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        calendar.add(Calendar.SECOND, arrivalTime);
        siriMonitoredCallBuilder.setAimedArrivalTime(calendar.getTime());

        // siri extensions
        SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
        ExtensionsStructure anyExtensions = new ExtensionsStructure();
        SiriDistanceExtension distances = new SiriDistanceExtension();

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setGroupingUsed(false);

        distances.setStopsFromCall(index);
        distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
        distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
        distances.setPresentableDistance(_presentationService.getPresentableDistance(distances));

        wrapper.setDistances(distances);

        if (vehicleId != null) {
            VehicleOccupancyRecord vor =
                    _nycTransitDataService.getLastVehicleOccupancyRecordForVehicleId(AgencyAndId.convertFromString(vehicleId));
            if (showRawApc) {
                SiriApcExtension apcExtension = _presentationService.getPresentableApc(vor);
                if (apcExtension != null) {
                    wrapper.setCapacities(apcExtension);
                }
            }
        }

        anyExtensions.setAny(wrapper);
        siriMonitoredCallBuilder.setExtensions(anyExtensions);

        return siriMonitoredCallBuilder.build();
    }

    private void fillExpectedArrivalDepartureTimes(SiriMonitoredCallBuilder siriMonitoredCallBuilder,
                                                   SiriSupportPredictionTimepointRecord prediction,
                                                   long responseTimestamp){

        long arrivalTime = prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime();
        long departureTime = prediction.getTimepointPredictionRecord().getTimepointPredictedDepartureTime();

        // Both arrival and departure time are in past
        if (arrivalTime < responseTimestamp && departureTime < responseTimestamp) {
            siriMonitoredCallBuilder.setExpectedArrivalTime(new Date(responseTimestamp + 1000));
            siriMonitoredCallBuilder.setExpectedDepartureTime(new Date(responseTimestamp + 1000));
        }
        // arrival time undefined and departure time in the future
        else if(arrivalTime < 0 && departureTime > responseTimestamp ) {
            siriMonitoredCallBuilder.setExpectedArrivalTime(new Date(departureTime));
            siriMonitoredCallBuilder.setExpectedDepartureTime(new Date(departureTime));
        }
        // arrival time
        else if(arrivalTime < responseTimestamp){
            siriMonitoredCallBuilder.setExpectedArrivalTime(new Date(responseTimestamp + 1000));
            siriMonitoredCallBuilder.setExpectedDepartureTime(new Date(departureTime));
        }
        else if(departureTime < responseTimestamp){
            siriMonitoredCallBuilder.setExpectedArrivalTime(new Date(arrivalTime));
            siriMonitoredCallBuilder.setExpectedDepartureTime(new Date(arrivalTime));
        }
        else {
            siriMonitoredCallBuilder.setExpectedArrivalTime(new Date(arrivalTime));
            siriMonitoredCallBuilder.setExpectedDepartureTime(new Date(departureTime));
        }
    }
}
