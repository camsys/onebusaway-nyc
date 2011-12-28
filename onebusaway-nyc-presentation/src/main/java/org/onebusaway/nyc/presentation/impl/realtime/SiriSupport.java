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
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.collections.CollectionUtils;

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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public final class SiriSupport {
  
  public static void fillMonitoredVehicleJourney(MonitoredVehicleJourneyStructure monitoredVehicleJourney, 
      TripBean tripBean, TripDetailsBean tripDetails, StopBean monitoredCallStopBean,
      PresentationService presentationService, TransitDataService transitDataService, long time,
      int maximumOnwardCalls) {

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

    if (presentationService.isInLayover(tripDetails.getStatus())) {
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

    // location
    LocationStructure location = new LocationStructure();

    // if vehicle is detected to be on detour, use actual lat/lon, not snapped location.
    if (presentationService.isOnDetour(tripDetails.getStatus())) {
      location.setLatitude(new BigDecimal(tripDetails.getStatus().getLastKnownLocation().getLat()));
      location.setLongitude(new BigDecimal(tripDetails.getStatus().getLastKnownLocation().getLon()));
    } else {
      location.setLatitude(new BigDecimal(tripDetails.getStatus().getLocation().getLat()));
      location.setLongitude(new BigDecimal(tripDetails.getStatus().getLocation().getLon()));
    }

    monitoredVehicleJourney.setVehicleLocation(location);

    // situation refs
    addSituations(monitoredVehicleJourney, tripDetails);

    // onward and monitored calls:
    List<TripStopTimeBean> stopTimes = tripDetails.getSchedule().getStopTimes();
    
    // sort stop times--important!!
    Collections.sort(stopTimes, new Comparator<TripStopTimeBean>() {
      public int compare(TripStopTimeBean arg0, TripStopTimeBean arg1) {
        return (int) (arg0.getDistanceAlongTrip() - arg1.getDistanceAlongTrip());
      }
    });

    int i = 0;
    int o = 0;

    boolean afterStart = false;
    boolean afterStop = (tripDetails.getStatus().getNextStop() == null);
    
    double distance = tripDetails.getStatus().getDistanceAlongTrip();
    HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();

    for (TripStopTimeBean stopTime : stopTimes) {
      if (stopTime.getDistanceAlongTrip() >= distance) {
        afterStart = true;
      }
      
      // we're now hitting stops after the bus' location...
      if (afterStart) {
        i++;
        
        StopBean stopBean = stopTime.getStop();
        int visitNumber = getVisitNumber(visitNumberForStopMap, stopBean);

        // set monitored call if we found the stop we're "monitoring"
        if(stopBean.getId().equals(monitoredCallStopBean.getId())) {          

          if (monitoredCallStopBean != null && !presentationService.isOnDetour(tripDetails.getStatus())) {
            monitoredVehicleJourney.setMonitoredCall(
                getMonitoredCallStructure(stopBean, stopTime, presentationService, distance, visitNumber, i - 1));
          }

          if(maximumOnwardCalls > 0 && !presentationService.isOnDetour(tripDetails.getStatus())) {
            if(monitoredVehicleJourney.getOnwardCalls() == null) {
              monitoredVehicleJourney.setOnwardCalls(new OnwardCallsStructure());
            }
            
            afterStop = true;
            continue;
          } else {
            break;
          }
        }

        // and the stops after the monitored call, if requested and we're not detoured
        if(afterStop) {
          if(o >= maximumOnwardCalls) {
            break;
          }
          
          monitoredVehicleJourney.getOnwardCalls().getOnwardCall().add(
              getOnwardCallStructure(stopBean, stopTime, presentationService, distance, visitNumber, i - 1));

          o++;
        }
      }
    }    

    return;
  }

  private static OnwardCallStructure getOnwardCallStructure(StopBean stopBean, TripStopTimeBean stopTime, 
      PresentationService presentationService, double distance, int visitNumber, int index) {
    
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

    distances.setStopsFromCall(index);
    distances.setCallDistanceAlongRoute(stopTime.getDistanceAlongTrip());
    distances.setDistanceFromCall(stopTime.getDistanceAlongTrip() - distance);
    distances.setPresentableDistance(presentationService.getPresentableDistance(distances));

    wrapper.setDistances(distances);
    distancesExtensions.setAny(wrapper);    
    onwardCallStructure.setExtensions(distancesExtensions);

    return onwardCallStructure;
  }
  
  private static MonitoredCallStructure getMonitoredCallStructure(StopBean stopBean, TripStopTimeBean stopTime, 
      PresentationService presentationService, double distance, int visitNumber, int index) {
    
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

    distances.setStopsFromCall(index);
    distances.setCallDistanceAlongRoute(stopTime.getDistanceAlongTrip());
    distances.setDistanceFromCall(stopTime.getDistanceAlongTrip() - distance);
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
  
  private static void addSituations(MonitoredVehicleJourneyStructure monitoredVehicleJourney, TripDetailsBean trip) {
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
