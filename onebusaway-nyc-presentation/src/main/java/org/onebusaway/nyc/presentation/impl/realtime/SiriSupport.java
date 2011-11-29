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

import org.onebusaway.nyc.presentation.impl.AgencySupportLibrary;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.collections.CollectionUtils;

import uk.org.siri.siri.JourneyPatternRefStructure;
import uk.org.siri.siri.DataFrameRefStructure;
import uk.org.siri.siri.DestinationRefStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SiriSupport {

  private PresentationService _presentationService;

  private TransitDataService _transitDataService;
  
  private Date _now = null;
  
  public void setTime(Date time) {
    _now = time;
  }

  public long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();
  }
  
  public void setTransitDataService(TransitDataService transitDataService) {
    _transitDataService = transitDataService;
  }

  public void setPresentationService(PresentationService presentationService) {
    _presentationService = presentationService;
  }

  // TODO: refactor the below two methods to use the same core logic? they seem basically the same...
  private MonitoredCallStructure getMonitoredCall(List<TripStopTimeBean> stopTimes, 
      StopBean monitoredCallStopBean, TripStatusBean statusBean) {

    double distance = statusBean.getDistanceAlongTrip();

    // sort stop times--important!
    Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
      public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
        return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
      }
    });

    HashMap<String, Integer> visitNumberForStop = new HashMap<String, Integer>();
    boolean afterStart = false;
    int i = 0;
    for (TripStopTimeBean stopTime : stopTimes) {
      StopBean stop = stopTime.getStop();

      int visitNumber = getVisitNumber(visitNumberForStop, stop);

      if (stopTime.getDistanceAlongTrip() >= distance) {
        afterStart = true;
      }

      if (afterStart) {
        i++;

        if (stop.getId().equals(monitoredCallStopBean.getId())) {
          MonitoredCallStructure monitoredCall = new MonitoredCallStructure();

          StopPointRefStructure stopPointRef = new StopPointRefStructure();
          stopPointRef.setValue(stop.getId());
          monitoredCall.setStopPointRef(stopPointRef);

          NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
          stopPoint.setValue(stop.getName());
          monitoredCall.setStopPointName(stopPoint);

          monitoredCall.setVisitNumber(BigInteger.valueOf(visitNumber));

          SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
          ExtensionsStructure distancesExtensions = new ExtensionsStructure();
          SiriDistanceExtension distances = new SiriDistanceExtension();

          distances.setStopsFromCall(i - 1);
          distances.setCallDistanceAlongRoute(stopTime.getDistanceAlongTrip());
          distances.setDistanceFromCall(stopTime.getDistanceAlongTrip() - distance);
          distances.setPresentableDistance(_presentationService.getPresentableDistance(distances));

          wrapper.setDistances(distances);
          distancesExtensions.setAny(wrapper);
          monitoredCall.setExtensions(distancesExtensions);

          return monitoredCall;
        }
      }
    }

    return null;
  }

  private OnwardCallsStructure getOnwardCalls(List<TripStopTimeBean> stopTimes, 
      StopBean monitoredCallStopBean, TripStatusBean statusBean) {

    double distance = statusBean.getDistanceAlongTrip();

    // sort stop times--important!
    Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
      public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
        return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
      }
    });

    OnwardCallsStructure onwardCalls = new OnwardCallsStructure();

    HashMap<String, Integer> visitNumberForStop = new HashMap<String, Integer>();
    boolean afterStart = false;
    boolean afterStop = (statusBean.getNextStop() == null);
    int i = 0;
    for (TripStopTimeBean stopTime : stopTimes) {
      StopBean stop = stopTime.getStop();

      int visitNumber = getVisitNumber(visitNumberForStop, stop);

      if (stopTime.getDistanceAlongTrip() >= distance) {
        afterStart = true;
      }

      if (afterStart) {
        i += 1;

        if(afterStop) {
          OnwardCallStructure onwardCall = new OnwardCallStructure();

          StopPointRefStructure stopPoint = new StopPointRefStructure();
          stopPoint.setValue(stop.getId());
          onwardCall.setStopPointRef(stopPoint);

          NaturalLanguageStringStructure stopName = new NaturalLanguageStringStructure();
          stopName.setValue(stop.getName());
          onwardCall.setStopPointName(stopName);

          onwardCall.setVisitNumber(BigInteger.valueOf(visitNumber));

          SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
          ExtensionsStructure distancesExtensions = new ExtensionsStructure();
          SiriDistanceExtension distances = new SiriDistanceExtension();

          distances.setStopsFromCall(i - 1);
          distances.setCallDistanceAlongRoute(stopTime.getDistanceAlongTrip());
          distances.setDistanceFromCall(stopTime.getDistanceAlongTrip() - distance);
          distances.setPresentableDistance(_presentationService.getPresentableDistance(distances));

          wrapper.setDistances(distances);
          distancesExtensions.setAny(wrapper);
          onwardCall.setExtensions(distancesExtensions);

          onwardCalls.getOnwardCall().add(onwardCall);
        }
      }
      
      if(stop.getId().equals(monitoredCallStopBean.getId())) {
         afterStop = true;
      }
    }

    if (onwardCalls.getOnwardCall().size() == 0) {
      return null;
    }

    return onwardCalls;
  }

  public void fillMonitoredVehicleJourney(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
      TripBean tripBean, TripDetailsBean tripDetails, StopBean monitoredCallStopBean,
      boolean includeOnwardCalls) {

    LineRefStructure lineRef = new LineRefStructure();
    lineRef.setValue(tripBean.getRoute().getId());
    monitoredVehicleJourney.setLineRef(lineRef);

    OperatorRefStructure operatorRef = new OperatorRefStructure();
    operatorRef.setValue(AgencySupportLibrary.getAgencyForId(tripBean.getRoute().getId()));
    monitoredVehicleJourney.setOperatorRef(operatorRef);

    DirectionRefStructure directionRef = new DirectionRefStructure();
    directionRef.setValue(tripBean.getDirectionId());
    monitoredVehicleJourney.setDirectionRef(directionRef);

    NaturalLanguageStringStructure routeLongName = new NaturalLanguageStringStructure();
    routeLongName.setValue(tripBean.getRoute().getLongName());
    monitoredVehicleJourney.setPublishedLineName(routeLongName);

    JourneyPatternRefStructure journeyPattern = new JourneyPatternRefStructure();
    journeyPattern.setValue(tripBean.getShapeId());
    monitoredVehicleJourney.setJourneyPatternRef(journeyPattern);
    
    NaturalLanguageStringStructure headsign = new NaturalLanguageStringStructure();
    headsign.setValue(tripBean.getTripHeadsign());
    monitoredVehicleJourney.setDestinationName(headsign);

    if (_presentationService.isInLayover(tripDetails.getStatus())) {
      NaturalLanguageStringStructure progressStatus = new NaturalLanguageStringStructure();
      progressStatus.setValue("layover");
      monitoredVehicleJourney.setProgressStatus(progressStatus);
    }

    VehicleRefStructure vehicleRef = new VehicleRefStructure();
    vehicleRef.setValue(tripDetails.getStatus().getVehicleId());
    monitoredVehicleJourney.setVehicleRef(vehicleRef);

    monitoredVehicleJourney.setMonitored(tripDetails.getStatus().isPredicted());

    monitoredVehicleJourney.setBearing((float) tripDetails.getStatus().getOrientation());

    monitoredVehicleJourney.setProgressRate(getProgressRateForPhaseAndStatus(
        tripDetails.getStatus().getStatus(), tripDetails.getStatus().getPhase()));

    // framed journey
    FramedVehicleJourneyRefStructure framedJourney = new FramedVehicleJourneyRefStructure();
    DataFrameRefStructure dataFrame = new DataFrameRefStructure();
    dataFrame.setValue(String.format("%1$tY-%1$tm-%1$td", tripDetails.getServiceDate()));
    framedJourney.setDataFrameRef(dataFrame);
    framedJourney.setDatedVehicleJourneyRef(tripBean.getId());
    monitoredVehicleJourney.setFramedVehicleJourneyRef(framedJourney);

    // origin/dest.
    List<TripStopTimeBean> stops = tripDetails.getSchedule().getStopTimes();

    JourneyPlaceRefStructure origin = new JourneyPlaceRefStructure();
    origin.setValue(stops.get(0).getStop().getId());
    monitoredVehicleJourney.setOriginRef(origin);

    DestinationRefStructure dest = new DestinationRefStructure();
    StopBean lastStop = stops.get(stops.size() - 1).getStop();
    dest.setValue(lastStop.getId());
    monitoredVehicleJourney.setDestinationRef(dest);

    LocationStructure location = new LocationStructure();

    // if vehicle is detected to be on detour, use actual lat/lon, not snapped location.
    if (_presentationService.isOnDetour(tripDetails.getStatus())) {
      location.setLatitude(new BigDecimal(tripDetails.getStatus().getLastKnownLocation().getLat()));
      location.setLongitude(new BigDecimal(tripDetails.getStatus().getLastKnownLocation().getLon()));
    } else {
      location.setLatitude(new BigDecimal(tripDetails.getStatus().getLocation().getLat()));
      location.setLongitude(new BigDecimal(tripDetails.getStatus().getLocation().getLon()));
    }

    monitoredVehicleJourney.setVehicleLocation(location);

    // situations
    addSituations(monitoredVehicleJourney, tripDetails);

    // include stop times from the next trip if we're in layover on the previous trip
    List<TripStopTimeBean> stopTimes = 
        getStopTimesForTripDetails(tripDetails, _presentationService.isInLayover(tripDetails.getStatus()));

    // monitored calls
    if (monitoredCallStopBean != null && !_presentationService.isOnDetour(tripDetails.getStatus())) {
      monitoredVehicleJourney.setMonitoredCall(getMonitoredCall(stopTimes, monitoredCallStopBean, tripDetails.getStatus()));
    }

    // onward calls
    if (includeOnwardCalls && !_presentationService.isOnDetour(tripDetails.getStatus())) {
      monitoredVehicleJourney.setOnwardCalls(getOnwardCalls(stopTimes, monitoredCallStopBean, tripDetails.getStatus()));
    }
  }
  
  private List<TripStopTimeBean> getStopTimesForTripDetails(TripDetailsBean tripDetails, boolean includeNextTrip) {
    List<TripStopTimeBean> output = tripDetails.getSchedule().getStopTimes();
    
    TripBean nextTrip = tripDetails.getSchedule().getNextTrip();
    
    if(nextTrip != null && includeNextTrip == true) {
      TripDetailsQueryBean query = new TripDetailsQueryBean();
      query.setTripId(nextTrip.getId());
      query.setTime(getTime());
      query.setVehicleId(tripDetails.getStatus().getVehicleId());
      query.setServiceDate(tripDetails.getStatus().getServiceDate());
        
      TripDetailsBean nextTripDetails = _transitDataService.getSingleTripDetails(query);

      double offset = tripDetails.getStatus().getDistanceAlongTrip();
      for(TripStopTimeBean stopTime : nextTripDetails.getSchedule().getStopTimes()) {
        stopTime.setDistanceAlongTrip(stopTime.getDistanceAlongTrip() + offset);
        output.add(stopTime);
      }
    }
    
    return output;
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

  private void addSituations(MonitoredVehicleJourneyStructure monitoredVehicleJourney, TripDetailsBean trip) {
    if (trip == null || CollectionUtils.isEmpty(trip.getSituations())) {
      return;
    }

    List<SituationRefStructure> situationRef = monitoredVehicleJourney.getSituationRef();

    for (ServiceAlertBean situation : trip.getSituations()) {
      SituationRefStructure sitRef = new SituationRefStructure();
      SituationSimpleRefStructure sitSimpleRef = new SituationSimpleRefStructure();
      sitSimpleRef.setValue(situation.getId());
      sitRef.setSituationSimpleRef(sitSimpleRef);
      situationRef.add(sitRef);
    }
  }

}
