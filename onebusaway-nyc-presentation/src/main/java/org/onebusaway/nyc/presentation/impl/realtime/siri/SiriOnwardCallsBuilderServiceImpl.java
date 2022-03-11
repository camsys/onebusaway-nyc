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

import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportPredictionTimepointRecord;
import org.onebusaway.nyc.presentation.impl.realtime.siri.builder.SiriOnwardCallBuilder;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriBuilderServiceHelper;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriOnwardCallsBuilderService;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.siri.siri.*;

import java.text.DecimalFormat;
import java.util.*;

@Component
public class SiriOnwardCallsBuilderServiceImpl implements SiriOnwardCallsBuilderService {

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
    public OnwardCallsStructure makeOnwardCalls(BlockInstanceBean blockInstance,
                                                TripBean tripOnBlock,
                                                TripStatusBean currentlyActiveTripOnBlock,
                                                OnwardCallsMode onwardCallsMode,
                                                Map<String, SiriSupportPredictionTimepointRecord> stopLevelPredictions,
                                                int maximumOnwardCalls,
                                                long responseTimestamp){

        OnwardCallsStructure onwardCallsStructures = new OnwardCallsStructure();

        // Only process if maxOnwardCalls is greater than 0
        if(maximumOnwardCalls > 0) {

            String tripIdOfMonitoredCall = tripOnBlock.getId();

            List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

            Double distanceOfVehicleAlongBlock = null;
            int blockTripStopsAfterTheVehicle = 0;
            int onwardCallsAdded = 0;
            boolean foundArrivalDepartureTripOnBlock = false;

            for(int i = 0; i < blockTrips.size(); i++) {

                BlockTripBean blockTrip = blockTrips.get(i);

                // First get DistanceAlongBlock for currently active trip on block
                if(distanceOfVehicleAlongBlock == null){
                    distanceOfVehicleAlongBlock = _siriBuilderServiceHelper.getDistanceOfVehicleAlongBlock(
                            currentlyActiveTripOnBlock, blockTrip, false);
                    if(distanceOfVehicleAlongBlock == null) {
                        continue;
                    }
                }

                // Continue loop after finding active trip
                // Check to see if active trip matches trip that we are searching
                // If same then we are querying current trip otherwise we are querying future trip
               /* if(!foundArrivalDepartureTripOnBlock){
                    foundArrivalDepartureTripOnBlock = tripOnBlock.getId().equals(blockTrip.getTrip().getId());
                    if(!foundArrivalDepartureTripOnBlock){
                        continue;
                    }
                }*/

                if(onwardCallsMode == OnwardCallsMode.STOP_MONITORING) {
                    // always include onward calls for the trip the monitored call is on ONLY.
                    if(!blockTrip.getTrip().getId().equals(tripIdOfMonitoredCall)) {
                        continue;
                    }
                }

                // Loop through all stop times until we reach the first one that matches the monitored call
                // Add stop to onwards call
                // Continue to add the next n number of stops (where n is maximumOnwardCalls)
                for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {

                    // check for non-revenue stops for onward calls
                    if(_siriBuilderServiceHelper.isNonRevenueStop(currentlyActiveTripOnBlock, stopTime)){
                        continue;
                    }

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
                    }

                    String stopPredictionKey = SiriSupportPredictionTimepointRecord.convertTripAndStopToKey(
                            blockTrip.getTrip().getId(), stopTime.getStopTime().getStop().getId());

                    OnwardCallStructure onwardCallStructure = getOnwardCallStructure(blockTrip, stopTime,
                            distanceOfVehicleAlongBlock, blockTripStopsAfterTheVehicle,
                            stopLevelPredictions.get(stopPredictionKey),
                            responseTimestamp);

                    onwardCallsStructures.getOnwardCall().add(onwardCallStructure);

                    onwardCallsAdded++;

                    if(onwardCallsAdded >= maximumOnwardCalls) {
                        return onwardCallsStructures;
                    }
                }

                // if we get here, we added our stops
                return onwardCallsStructures;
            }
        }

        return onwardCallsStructures;

    }

    private OnwardCallStructure getOnwardCallStructure(BlockTripBean blockTrip,
                                                       BlockStopTimeBean stopTime,
                                                       double distanceOfVehicleAlongBlock,
                                                       int blockTripStopsAfterTheVehicle,
                                                       SiriSupportPredictionTimepointRecord prediction,
                                                       long responseTimestamp) {

        StopBean stopTimeStop = stopTime.getStopTime().getStop();
        double distanceOfCallAlongTrip = stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock();
        double distanceOfVehicleFromCall = stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock;
        int index = blockTripStopsAfterTheVehicle - 1;
        int arrivalTime = stopTime.getStopTime().getArrivalTime();


        SiriOnwardCallBuilder siriOnwardCallBuilder = new SiriOnwardCallBuilder();

        StopPointRefStructure stopPointRef = new StopPointRefStructure();
        stopPointRef.setValue(stopTimeStop.getId());
        siriOnwardCallBuilder.setStopPointRef(stopPointRef);

        NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
        stopPoint.setValue(stopTimeStop.getName());
        siriOnwardCallBuilder.setStopPointName(stopPoint);

        boolean isNearFirstStop = false;
        if (distanceOfCallAlongTrip < 100) isNearFirstStop = true;

        if(prediction != null) {
            if (prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime() < responseTimestamp) {
                if (!isNearFirstStop) { siriOnwardCallBuilder.setExpectedArrivalTime(new Date(responseTimestamp));}
                else {
                    siriOnwardCallBuilder.setExpectedDepartureTime(new Date(responseTimestamp));
                }
            } else {
                if (!isNearFirstStop) {	siriOnwardCallBuilder.setExpectedArrivalTime(new Date(prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime()));}
                else {
                    siriOnwardCallBuilder.setExpectedDepartureTime(new Date(prediction.getTimepointPredictionRecord().getTimepointPredictedDepartureTime()));
                }
            }
        }

        Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        calendar.add(Calendar.SECOND, arrivalTime);
        siriOnwardCallBuilder.setAimedArrivalTime(calendar.getTime());

        // siri extensions
        SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
        ExtensionsStructure extensionsStructure = new ExtensionsStructure();
        SiriDistanceExtension distances = new SiriDistanceExtension();

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        df.setGroupingUsed(false);

        distances.setStopsFromCall(index);
        distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
        distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
        distances.setPresentableDistance(_presentationService.getPresentableDistance(distances));

        wrapper.setDistances(distances);
        extensionsStructure.setAny(wrapper);
        siriOnwardCallBuilder.setExtensions(extensionsStructure);

        return siriOnwardCallBuilder.build();
    }

}
