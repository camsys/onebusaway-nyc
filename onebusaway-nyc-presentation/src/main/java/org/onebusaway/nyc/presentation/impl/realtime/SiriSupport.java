/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.onebusaway.nyc.presentation.impl.realtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.presentation.impl.AgencySupportLibrary;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import uk.org.siri.siri.BlockRefStructure;
import uk.org.siri.siri.DataFrameRefStructure;
import uk.org.siri.siri.DestinationRefStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.JourneyPatternRefStructure;
import uk.org.siri.siri.JourneyPlaceRefStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.OnwardCallStructure;
import uk.org.siri.siri.OnwardCallsStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.ProgressRateEnumeration;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleRefStructure;

public final class SiriSupport {
  
  public enum OnwardCallsMode {
	  VEHICLE_MONITORING,
	  STOP_MONITORING
  }
	
  /**
   * The tripDetails bean here may not be for the trip the vehicle is currently on, in the case of A-D for stop!
   */
  public static void fillMonitoredVehicleJourney(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
	  TripDetailsBean tripDetails, StopBean monitoredCallStopBean, OnwardCallsMode onwardCallsMode,
      PresentationService presentationService, NycTransitDataService nycTransitDataService,
      int maximumOnwardCalls) {

	TripBean tripBean = tripDetails.getTrip();
	TripStatusBean tripStatus = tripDetails.getStatus();

	if(monitoredCallStopBean == null) {
		monitoredCallStopBean = tripStatus.getNextStop();
	}
	
	/////////////

	LineRefStructure lineRef = new LineRefStructure();
    lineRef.setValue(tripBean.getRoute().getId());
    monitoredVehicleJourney.setLineRef(lineRef);

    OperatorRefStructure operatorRef = new OperatorRefStructure();
    operatorRef.setValue(AgencySupportLibrary.getAgencyForId(tripBean.getRoute().getId()));
    monitoredVehicleJourney.setOperatorRef(operatorRef);

    DirectionRefStructure directionRef = new DirectionRefStructure();
    directionRef.setValue(tripBean.getDirectionId());
    monitoredVehicleJourney.setDirectionRef(directionRef);

    NaturalLanguageStringStructure routeShortName = new NaturalLanguageStringStructure();
    routeShortName.setValue(tripBean.getRoute().getShortName());
    monitoredVehicleJourney.setPublishedLineName(routeShortName);

    JourneyPatternRefStructure journeyPattern = new JourneyPatternRefStructure();
    journeyPattern.setValue(tripBean.getShapeId());
    monitoredVehicleJourney.setJourneyPatternRef(journeyPattern);
    
    NaturalLanguageStringStructure headsign = new NaturalLanguageStringStructure();
    headsign.setValue(tripBean.getTripHeadsign());
    monitoredVehicleJourney.setDestinationName(headsign);

    VehicleRefStructure vehicleRef = new VehicleRefStructure();
    vehicleRef.setValue(tripStatus.getVehicleId());
    monitoredVehicleJourney.setVehicleRef(vehicleRef);

    monitoredVehicleJourney.setMonitored(tripStatus.isPredicted());
    
    monitoredVehicleJourney.setBearing((float)tripStatus.getOrientation());

    monitoredVehicleJourney.setProgressRate(getProgressRateForPhaseAndStatus(
        tripStatus.getStatus(), tripStatus.getPhase()));

    // origin-destination
    List<TripStopTimeBean> stops = tripDetails.getSchedule().getStopTimes();

    JourneyPlaceRefStructure origin = new JourneyPlaceRefStructure();
    origin.setValue(stops.get(0).getStop().getId());
    monitoredVehicleJourney.setOriginRef(origin);

    StopBean lastStop = stops.get(stops.size() - 1).getStop();
    DestinationRefStructure dest = new DestinationRefStructure();
    dest.setValue(lastStop.getId());
    monitoredVehicleJourney.setDestinationRef(dest);

    // framed journey 
    FramedVehicleJourneyRefStructure framedJourney = new FramedVehicleJourneyRefStructure();
    DataFrameRefStructure dataFrame = new DataFrameRefStructure();
    dataFrame.setValue(String.format("%1$tY-%1$tm-%1$td", tripDetails.getServiceDate()));
    framedJourney.setDataFrameRef(dataFrame);
    framedJourney.setDatedVehicleJourneyRef(tripBean.getId());
    monitoredVehicleJourney.setFramedVehicleJourneyRef(framedJourney);
    
    // location
    // if vehicle is detected to be on detour, use actual lat/lon, not snapped location.
    LocationStructure location = new LocationStructure();

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(6);
    
    if (presentationService.isOnDetour(tripStatus)) {
      location.setLatitude(new BigDecimal(df.format(tripStatus.getLastKnownLocation().getLat())));
      location.setLongitude(new BigDecimal(df.format(tripStatus.getLastKnownLocation().getLon())));
    } else {
      location.setLatitude(new BigDecimal(df.format(tripStatus.getLocation().getLat())));
      location.setLongitude(new BigDecimal(df.format(tripStatus.getLocation().getLon())));
    }

    monitoredVehicleJourney.setVehicleLocation(location);

    // progress status
    List<String> progressStatuses = new ArrayList<String>();
    
    if (presentationService.isInLayover(tripStatus)) {
    	progressStatuses.add("layover");
    } else {
    	// "prevTrip" really means not on the framedvehiclejourney trip
    	if(!tripBean.getId().equals(tripStatus.getActiveTrip().getId())) {
    		progressStatuses.add("prevTrip");
    	}
    }
    
    if(!progressStatuses.isEmpty()) {
    	NaturalLanguageStringStructure progressStatus = new NaturalLanguageStringStructure();
        progressStatus.setValue(StringUtils.join(progressStatuses, ","));
        monitoredVehicleJourney.setProgressStatus(progressStatus);    	
    }

    // block
    if (presentationService.isBlockLevelInference(tripStatus)) {
    	BlockRefStructure blockRef = new BlockRefStructure();
    	blockRef.setValue(tripDetails.getTrip().getBlockId());
    	monitoredVehicleJourney.setBlockRef(blockRef);
    }
	  
	BlockInstanceBean blockInstance = 
			nycTransitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());

    // scheduled depature time
    if (presentationService.isBlockLevelInference(tripStatus) && presentationService.isInLayover(tripStatus)) {
		BlockStopTimeBean originDepartureStopTime = null;

		List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();
    	for(int t = 0; t < blockTrips.size(); t++) {
    		BlockTripBean thisTrip = blockTrips.get(t);
    		BlockTripBean nextTrip = null;    		
    		if(t + 1 < blockTrips.size()) {
    			nextTrip = blockTrips.get(t + 1);
    		}

    		if(thisTrip.getTrip().getId().equals(tripStatus.getActiveTrip().getId())) {    			
    			// just started new trip
    			if(tripStatus.getDistanceAlongTrip() < (0.5 * tripStatus.getTotalDistanceAlongTrip())) {
    				originDepartureStopTime = thisTrip.getBlockStopTimes().get(0);

    			// at end of previous trip
    			} else {
    				if(nextTrip != null) {
    					originDepartureStopTime = nextTrip.getBlockStopTimes().get(0);
    				}
    			}
    			
    			break;
    		}
    	}
    			
		if(originDepartureStopTime != null) {            	
			Date departureTime = new Date(tripDetails.getServiceDate() + (originDepartureStopTime.getStopTime().getDepartureTime() * 1000));
			monitoredVehicleJourney.setOriginAimedDepartureTime(departureTime);
		}
    }    
    
    // monitored call
    fillMonitoredCall(monitoredVehicleJourney, blockInstance, tripStatus, monitoredCallStopBean, 
    		presentationService, nycTransitDataService);

    // onward calls
    fillOnwardCalls(monitoredVehicleJourney, blockInstance, tripDetails, onwardCallsMode,
    		presentationService, nycTransitDataService, maximumOnwardCalls);
    
    // situations
    fillSituations(monitoredVehicleJourney, tripStatus);
    
    return;
  }
  
  /***
   * PRIVATE STATIC METHODS
   */
  private static void fillOnwardCalls(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
		  BlockInstanceBean blockInstance, TripDetailsBean tripDetails, OnwardCallsMode onwardCallsMode,
	      PresentationService presentationService, NycTransitDataService nycTransitDataService, 
	      int maximumOnwardCalls) {

	  TripStatusBean tripStatus = tripDetails.getStatus();	  
	  String tripIdOfMonitoredCall = tripDetails.getTripId();
	  
	  monitoredVehicleJourney.setOnwardCalls(new OnwardCallsStructure());

	  //////////
	  
	  // no need to go further if this is the case!
	  if(maximumOnwardCalls == 0) { 
		  return;
	  }

	  List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

	  double distanceOfVehicleAlongBlock = 0;
	  int blockTripStopsAfterTheVehicle = 0; 
	  int onwardCallsAdded = 0;
	  
	  boolean foundActiveTrip = false;
	  for(int i = 0; i < blockTrips.size(); i++) {
		  BlockTripBean blockTrip = blockTrips.get(i);
		  
		  if(!foundActiveTrip) {
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
				  distanceOfVehicleAlongBlock += tripStatus.getDistanceAlongTrip();

				  foundActiveTrip = true;
			  } else {
				  // a block trip's distance along block is the *beginning* of that block trip along the block
				  // so to get the size of this one, we have to look at the next.
				  if(i + 1 < blockTrips.size()) {
					  distanceOfVehicleAlongBlock = blockTrips.get(i + 1).getDistanceAlongBlock();
				  }

				  // bus has already served this trip, so no need to go further
				  continue;
			  }
		  }

		  if(onwardCallsMode == OnwardCallsMode.STOP_MONITORING) {
			  // always include onward calls for the trip the monitored call is on ONLY.
			  if(!blockTrip.getTrip().getId().equals(tripIdOfMonitoredCall)) {
				  continue;
			  }
		  }

		  HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();	   
		  for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {
			  int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStopTime().getStop());

			  // block trip stops away--on this trip, only after we've passed the stop, 
			  // on future trips, count always.
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
				  if(stopTime.getDistanceAlongBlock() >= distanceOfVehicleAlongBlock) {
					  blockTripStopsAfterTheVehicle++;
				  } else {
					  // stop is behind the bus--no need to go further
					  continue;
				  }

			  // future trip--bus hasn't reached this trip yet, so count all stops
			  } else {
				  blockTripStopsAfterTheVehicle++;
			  }

		      if(onwardCallsAdded >= maximumOnwardCalls) {
		    	return;
		      }
		    	
		      monitoredVehicleJourney.getOnwardCalls().getOnwardCall().add(
		    		  getOnwardCallStructure(stopTime.getStopTime().getStop(), presentationService, 
		    				  stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock(), 
							  stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock, 
		    				  visitNumber, blockTripStopsAfterTheVehicle - 1));

		      onwardCallsAdded++;
		  }
		  
		  if(onwardCallsMode == OnwardCallsMode.VEHICLE_MONITORING) {
			  // if we're in layover, stop adding onward calls after the next trip the bus is on
			  // if not, after the current trip
			  if(presentationService.isInLayover(tripStatus)) {
				  if(tripDetails.getSchedule().getNextTrip().getId().equals(blockTrip.getTrip().getId())) {
					  break;
				  }
			  } else {
				  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
					  break;
				  }
			  }
		  }
	  }
	  
	  return;
  }
  
  private static void fillMonitoredCall(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
		  BlockInstanceBean blockInstance, TripStatusBean tripStatus, StopBean monitoredCallStopBean, 
	      PresentationService presentationService, NycTransitDataService nycTransitDataService) {

	  List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

	  double distanceOfVehicleAlongBlock = 0;
	  int blockTripStopsAfterTheVehicle = 0; 

	  boolean foundActiveTrip = false;
	  for(int i = 0; i < blockTrips.size(); i++) {
		  BlockTripBean blockTrip = blockTrips.get(i);
		  
		  if(!foundActiveTrip) {
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
				  distanceOfVehicleAlongBlock += tripStatus.getDistanceAlongTrip();

				  foundActiveTrip = true;
			  } else {
				  // a block trip's distance along block is the *beginning* of that block trip along the block
				  // so to get the size of this one, we have to look at the next.
				  if(i + 1 < blockTrips.size()) {
					  distanceOfVehicleAlongBlock = blockTrips.get(i + 1).getDistanceAlongBlock();
				  }

				  // bus has already served this trip, so no need to go further
				  continue;
			  }
		  }
		  
		  HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();	   

		  for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {
			  int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStopTime().getStop());

			  // block trip stops away--on this trip, only after we've passed the stop, 
			  // on future trips, count always.
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
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

			  // monitored call
			  if(stopTime.getStopTime().getStop().getId().equals(monitoredCallStopBean.getId())) {    
				  if(!presentationService.isOnDetour(tripStatus)) {
					  monitoredVehicleJourney.setMonitoredCall(
							  getMonitoredCallStructure(stopTime.getStopTime().getStop(), presentationService, 
									  stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock(), 
									  stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock, 
									  visitNumber, blockTripStopsAfterTheVehicle - 1));
				  }

				  // we found our monitored call--stop
				  return;
			  }
		  }    	
	  }
  }
  
  private static void fillSituations(MonitoredVehicleJourneyStructure monitoredVehicleJourney, TripStatusBean tripStatus) {
	if (tripStatus == null || CollectionUtils.isEmpty(tripStatus.getSituations())) {
      return;
    }

    List<SituationRefStructure> situationRef = monitoredVehicleJourney.getSituationRef();

    for (ServiceAlertBean situation : tripStatus.getSituations()) {
      SituationRefStructure sitRef = new SituationRefStructure();
      SituationSimpleRefStructure sitSimpleRef = new SituationSimpleRefStructure();
      sitSimpleRef.setValue(situation.getId());
      sitRef.setSituationSimpleRef(sitSimpleRef);
      situationRef.add(sitRef);
    }
  }
  
  private static OnwardCallStructure getOnwardCallStructure(StopBean stopBean, 
      PresentationService presentationService, 
      double distanceOfCallAlongTrip, double distanceOfVehicleFromCall, int visitNumber, int index) {
    
    OnwardCallStructure onwardCallStructure = new OnwardCallStructure();
    onwardCallStructure.setVisitNumber(BigInteger.valueOf(visitNumber));

    StopPointRefStructure stopPointRef = new StopPointRefStructure();
    stopPointRef.setValue(stopBean.getId());
    onwardCallStructure.setStopPointRef(stopPointRef);

    NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
    stopPoint.setValue(stopBean.getName());
    onwardCallStructure.setStopPointName(stopPoint);

    // siri extensions
    SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
    ExtensionsStructure distancesExtensions = new ExtensionsStructure();
    SiriDistanceExtension distances = new SiriDistanceExtension();

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(2);
    df.setGroupingUsed(false);

    distances.setStopsFromCall(index);
    distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
    distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
    distances.setPresentableDistance(presentationService.getPresentableDistance(distances));

    wrapper.setDistances(distances);
    distancesExtensions.setAny(wrapper);    
    onwardCallStructure.setExtensions(distancesExtensions);

    return onwardCallStructure;
  }
    
  private static MonitoredCallStructure getMonitoredCallStructure(StopBean stopBean, 
      PresentationService presentationService, 
      double distanceOfCallAlongTrip, double distanceOfVehicleFromCall, int visitNumber, int index) {
    
    MonitoredCallStructure monitoredCallStructure = new MonitoredCallStructure();
    monitoredCallStructure.setVisitNumber(BigInteger.valueOf(visitNumber));

    StopPointRefStructure stopPointRef = new StopPointRefStructure();
    stopPointRef.setValue(stopBean.getId());
    monitoredCallStructure.setStopPointRef(stopPointRef);

    NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
    stopPoint.setValue(stopBean.getName());
    monitoredCallStructure.setStopPointName(stopPoint);

    // siri extensions
    SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
    ExtensionsStructure distancesExtensions = new ExtensionsStructure();
    SiriDistanceExtension distances = new SiriDistanceExtension();

    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(2);
    df.setGroupingUsed(false);
    
    distances.setStopsFromCall(index);
    distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
    distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
    distances.setPresentableDistance(presentationService.getPresentableDistance(distances));

    wrapper.setDistances(distances);
    distancesExtensions.setAny(wrapper);
    monitoredCallStructure.setExtensions(distancesExtensions);
    
    return monitoredCallStructure;
  }
    
  private static int getVisitNumber(HashMap<String, Integer> visitNumberForStop, StopBean stop) {
    int visitNumber;
   
    if (visitNumberForStop.containsKey(stop.getId())) {
      visitNumber = visitNumberForStop.get(stop.getId()) + 1;
    } else {
      visitNumber = 1;
    }
    
    visitNumberForStop.put(stop.getId(), visitNumber);
    
    return visitNumber;
  }
  
  private static ProgressRateEnumeration getProgressRateForPhaseAndStatus(String status, String phase) {
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
  
}
