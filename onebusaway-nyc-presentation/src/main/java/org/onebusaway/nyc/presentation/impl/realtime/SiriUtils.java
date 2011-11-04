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

import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import uk.org.siri.siri.CourseOfJourneyStructure;
import uk.org.siri.siri.DataFrameRefStructure;
import uk.org.siri.siri.DestinationRefStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.JourneyPlaceRefStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.OnwardCallStructure;
import uk.org.siri.siri.OnwardCallsStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.ProgressRateEnumeration;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;
import uk.org.siri.siri.VehicleRefStructure;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SiriUtils {
  
  private static String getAgencyForId(String id) {
    String[] parts = id.split("_");
    return parts[0];
  }
  
  public static MonitoredCallStructure getMonitoredCall(List<TripStopTimeBean> stopTimes, StopBean monitoredCallStopBean, 
      TripStatusBean statusBean) {

    double distance = statusBean.getDistanceAlongTrip();
    if (Double.isNaN(distance)) {
      distance = statusBean.getScheduledDistanceAlongTrip();
    }
    
    // sort stop times--important!
    Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
      public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
        return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
      }
    });

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
        i++;

        if (stop.getId().equals(monitoredCallStopBean.getId())) {
          afterStop = true;
        }

        if(afterStop) {
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
          wrapper.setDistances(distances);
          distancesExtensions.setAny(wrapper);
          monitoredCall.setExtensions(distancesExtensions);

          return monitoredCall;
        }
      }
    }
    
    return null;
  }
  
  public static OnwardCallsStructure getOnwardCalls(List<TripStopTimeBean> stopTimes, TripStatusBean statusBean) {

    double distance = statusBean.getDistanceAlongTrip();
    if (Double.isNaN(distance)) {
      distance = statusBean.getScheduledDistanceAlongTrip();
    }
    
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
        
        if (afterStop) {
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
          distances.setDistanceFromCall(stopTime.getDistanceAlongTrip());
          wrapper.setDistances(distances);
          distancesExtensions.setAny(wrapper);
          onwardCall.setExtensions(distancesExtensions);

          onwardCalls.getOnwardCall().add(onwardCall);
        }

        if (stop.equals(statusBean.getNextStop())) {
          afterStop = true;
        }
      }
    }
    
    if (onwardCalls.getOnwardCall().size() == 0) {
      return null;
    }
    
    return onwardCalls;
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

  public static MonitoredVehicleJourney getMonitoredVehicleJourney(TripDetailsBean trip, StopBean monitoredCallStopBean, 
      boolean includeOnwardCalls) {

    TripBean tripBean = trip.getTrip();

    MonitoredVehicleJourney monitoredVehicleJourney = new MonitoredVehicleJourney();

    CourseOfJourneyStructure journey = new CourseOfJourneyStructure();
    journey.setValue(trip.getTripId());    
    monitoredVehicleJourney.setCourseOfJourneyRef(journey);
    
    LineRefStructure lineRef = new LineRefStructure();
    lineRef.setValue(tripBean.getRoute().getId());
    monitoredVehicleJourney.setLineRef(lineRef);

    OperatorRefStructure operatorRef = new OperatorRefStructure();
    operatorRef.setValue(getAgencyForId(tripBean.getRoute().getId()));
    monitoredVehicleJourney.setOperatorRef(operatorRef);

    if(isAtTerminal(trip.getStatus())) {
      NaturalLanguageStringStructure progressStatus = new NaturalLanguageStringStructure();
      progressStatus.setValue("atTerminal");
      monitoredVehicleJourney.setProgressStatus(progressStatus);    
    }
    
    DirectionRefStructure directionRef = new DirectionRefStructure();
    directionRef.setValue(tripBean.getDirectionId());
    monitoredVehicleJourney.setDirectionRef(directionRef);

    NaturalLanguageStringStructure headsign = new NaturalLanguageStringStructure();
    headsign.setValue(tripBean.getTripHeadsign());
    monitoredVehicleJourney.setPublishedLineName(headsign);
  
    VehicleRefStructure vehicleRef = new VehicleRefStructure();
    vehicleRef.setValue(trip.getStatus().getVehicleId());
    monitoredVehicleJourney.setVehicleRef(vehicleRef);
    
    monitoredVehicleJourney.setMonitored(trip.getStatus().isPredicted());

    monitoredVehicleJourney.setBearing((float)trip.getStatus().getOrientation());

    monitoredVehicleJourney.setProgressRate(getProgressRateForPhaseAndStatus(trip.getStatus().getStatus(), 
        trip.getStatus().getPhase()));    

    // framed journey
    FramedVehicleJourneyRefStructure framedJourney = new FramedVehicleJourneyRefStructure();
    DataFrameRefStructure dataFrame = new DataFrameRefStructure();
    dataFrame.setValue(String.format("%1$tY-%1$tm-%1$td", trip.getServiceDate()));
    framedJourney.setDataFrameRef(dataFrame);
    framedJourney.setDatedVehicleJourneyRef(trip.getTripId());
    monitoredVehicleJourney.setFramedVehicleJourneyRef(framedJourney);
    
    // origin/dest.
    List<TripStopTimeBean> stops = trip.getSchedule().getStopTimes();

    JourneyPlaceRefStructure origin = new JourneyPlaceRefStructure();
    origin.setValue(stops.get(0).getStop().getId());
    monitoredVehicleJourney.setOriginRef(origin);
    
    StopBean lastStop = stops.get(stops.size() - 1).getStop();
    DestinationRefStructure dest = new DestinationRefStructure();
    dest.setValue(lastStop.getId());
    monitoredVehicleJourney.setDestinationRef(dest);

    LocationStructure location = new LocationStructure();
    location.setLatitude(new BigDecimal(trip.getStatus().getLocation().getLat()));
    location.setLongitude(new BigDecimal(trip.getStatus().getLocation().getLon()));    
    monitoredVehicleJourney.setVehicleLocation(location);
    
    // monitored calls
    boolean deviated = trip.getStatus().getStatus().toLowerCase().equals("deviated");
    if (monitoredCallStopBean != null && !deviated) {
      List<TripStopTimeBean> stopTimes = trip.getSchedule().getStopTimes();

      MonitoredCallStructure monitoredCall = SiriUtils.getMonitoredCall(stopTimes, 
          monitoredCallStopBean, trip.getStatus());
      
      if(monitoredCall == null)
        return null;
      
      monitoredVehicleJourney.setMonitoredCall(monitoredCall);
    }
    
    // onward calls
    if (includeOnwardCalls && !deviated) {
      List<TripStopTimeBean> stopTimes = trip.getSchedule().getStopTimes();
      
      monitoredVehicleJourney.setOnwardCalls(getOnwardCalls(stopTimes, trip.getStatus()));
    }
    
    return monitoredVehicleJourney;
  }

  public static ProgressRateEnumeration getProgressRateForPhaseAndStatus(String status, String phase) {
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
  
  // NB: this means the vehicle is at *any* terminal, not necessarily a terminal
  // that is the head of any trip.
  public static Boolean isAtTerminal(TripStatusBean statusBean) {
    if(statusBean != null) {
      String phase = statusBean.getPhase();

      if (phase != null &&
          (phase.toUpperCase().equals("LAYOVER_DURING") ||
              phase.toUpperCase().equals("LAYOVER_BEFORE"))) {
        return true;
      } else
        return false;
    }

    return null;
  }
}
