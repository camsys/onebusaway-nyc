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
    }

    // "prevTrip" really means not on the framedvehiclejourney trip
    if(!tripBean.getId().equals(tripStatus.getActiveTrip().getId())) {
    	progressStatuses.add("prevTrip");
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
     
    // scheduled depature time
    if (presentationService.isBlockLevelInference(tripStatus) && presentationService.isInLayover(tripStatus)) {
    	// if we're less than 50% of the trip, assume we've already started the next trip, otherwise, advance
    	// to it if we're over 50% (at the end) of the last one. 
    	TripDetailsBean nextTripDetails = null;    	

    	if(tripStatus.getDistanceAlongTrip() < (0.5 * tripStatus.getTotalDistanceAlongTrip())) {
    		nextTripDetails = tripDetails;
    	} else {
    		TripBean nextTrip = tripDetails.getSchedule().getNextTrip();

    		if(nextTrip != null) {
    			TripDetailsQueryBean query = new TripDetailsQueryBean();
    			query.setTripId(nextTrip.getId());
    			query.setVehicleId(tripDetails.getStatus().getVehicleId());        	
    			nextTripDetails = nycTransitDataService.getSingleTripDetails(query);
    		}
    	}
    	
    	if(nextTripDetails != null) {
			List<TripStopTimeBean> stopTimesForNextTrip = nextTripDetails.getSchedule().getStopTimes();
			TripStopTimeBean firstStopTime = stopTimesForNextTrip.get(0);
        	
			if(firstStopTime != null) {            	
				Date departureTime = new Date(tripDetails.getServiceDate() + (firstStopTime.getDepartureTime() * 1000));
				monitoredVehicleJourney.setOriginAimedDepartureTime(departureTime);
			}
		}
    }    
    
    // monitored call
    fillMonitoredCall(monitoredVehicleJourney, tripStatus, monitoredCallStopBean, 
    		presentationService, nycTransitDataService);

    // onward calls
    fillOnwardCalls(monitoredVehicleJourney, tripDetails, onwardCallsMode,
    		presentationService, nycTransitDataService, maximumOnwardCalls);
    
    // situations
    fillSituations(monitoredVehicleJourney, tripStatus);
    
    return;
  }
  
  /***
   * PRIVATE STATIC METHODS
   */
  private static void fillOnwardCalls(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
		  TripDetailsBean tripDetails, OnwardCallsMode onwardCallsMode,
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
	  
	  BlockInstanceBean blockInstance = 
			  nycTransitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());

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
					  distanceOfVehicleAlongBlock += blockTrips.get(i + 1).getDistanceAlongBlock();
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
		  
		  // find stops for this trip
		  TripDetailsQueryBean blockTripQuery = new TripDetailsQueryBean();
		  blockTripQuery.setTripId(blockTrip.getTrip().getId());
		  blockTripQuery.setVehicleId(tripStatus.getVehicleId());

		  TripDetailsBean blockTripDetails = 
				  nycTransitDataService.getSingleTripDetails(blockTripQuery);

		  List<TripStopTimeBean> stopTimes = blockTripDetails.getSchedule().getStopTimes();
		  Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
			  public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
				  return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
			  }
		  });		

		  HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();	   
		  for(TripStopTimeBean stopTime : stopTimes) {
			  int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStop());

			  // block trip stops away--on this trip, only after we've passed the stop, 
			  // on future trips, count always.
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
				  if(stopTime.getDistanceAlongTrip() >= tripStatus.getDistanceAlongTrip()) {
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
		    		  getOnwardCallStructure(stopTime.getStop(), presentationService, 
		    				  stopTime.getDistanceAlongTrip(), 
							  (blockTrip.getDistanceAlongBlock() + stopTime.getDistanceAlongTrip()) - distanceOfVehicleAlongBlock, 
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
		  TripStatusBean tripStatus, StopBean monitoredCallStopBean, 
	      PresentationService presentationService, NycTransitDataService nycTransitDataService) {

	  BlockInstanceBean blockInstance = 
			  nycTransitDataService.getBlockInstance(tripStatus.getActiveTrip().getBlockId(), tripStatus.getServiceDate());

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
					  distanceOfVehicleAlongBlock += blockTrips.get(i + 1).getDistanceAlongBlock();
				  }

				  // bus has already served this trip, so no need to go further
				  continue;
			  }
		  }
		  
		  // find stops for this trip
		  TripDetailsQueryBean blockTripQuery = new TripDetailsQueryBean();
		  blockTripQuery.setTripId(blockTrip.getTrip().getId());
		  blockTripQuery.setVehicleId(tripStatus.getVehicleId());

		  TripDetailsBean blockTripDetails = 
				  nycTransitDataService.getSingleTripDetails(blockTripQuery);

		  List<TripStopTimeBean> stopTimes = blockTripDetails.getSchedule().getStopTimes();
		  Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
			  public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
				  return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
			  }
		  });		

		  HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();	   

		  for(TripStopTimeBean stopTime : stopTimes) {
			  int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStop());

			  // block trip stops away--on this trip, only after we've passed the stop, 
			  // on future trips, count always.
			  if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
				  if(stopTime.getDistanceAlongTrip() >= tripStatus.getDistanceAlongTrip()) {
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
			  if(stopTime.getStop().getId().equals(monitoredCallStopBean.getId())) {    
				  if(!presentationService.isOnDetour(tripStatus)) {
					  monitoredVehicleJourney.setMonitoredCall(
							  getMonitoredCallStructure(stopTime.getStop(), presentationService, 
									  stopTime.getDistanceAlongTrip(), 
									  (blockTrip.getDistanceAlongBlock() + stopTime.getDistanceAlongTrip()) - distanceOfVehicleAlongBlock, 
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
