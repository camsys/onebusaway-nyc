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

package org.onebusaway.nyc.presentation.impl.realtime;

import java.util.*;

import org.onebusaway.nyc.presentation.impl.realtime.siri.OnwardCallsMode;
import org.onebusaway.nyc.presentation.service.realtime.PredictionsSupportService;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.realtime.siri.SiriMonitoredVehicleJourneyBuilderService;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.siri.support.SiriJsonSerializer;
import org.onebusaway.nyc.siri.support.SiriXmlSerializer;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

/**
 * A source of SIRI classes containing real time data, subject to the conventions expressed
 * in the PresentationService.
 * 
 * @author jmaki
 *
 */
@Component("NycRealtimeService")
public class RealtimeServiceImpl implements RealtimeService {
  private static Logger _log = LoggerFactory.getLogger(RealtimeServiceImpl.class);

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  private ConfigurationService _configurationService;

  private PresentationService _presentationService;

  @Autowired
  private SiriMonitoredVehicleJourneyBuilderService _siriMvjBuilderService;

  private PredictionsSupportService _predictionsSupportService;
  
  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();

  private SiriJsonSerializer _siriJsonSerializer = new SiriJsonSerializer();

  private Long _now = null;
  
  @Override
  public void setTime(long time) {
    _now = time;
    _presentationService.setTime(time);
  }

  public long getTime() {
    if(_now != null)
      return _now;
    else
      return System.currentTimeMillis();
  }

  @Autowired
  @Qualifier("NycPresentationService")
  public void setPresentationService(PresentationService presentationService) {
    _presentationService = presentationService;
  }

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService){
    _configurationService = configurationService;
  }

  @Autowired
  public void setPredictionsSupportService(PredictionsSupportService predictionsSupportService){
    _predictionsSupportService = predictionsSupportService;
  }
  
  @Override
  public PresentationService getPresentationService() {
    return _presentationService;
  }  

  @Override
  public SiriJsonSerializer getSiriJsonSerializer() {
    return _siriJsonSerializer;
  }
  
  @Override
  public SiriXmlSerializer getSiriXmlSerializer() {
    return _siriXmlSerializer;
  }

  /**
   * SIRI METHODS
   */
  @Override
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, String directionId,
                                                                   int maximumOnwardCalls, long currentTime,
                                                                   boolean showApc, boolean showRawApc) {
    List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();
    
    ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId, currentTime);
    for(TripDetailsBean tripDetails : trips.getList()) {
      // filter out interlined routes
      if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
        continue;

      // filtered out by user
      if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
        continue;
      
      if(!_presentationService.include(tripDetails.getStatus()))
        continue;
        
      VehicleActivityStructure activity = new VehicleActivityStructure();
      activity.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap = null;
      if(_presentationService.useTimePredictionsIfAvailable()) {
        stopIdToPredictionRecordMap = _predictionsSupportService.getStopIdToPredictionRecordMap(tripDetails.getStatus());
      }

      MonitoredVehicleJourney monitoredVehicleJourney = _siriMvjBuilderService.makeMonitoredVehicleJourney(
              tripDetails.getTrip(), tripDetails.getStatus(), null, stopIdToPredictionRecordMap,
              OnwardCallsMode.VEHICLE_MONITORING, maximumOnwardCalls, currentTime, showApc, showRawApc, false);

      activity.setMonitoredVehicleJourney(monitoredVehicleJourney);

      output.add(activity);
    }

    Collections.sort(output, (arg0, arg1) -> {
      try {
        SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
        SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
        return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());
      } catch(Exception e) {
        return -1;
      }
    });
    
    return output;
  }



  @Override
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, int maximumOnwardCalls,
                                                               long currentTime, boolean showApc, boolean showRawApc) {
  
	TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setTime(new Date(currentTime));
    query.setVehicleId(vehicleId);

    TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
    inclusion.setIncludeTripStatus(true);
    inclusion.setIncludeTripBean(true);
    query.setInclusion(inclusion);

    TripDetailsBean tripDetailsForCurrentTrip = _nycTransitDataService.getTripDetailsForVehicleAndTime(query);
    if (tripDetailsForCurrentTrip != null) {
      if(!_presentationService.include(tripDetailsForCurrentTrip.getStatus()))
        return null;

      Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap = null;
      if(_presentationService.useTimePredictionsIfAvailable()) {
        stopIdToPredictionRecordMap = _predictionsSupportService.getStopIdToPredictionRecordMap(tripDetailsForCurrentTrip.getStatus());
      }
      
      VehicleActivityStructure output = new VehicleActivityStructure();
      output.setRecordedAtTime(new Date(tripDetailsForCurrentTrip.getStatus().getLastUpdateTime()));

      MonitoredVehicleJourney monitoredVehicleJourney = _siriMvjBuilderService.makeMonitoredVehicleJourney(
              tripDetailsForCurrentTrip.getTrip(), tripDetailsForCurrentTrip.getStatus(), null, stopIdToPredictionRecordMap,
              OnwardCallsMode.STOP_MONITORING, maximumOnwardCalls, currentTime, showApc, showRawApc, false);

      output.setMonitoredVehicleJourney(monitoredVehicleJourney);

      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, int maximumOnwardCalls,
                                                                         long currentTime, boolean showApc,
                                                                         boolean showRawApc) {
    List<MonitoredStopVisitStructure> output = new ArrayList<>();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId, currentTime)) {
      TripStatusBean statusBeanForCurrentTrip = adBean.getTripStatus();
      TripBean tripBeanForAd = adBean.getTrip();
      final RouteBean routeBean = tripBeanForAd.getRoute();
      boolean isCancelled = false;

      if(statusBeanForCurrentTrip == null) {
        continue;
      }

      if(!_presentationService.include(statusBeanForCurrentTrip) ||
              !_presentationService.include(adBean, statusBeanForCurrentTrip)) {
        continue;
      }

      if(!_nycTransitDataService.stopHasRevenueServiceOnRoute(
              (routeBean.getAgency()!=null?routeBean.getAgency().getId():null),
              stopId, routeBean.getId(), adBean.getTrip().getDirectionId())) {
        continue;
      }

      Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap = null;
      if(_presentationService.useTimePredictionsIfAvailable()) {
        stopIdToPredictionRecordMap = _predictionsSupportService.getStopIdToPredictionRecordMap(statusBeanForCurrentTrip);
      }

      MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();

      stopVisit.setRecordedAtTime(new Date(statusBeanForCurrentTrip.getLastUpdateTime()));

      MonitoredVehicleJourneyStructure monitoredVehicleJourney = _siriMvjBuilderService.makeMonitoredVehicleJourneyStructure(
              tripBeanForAd, statusBeanForCurrentTrip, adBean.getStop(), stopIdToPredictionRecordMap,
              OnwardCallsMode.STOP_MONITORING, maximumOnwardCalls, currentTime, showApc, showRawApc, isCancelled);

      stopVisit.setMonitoredVehicleJourney(monitoredVehicleJourney);

      output.add(stopVisit);
    }

    Collections.sort(output, (arg0, arg1) -> {
      try {
        if(arg0.getMonitoredVehicleJourney().getMonitoredCall().getExpectedArrivalTime() != null
                && arg1.getMonitoredVehicleJourney().getMonitoredCall().getExpectedArrivalTime() != null){
          return arg0.getMonitoredVehicleJourney().getMonitoredCall().getExpectedArrivalTime().compareTo(
                  arg1.getMonitoredVehicleJourney().getMonitoredCall().getExpectedArrivalTime());
        }
        SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
        SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
        return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());
      } catch(Exception e) {
        return -1;
      }
    });
    
    
    return output;
  }

  /**
   * CURRENT IN-SERVICE VEHICLE STATUS FOR ROUTE
   */

  /**
   * Returns true if there are vehicles in service for given route+direction
   */
  @Override
  public boolean getVehiclesInServiceForRoute(String routeId, String directionId, long currentTime) {
	  ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId, currentTime);
	  for(TripDetailsBean tripDetails : trips.getList()) {
		  // filter out interlined routes
		  if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  // filtered out by user
		  if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
			  continue;

		  if(!_presentationService.include(tripDetails.getStatus()))
			  continue;

		  return true;
	  } 

	  return false;
  }

  /**
   * Returns true if there are vehicles in service for given route+direction that will stop
   * at the indicated stop in the future.
   */
  @Override
  public boolean getVehiclesInServiceForStopAndRoute(String stopId, String routeId, long currentTime) {
	  for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId, currentTime)) {
		  TripStatusBean statusBean = adBean.getTripStatus();
		  if(!_presentationService.include(statusBean) || !_presentationService.include(adBean, statusBean))
			  continue;

		  // filtered out by user
		  if(routeId != null && !adBean.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  // check for non revenue stops
          if(!_nycTransitDataService.stopHasRevenueServiceOnRoute(
                  statusBean.getActiveTrip().getRoute().getAgency().getId(),stopId, routeId, statusBean.getActiveTrip().getDirectionId())){
              continue;
          }

		  return true;
	  }

	  return false;
  }
  
  /**
   * SERVICE ALERTS METHODS
   */

  @Override
  public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId) {
    return getServiceAlertsForRouteAndDirection(routeId, null); 
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId) {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    query.getAffects().add(affects);

    affects.setRouteId(routeId);
    if (directionId != null) {
      affects.setDirectionId(directionId);
    } else {
      /*
       * TODO
       * The route index is not currently being populated correctly; query by route and direction,
       * and supply both directions if not present
       */
      SituationQueryBean.AffectsBean affects1 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects1);
      affects1.setRouteId(routeId);
      affects1.setDirectionId("0");
      SituationQueryBean.AffectsBean affects2 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects2);
      affects2.setRouteId(routeId);
      affects2.setDirectionId("1");
    }
    
    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsGlobal() {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    
    affects.setAgencyId("__ALL_OPERATORS__");
    query.getAffects().add(affects);

    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }

  @Override
  public boolean showApc(String apiKey){
    if(!useApc()){
      return false;
    }
    String apc = _configurationService.getConfigurationValueAsString("display.validApcKeys", "");
    String[] keys = apc.split("\\s*;\\s*");
    for(String key : keys){
        if(apiKey.equalsIgnoreCase(key.trim()) || key.trim().equals("*")){
            return true;
        }
    }
    return false;
  }

  @Override
  public boolean showApc(){
    if(!useApc()){
      return false;
    }
    String apc = _configurationService.getConfigurationValueAsString("display.validApcKeys", "");
    String[] keys = apc.split("\\s*;\\s*");
    for(String key : keys){
        if(key.trim().equals("*")){
            return true;
        }
    }
    return false;
  }

  @Override
  public boolean showRawApc(String apiKey){
      if(!useApc()){
          return false;
      }
      String apc = _configurationService.getConfigurationValueAsString("display.validRawApcKeys", "");
      String[] keys = apc.split("\\s*;\\s*");
      for(String key : keys){
          if(apiKey.equalsIgnoreCase(key.trim()) || key.trim().equals("*")){
              return true;
          }
      }
      return false;
  }

    @Override
    public boolean showRawApc(){
        if(!useApc()){
            return false;
        }
        String apc = _configurationService.getConfigurationValueAsString("display.validRawApcKeys", "");
        String[] keys = apc.split("\\s*;\\s*");
        for(String key : keys){
            if(key.trim().equals("*")){
                return true;
            }
        }
        return false;
    }

  /**
   * PRIVATE METHODS
   */
  private ListBean<TripDetailsBean> getAllTripsForRoute(String routeId, long currentTime) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeId);
    tripRouteQueryBean.setTime(currentTime);
    
    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _nycTransitDataService.getTripsForRoute(tripRouteQueryBean);
  } 
  
  private List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(String stopId, long currentTime) {
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(currentTime);
    query.setMinutesBefore(5 * 60);
    query.setMinutesAfter(5 * 60);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures =
        _nycTransitDataService.getStopWithArrivalsAndDepartures(stopId, query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }

    private boolean useApc(){
        return _configurationService.getConfigurationValueAsBoolean("tds.useApc", Boolean.FALSE);
    }


}
