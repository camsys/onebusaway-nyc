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

package org.onebusaway.nyc.presentation.impl.realtime.siri;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.AgencySupportLibrary;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportPredictionTimepointRecord;
import org.onebusaway.nyc.presentation.impl.realtime.siri.builder.SiriMonitoredVehicleJourneyBuilder;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriMonitoredCallBuilderService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriMonitoredVehicleJourneyBuilderService;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriOnwardCallsBuilderService;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.siri.support.SiriVehicleFeatures;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.VehicleFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.org.siri.siri.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

@Component
public class SiriMonitoredVehicleJourneyBuilderServiceImpl implements SiriMonitoredVehicleJourneyBuilderService {

    private ConfigurationService _configurationService;

    private NycTransitDataService _nycTransitDataService;

    private PresentationService _presentationService;

    private SiriMonitoredCallBuilderService _siriMonitoredCallBuilderService;

    private SiriOnwardCallsBuilderService _siriOnwardCallsBuilderService;


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
    public void setMonitoredCallBuilderService(SiriMonitoredCallBuilderService siriMonitoredCallBuilderService) {
        _siriMonitoredCallBuilderService = siriMonitoredCallBuilderService;
    }

    @Autowired
    public void setSiriOnwardCallsBuilderService(SiriOnwardCallsBuilderService siriOnwardCallsBuilderService) {
        _siriOnwardCallsBuilderService = siriOnwardCallsBuilderService;
    }

    @Override
    public VehicleActivityStructure.MonitoredVehicleJourney makeMonitoredVehicleJourney(TripBean tripOnBlock,
                                                                                        TripStatusBean currentlyActiveTripOnBlock,
                                                                                        StopBean monitoredCallStopBean,
                                                                                        Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap,
                                                                                        OnwardCallsMode onwardCallsMode,
                                                                                        int maximumOnwardCalls,
                                                                                        long responseTimestamp,
                                                                                        boolean showApc,
                                                                                        boolean showRawApc,
                                                                                        boolean isCancelled,
                                                                                        Set<VehicleFeature> vehicleFeatures){


        SiriMonitoredVehicleJourneyBuilder siriMonitoredVehicleJourneyBuilder = processMonitoredVehicleJourneyStructureWithBuilder(
            tripOnBlock, currentlyActiveTripOnBlock, monitoredCallStopBean, stopIdToPredictionRecordMap,
            onwardCallsMode, maximumOnwardCalls, responseTimestamp, showApc, showRawApc, isCancelled, vehicleFeatures);

        return siriMonitoredVehicleJourneyBuilder.buildMonitoredVehicleJourney();
    }

    @Override
    public MonitoredVehicleJourneyStructure makeMonitoredVehicleJourneyStructure(TripBean tripOnBlock,
                                                                                 TripStatusBean currentlyActiveTripOnBlock,
                                                                                 StopBean monitoredCallStopBean,
                                                                                 Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap,
                                                                                 OnwardCallsMode onwardCallsMode,
                                                                                 int maximumOnwardCalls,
                                                                                 long responseTimestamp,
                                                                                 boolean showApc,
                                                                                 boolean showRawApc,
                                                                                 boolean isCancelled,
                                                                                 Set<VehicleFeature> vehicleFeatures){

        SiriMonitoredVehicleJourneyBuilder siriMonitoredVehicleJourneyBuilder = processMonitoredVehicleJourneyStructureWithBuilder(
                tripOnBlock, currentlyActiveTripOnBlock, monitoredCallStopBean, stopIdToPredictionRecordMap,
                onwardCallsMode, maximumOnwardCalls, responseTimestamp, showApc, showRawApc, isCancelled, vehicleFeatures);

        return siriMonitoredVehicleJourneyBuilder.buildMonitoredVehicleJourneyStructure();
    }

    private SiriMonitoredVehicleJourneyBuilder processMonitoredVehicleJourneyStructureWithBuilder(TripBean tripOnBlock,
                                                                           TripStatusBean currentlyActiveTripOnBlock,
                                                                           StopBean monitoredCallStopBean,
                                                                           Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap,
                                                                           OnwardCallsMode onwardCallsMode,
                                                                           int maximumOnwardCalls,
                                                                           long responseTimestamp,
                                                                           boolean showApc,
                                                                           boolean showRawApc,
                                                                           boolean isCancelled,
                                                                           Set<VehicleFeature> vehicleFeatures){

        SiriMonitoredVehicleJourneyBuilder siriMonitoredVehicleJourneyBuilder = new SiriMonitoredVehicleJourneyBuilder();

        // Get Block Instance For Current Vehicle Trip
        BlockInstanceBean blockInstance = getBlockInstanceForTrip(currentlyActiveTripOnBlock);

        // Get All trips associated with Block
        List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();
        List<BlockStopTimeBean> blockTripStopTimes = getBlockTripStopTimes(blockTrips, tripOnBlock.getId());

        List<String> progressStatuses = getProgressStatuses(currentlyActiveTripOnBlock, tripOnBlock);

        // Get Next Upcoming Stop if one is not provided
        if(monitoredCallStopBean == null) {
            monitoredCallStopBean = currentlyActiveTripOnBlock.getNextStop();
        }

        siriMonitoredVehicleJourneyBuilder.setLineRef(getLineRef(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setOperatorRef(getOperatorRef(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setDirectionRef(getDirectionRef(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setPublishedLineName(getRouteShortName(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setJourneyPatternRef(getShapeId(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setDestinationName(getHeadsign(tripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setVehicleRef(getVehicleRef(currentlyActiveTripOnBlock));
        siriMonitoredVehicleJourneyBuilder.setMonitored(currentlyActiveTripOnBlock.isPredicted());
        siriMonitoredVehicleJourneyBuilder.setBearing((float) currentlyActiveTripOnBlock.getOrientation());

        siriMonitoredVehicleJourneyBuilder.setProgressRate(getProgressRateForPhaseAndStatus(
                currentlyActiveTripOnBlock.getStatus(), currentlyActiveTripOnBlock.getPhase()));

        // Set Origin and Destination
        if(!blockTripStopTimes.isEmpty()){
            siriMonitoredVehicleJourneyBuilder.setOriginRef(getOrigin(blockTripStopTimes));
            siriMonitoredVehicleJourneyBuilder.setDestinationRef(getDestination(blockTripStopTimes));
        }

        // Show Occupancy Enumeration eg. FEW_SEATS_AVAILABLE
        if(displayEnumeratedOccupancy(showApc, currentlyActiveTripOnBlock)) {
            siriMonitoredVehicleJourneyBuilder.setOccupancy(getOccupancyEnumeration(currentlyActiveTripOnBlock));
        }

        // Framed Journey
        siriMonitoredVehicleJourneyBuilder.setFramedVehicleJourneyRef(getFramedJourney(currentlyActiveTripOnBlock, tripOnBlock));

        // Location
        siriMonitoredVehicleJourneyBuilder.setVehicleLocation(getLocation(currentlyActiveTripOnBlock));

        // Progress Status
        if(!progressStatuses.isEmpty()) {
            siriMonitoredVehicleJourneyBuilder.setProgressStatus(getProgressStatus(progressStatuses));
        }

        // Block Ref - Checks to see if current vehicle is matched to a Block first before matching
        if (_presentationService.hasFormalBlockLevelMatch(currentlyActiveTripOnBlock)) {
            siriMonitoredVehicleJourneyBuilder.setBlockRef(getBlockRef(tripOnBlock));
        }

        // scheduled depature time
        if (_presentationService.hasFormalBlockLevelMatch(currentlyActiveTripOnBlock)
                && (_presentationService.isInLayover(currentlyActiveTripOnBlock)
                || !tripOnBlock.getId().equals(currentlyActiveTripOnBlock.getActiveTrip().getId()))) {

            BlockStopTimeBean originDepartureStopTime = getOriginDepartureStopTime(blockTrips, currentlyActiveTripOnBlock, progressStatuses);

            if(originDepartureStopTime != null) {
                Date departureTime = new Date(currentlyActiveTripOnBlock.getServiceDate() + (originDepartureStopTime.getStopTime().getDepartureTime() * 1000));
                siriMonitoredVehicleJourneyBuilder.setOriginAimedDepartureTime(departureTime);
            }
        }

        // monitored call
        if(!_presentationService.isOnDetour(currentlyActiveTripOnBlock)) {
            MonitoredCallStructure monitoredCall = _siriMonitoredCallBuilderService.makeMonitoredCall(
                    blockInstance, tripOnBlock, currentlyActiveTripOnBlock, monitoredCallStopBean,
                    stopIdToPredictionRecordMap, showRawApc, isCancelled, responseTimestamp,vehicleFeatures);
            siriMonitoredVehicleJourneyBuilder.setMonitoredCall(monitoredCall);
        }

        // onward calls
        if(!_presentationService.isOnDetour(currentlyActiveTripOnBlock)){
            OnwardCallsStructure onwardCallStructure = _siriOnwardCallsBuilderService
                    .makeOnwardCalls(blockInstance, tripOnBlock,currentlyActiveTripOnBlock, onwardCallsMode,
                            stopIdToPredictionRecordMap, maximumOnwardCalls,isCancelled, responseTimestamp);
            siriMonitoredVehicleJourneyBuilder.setOnwardCalls(onwardCallStructure);
        }

        // situations
        siriMonitoredVehicleJourneyBuilder.setSituationRef(getSituationRef(currentlyActiveTripOnBlock));

        return siriMonitoredVehicleJourneyBuilder;
    }

    private BlockInstanceBean getBlockInstanceForTrip(TripStatusBean currentVehicleTripStatus){
        return _nycTransitDataService.getBlockInstance(currentVehicleTripStatus.getActiveTrip().getBlockId(),
                currentVehicleTripStatus.getServiceDate());
    }

    private List<BlockStopTimeBean> getBlockTripStopTimes(List<BlockTripBean> blockTrips, String tripId){
        for(BlockTripBean blockTrip : blockTrips){
            if(blockTrip.getTrip().getId().equals(tripId)) {
                return blockTrip.getBlockStopTimes();
            }
        }
        return Collections.EMPTY_LIST;
    }

    private LineRefStructure getLineRef(TripBean framedJourneyTripBean){
        LineRefStructure lineRef = new LineRefStructure();
        lineRef.setValue(framedJourneyTripBean.getRoute().getId());
        return lineRef;
    }

    private OperatorRefStructure getOperatorRef(TripBean framedJourneyTripBean){
        OperatorRefStructure operatorRef = new OperatorRefStructure();
        operatorRef.setValue(AgencySupportLibrary.getAgencyForId(framedJourneyTripBean.getRoute().getId()));
        return operatorRef;
    }

    private DirectionRefStructure getDirectionRef(TripBean framedJourneyTripBean){
        DirectionRefStructure directionRef = new DirectionRefStructure();
        directionRef.setValue(framedJourneyTripBean.getDirectionId());
        return directionRef;
    }

    private NaturalLanguageStringStructure getRouteShortName(TripBean framedJourneyTripBean){
        NaturalLanguageStringStructure routeShortName = new NaturalLanguageStringStructure();
        routeShortName.setValue(framedJourneyTripBean.getRoute().getShortName());
        return routeShortName;
    }

    private JourneyPatternRefStructure getShapeId(TripBean framedJourneyTripBean){
        JourneyPatternRefStructure journeyPattern = new JourneyPatternRefStructure();
        journeyPattern.setValue(framedJourneyTripBean.getShapeId());
        return journeyPattern;
    }

    private NaturalLanguageStringStructure getHeadsign(TripBean framedJourneyTripBean){
        NaturalLanguageStringStructure headsign = new NaturalLanguageStringStructure();
        headsign.setValue(framedJourneyTripBean.getTripHeadsign());
        return headsign;
    }

    private VehicleRefStructure getVehicleRef(TripStatusBean currentVehicleTripStatus){
        VehicleRefStructure vehicleRef = new VehicleRefStructure();
        vehicleRef.setValue(currentVehicleTripStatus.getVehicleId());
        return vehicleRef;
    }

    private ProgressRateEnumeration getProgressRateForPhaseAndStatus(String status, String phase) {
        if (phase == null) {
            return ProgressRateEnumeration.UNKNOWN;
        }

        if (phase.toLowerCase().startsWith("layover")
                || phase.toLowerCase().startsWith("deadhead")
                || phase.toLowerCase().equals("at_base")) {
            return ProgressRateEnumeration.NO_PROGRESS;
        }

        if (status != null && status.toLowerCase().equals("stalled")) {
            return ProgressRateEnumeration.NO_PROGRESS;
        }

        if (phase.toLowerCase().equals("in_progress")) {
            return ProgressRateEnumeration.NORMAL_PROGRESS;
        }

        return ProgressRateEnumeration.UNKNOWN;
    }

    private boolean displayEnumeratedOccupancy(boolean showApc, TripStatusBean currentVehicleTripStatus){
        if (!showApc
                || currentVehicleTripStatus == null
                || currentVehicleTripStatus.getActiveTrip() == null
                || currentVehicleTripStatus.getActiveTrip().getRoute() ==  null
                || currentVehicleTripStatus.getVehicleId() == null)
        {
            return false;
        }
        return true;
    }

    private JourneyPlaceRefStructure getOrigin(List<BlockStopTimeBean> blockTripStopTimes) {
        if(blockTripStopTimes != null && !blockTripStopTimes.isEmpty()) {
            JourneyPlaceRefStructure origin = new JourneyPlaceRefStructure();
            origin.setValue(blockTripStopTimes.get(0).getStopTime().getStop().getId());
            return origin;
        }
        return null;
    }

    private DestinationRefStructure getDestination(List<BlockStopTimeBean> blockTripStopTimes) {
        if(blockTripStopTimes != null && blockTripStopTimes.size() > 1){
            DestinationRefStructure dest = new DestinationRefStructure();
            StopBean lastStop = blockTripStopTimes.get(blockTripStopTimes.size() - 1).getStopTime().getStop();
            dest.setValue(lastStop.getId());
        }
        return null;
    }

    private OccupancyEnumeration getOccupancyEnumeration(TripStatusBean currentVehicleTripStatus){
        VehicleOccupancyRecord vor =
                _nycTransitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(
                        AgencyAndId.convertFromString(currentVehicleTripStatus.getVehicleId()),
                        currentVehicleTripStatus.getActiveTrip().getRoute().getId(),
                        currentVehicleTripStatus.getActiveTrip().getDirectionId());
        return mapOccupancyStatusToEnumeration(vor);
    }

    private OccupancyEnumeration mapOccupancyStatusToEnumeration(VehicleOccupancyRecord vor) {
        if (vor == null || vor.getOccupancyStatus() == null) return null;
        switch (vor.getOccupancyStatus()) {
            case UNKNOWN:
                return null;
            case EMPTY:
            case MANY_SEATS_AVAILABLE:
            case FEW_SEATS_AVAILABLE:
                return OccupancyEnumeration.SEATS_AVAILABLE;
            case STANDING_ROOM_ONLY:
                return OccupancyEnumeration.STANDING_AVAILABLE;
            case FULL:
            case CRUSHED_STANDING_ROOM_ONLY:
            case NOT_ACCEPTING_PASSENGERS:
                return OccupancyEnumeration.FULL;
            default:
                return null;
        }
    }

    private FramedVehicleJourneyRefStructure getFramedJourney(TripStatusBean currentVehicleTripStatus, TripBean framedJourneyTripBean) {
        FramedVehicleJourneyRefStructure framedJourney = new FramedVehicleJourneyRefStructure();
        DataFrameRefStructure dataFrame = new DataFrameRefStructure();
        dataFrame.setValue(String.format("%1$tY-%1$tm-%1$td", currentVehicleTripStatus.getServiceDate()));
        framedJourney.setDataFrameRef(dataFrame);
        framedJourney.setDatedVehicleJourneyRef(framedJourneyTripBean.getId());
        return framedJourney;
    }

    private LocationStructure getLocation(TripStatusBean currentVehicleTripStatus) {
        LocationStructure location = new LocationStructure();

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(6);

        if (_presentationService.isOnDetour(currentVehicleTripStatus)) {
            location.setLatitude(new BigDecimal(df.format(currentVehicleTripStatus.getLastKnownLocation().getLat())));
            location.setLongitude(new BigDecimal(df.format(currentVehicleTripStatus.getLastKnownLocation().getLon())));
        } else {
            location.setLatitude(new BigDecimal(df.format(currentVehicleTripStatus.getLocation().getLat())));
            location.setLongitude(new BigDecimal(df.format(currentVehicleTripStatus.getLocation().getLon())));
        }

        return location;
    }

    private List<String> getProgressStatuses(TripStatusBean currentVehicleTripStatus, TripBean framedJourneyTripBean) {
        List<String> progressStatuses = new ArrayList<String>();

        if (_presentationService.isInLayover(currentVehicleTripStatus)) {
            progressStatuses.add("layover");
        }

        if (_presentationService.isSpooking(currentVehicleTripStatus)) {
            progressStatuses.add("spooking");
        }

        // "prevTrip" really means not on the framedvehiclejourney trip
        // Indicates that this trip is in the future and not currently active
        if(!framedJourneyTripBean.getId().equals(currentVehicleTripStatus.getActiveTrip().getId())) {
            progressStatuses.add("prevTrip");
        }

        return progressStatuses;
    }

    private BlockRefStructure getBlockRef(TripBean framedJourneyTripBean) {
        BlockRefStructure blockRef = new BlockRefStructure();
        blockRef.setValue(framedJourneyTripBean.getBlockId());
        return blockRef;
    }

    private NaturalLanguageStringStructure getProgressStatus(List<String> progressStatuses) {
        NaturalLanguageStringStructure progressStatus = new NaturalLanguageStringStructure();
        progressStatus.setValue(StringUtils.join(progressStatuses, ","));
        return progressStatus;
    }

    private BlockStopTimeBean getOriginDepartureStopTime(List<BlockTripBean> blockTrips,
                                                         TripStatusBean currentVehicleTripStatus,
                                                         List<String> progressStatuses) {
        for(int t = 0; t < blockTrips.size(); t++) {
            BlockTripBean thisTrip = blockTrips.get(t);
            BlockTripBean nextTrip = null;
            if(t + 1 < blockTrips.size()) {
                nextTrip = blockTrips.get(t + 1);
            }

            if(thisTrip.getTrip().getId().equals(currentVehicleTripStatus.getActiveTrip().getId())) {
                // just started new trip
                if(currentVehicleTripStatus.getDistanceAlongTrip() < (0.5 * currentVehicleTripStatus.getTotalDistanceAlongTrip())
                        && !progressStatuses.contains("prevTrip")) {

                    return thisTrip.getBlockStopTimes().get(0);

                    // at end of previous trip
                } else {
                    if(nextTrip != null) {
                        int blockStopTimesLastIndex = thisTrip.getBlockStopTimes().size() - 1;
                        BlockStopTimeBean currentTripFinalStopTime = thisTrip.getBlockStopTimes().get(blockStopTimesLastIndex);

                        int currentTripLastStopArrivalTime = currentTripFinalStopTime.getStopTime().getArrivalTime();
                        int nextTripFirstStopDepartureTime = nextTrip.getBlockStopTimes().get(0).getStopTime().getDepartureTime();

                        if(nextTripFirstStopDepartureTime - currentTripLastStopArrivalTime > 60){
                            return nextTrip.getBlockStopTimes().get(0);
                        }
                    }
                }

                break;
            }
        }
        return null;
    }

    private List<SituationRefStructure> getSituationRef(TripStatusBean currentlyActiveTripOnBlock){
        List<SituationRefStructure> situationRef = new ArrayList<>();

        if (currentlyActiveTripOnBlock != null &&
                currentlyActiveTripOnBlock.getSituations() != null &&
                !currentlyActiveTripOnBlock.getSituations().isEmpty()) {

            for (ServiceAlertBean situation : currentlyActiveTripOnBlock.getSituations()) {
                SituationRefStructure sitRef = new SituationRefStructure();
                SituationSimpleRefStructure sitSimpleRef = new SituationSimpleRefStructure();
                sitSimpleRef.setValue(situation.getId());
                sitRef.setSituationSimpleRef(sitSimpleRef);
                situationRef.add(sitRef);
            }
        }

        return situationRef;
    }

}
