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
package org.onebusaway.nyc.webapp.actions.m;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.impl.search.AbstractSearchResultFactoryImpl;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.m.model.*;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.util.SystemTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.VehicleActivityStructure;

public class SearchResultFactoryImpl extends AbstractSearchResultFactoryImpl implements SearchResultFactory {

  private static Logger _log = LoggerFactory.getLogger(SearchResultFactoryImpl.class);
  
  private ConfigurationService _configurationService;

  private RealtimeService _realtimeService;

  private NycTransitDataService _nycTransitDataService;
  
  private static final String PROGRESS_LAYOVER = "layover";
  private static final String PROGRESS_PREV_TRIP = "prevTrip";

  public SearchResultFactoryImpl(NycTransitDataService nycTransitDataService,
      RealtimeService realtimeService, ConfigurationService configurationService) {
    _nycTransitDataService = nycTransitDataService;
    _realtimeService = realtimeService;
    _configurationService = configurationService;
  }

  @Override
  public SearchResult getRouteResultForRegion(RouteBean routeBean) {
    return new RouteInRegionResult(routeBean);
  }

  @Override
  public SearchResult getRouteResult(RouteBean routeBean) {
    List<RouteDirection> directions = new ArrayList<RouteDirection>();

    StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

    // create stop ID->stop bean map
    Map<String, StopBean> stopIdToStopBeanMap = new HashMap<String, StopBean>();
    for (StopBean stopBean : stopsForRoute.getStops()) {
      stopIdToStopBeanMap.put(stopBean.getId(), stopBean);
    }

    // add stops in both directions
    //Map<String, List<String>> stopIdAndDirectionToDistanceAwayStringMap = getStopIdAndDirectionToDistanceAwayStringsListMapForRoute(routeBean);


    Map<String, List<VehicleRealtimeStopDistance>> stopIdToVehicleRealtimeStopDistanceMap = new HashMap<>();

    fillDistanceVehicleAndRealtime(routeBean, stopIdToVehicleRealtimeStopDistanceMap);

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();

        if (!type.equals("destination"))
          continue;

        // service in this direction
        Boolean hasUpcomingScheduledService = _nycTransitDataService.routeHasUpcomingScheduledService(
        	(routeBean.getAgency()!=null?routeBean.getAgency().getId():null),
            System.currentTimeMillis(), routeBean.getId(),
            stopGroupBean.getId());

        // if there are buses on route, always have "scheduled service"
        Boolean routeHasVehiclesInService =
      		  _realtimeService.getVehiclesInServiceForRoute(routeBean.getId(), stopGroupBean.getId(), System.currentTimeMillis());

        if(routeHasVehiclesInService) {
      	  hasUpcomingScheduledService = true;
        }

        // stops in this direction
        List<StopOnRoute> stopsOnRoute = null;
        if (!stopGroupBean.getStopIds().isEmpty()) {
          stopsOnRoute = new ArrayList<StopOnRoute>();

          for (String stopId : stopGroupBean.getStopIds()) {
        	  if (_nycTransitDataService.stopHasRevenueServiceOnRoute((routeBean.getAgency()!=null?routeBean.getAgency().getId():null),
                      stopId, routeBean.getId(), stopGroupBean.getId())) {
        	    String key = getKey(stopId, stopGroupBean.getId());
                stopsOnRoute.add(new StopOnRoute(stopIdToStopBeanMap.get(stopId),
                        stopIdToVehicleRealtimeStopDistanceMap.get(key)
                ));
            }
          }
        }


        directions.add(new RouteDirection(stopGroupBean.getName().getName(), stopGroupBean, stopsOnRoute,
            hasUpcomingScheduledService, null));
      }
    }

    // service alerts in this direction
    Set<String> serviceAlertDescriptions = new HashSet<>();

    List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRoute(routeBean.getId());
    populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);

    return new RouteResult(routeBean, directions, serviceAlertDescriptions);
  }



  @Override
  // TODO: this method needs refactoring to consider route and direction for routes with loops
  public SearchResult getStopResult(StopBean stopBean, Set<RouteBean> routeFilter) {

      List<RouteAtStop> routesWithArrivals = new ArrayList<>();
      List<RouteAtStop> routesWithNoVehiclesEnRoute = new ArrayList<>();
      List<RouteAtStop> routesWithNoScheduledService = new ArrayList<>();
      List<RouteBean> filteredRoutes = new ArrayList<>();

      Set<String> serviceAlertDescriptions = new HashSet<>();

      for (RouteBean routeBean : stopBean.getRoutes()) {
        if (routeFilter != null && !routeFilter.isEmpty()
                && !routeFilter.contains(routeBean)) {
          filteredRoutes.add(routeBean);
          continue;
        }

        StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

        List<RouteDirection> directions = new ArrayList<>();
        List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();

        for (StopGroupingBean stopGroupingBean : stopGroupings) {
          for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
            NameBean name = stopGroupBean.getName();
            String type = name.getType();

            if (!type.equals("destination"))
              continue;

            // filter out route directions that don't stop at this stop
            if (!stopGroupBean.getStopIds().contains(stopBean.getId()))
              continue;

            // arrivals in this direction
            Map<String, List<StopOnRoute>> arrivalsForRouteAndDirection = getDisplayStringsByHeadsignForStopAndRouteAndDirection(
                    stopBean, routeBean, stopGroupBean);

            // service alerts for this route + direction
            List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(
                    routeBean.getId(), stopGroupBean.getId());
            populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans);

            // service in this direction
            Boolean hasUpcomingScheduledService = _nycTransitDataService.stopHasUpcomingScheduledService(
                    (routeBean.getAgency() != null ? routeBean.getAgency().getId() : null),
                    System.currentTimeMillis(), stopBean.getId(), routeBean.getId(),
                    stopGroupBean.getId());

            // if there are buses on route, always have "scheduled service"
            if (!arrivalsForRouteAndDirection.isEmpty()) {
              hasUpcomingScheduledService = true;
            }

            Map<String, List<Boolean>> hasRealtimes = getHasRealtimesForStopAndRouteAndDirection(stopBean, routeBean, stopGroupBean);

            if (arrivalsForRouteAndDirection.isEmpty()) {
              directions.add(new RouteDirection(stopGroupBean.getName().getName(), stopGroupBean,  Collections.emptyList(),
                      hasUpcomingScheduledService, Collections.emptyList()));
            } else {

              for (Map.Entry<String, List<StopOnRoute>> entry : arrivalsForRouteAndDirection.entrySet()) {
                directions.add(new RouteDirection(entry.getKey(), stopGroupBean, entry.getValue(),
                        hasUpcomingScheduledService, Collections.emptyList(), hasRealtimes.get(entry.getKey())));

              }
            }
          }
        }

        // For each direction, determine whether the route has no service, has no vehicles,
        // or has service with vehicles en route. Add RouteAtStop object to appropriate collection.
        // Now one RouteAtStop object exists for each direction for each route.
        for (RouteDirection direction : directions) {
          List<RouteDirection> directionList = Collections.singletonList(direction);

          RouteAtStop routeAtStop = new RouteAtStop(routeBean, directionList, serviceAlertDescriptions);
          if (!direction.getStops().isEmpty())
            routesWithArrivals.add(routeAtStop);
          else if (Boolean.FALSE.equals(direction.getHasUpcomingScheduledService()))
            routesWithNoScheduledService.add(routeAtStop);
          else
            routesWithNoVehiclesEnRoute.add(routeAtStop);
        }
      }

      return new StopResult(stopBean, routesWithArrivals,
              routesWithNoVehiclesEnRoute, routesWithNoScheduledService, filteredRoutes, serviceAlertDescriptions);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<RouteBean> routeFilter) {
    return new GeocodeResult(geocodeResult);
  }

  // stop view
  private Map<String, List<StopOnRoute>> getDisplayStringsByHeadsignForStopAndRouteAndDirection(
      StopBean stopBean, RouteBean routeBean, StopGroupBean stopGroupBean) {

    Map<String, List<StopOnRoute>> results = new HashMap<String, List<StopOnRoute>>();

    Boolean showApc = _realtimeService.showApc();
    Boolean showRawApc = _realtimeService.showRawApc();

    // stop visits
    List<MonitoredStopVisitStructure> visitList = _realtimeService.getMonitoredStopVisitsForStop(
        stopBean.getId(), 0, System.currentTimeMillis(), showApc, showRawApc, false);

    for (MonitoredStopVisitStructure visit : visitList) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      if (routeId != null && !routeBean.getId().equals(routeId))
        continue;

      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      if (directionId != null && !stopGroupBean.getId().equals(directionId))
        continue;

      // on detour?
      MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
      if (monitoredCall == null)
        continue;

      if (!results.containsKey(visit.getMonitoredVehicleJourney().getDestinationName().getValue()))
        results.put(visit.getMonitoredVehicleJourney().getDestinationName().getValue(), new ArrayList<StopOnRoute>());

      if(results.get(visit.getMonitoredVehicleJourney().getDestinationName().getValue()).size() >= 3)
        continue;
      
      String distance = getPresentableDistance(visit.getMonitoredVehicleJourney(),
    		visit.getRecordedAtTime().getTime(), true);
    	  
      String timePrediction = getPresentableTime(visit.getMonitoredVehicleJourney(),
    	 	visit.getRecordedAtTime().getTime(), true);

      boolean isKneeling = getIsKneeling(visit.getMonitoredVehicleJourney());

      String vehicleId = null;
      if (visit.getMonitoredVehicleJourney() != null && visit.getMonitoredVehicleJourney().getVehicleRef() != null) {
        vehicleId = visit.getMonitoredVehicleJourney().getVehicleRef().getValue();
      } else {
        vehicleId = "N/A"; // insert an empty element so it aligns with distanceAways
      }

      List<VehicleRealtimeStopDistance> vrsdList = new ArrayList<>();
      VehicleRealtimeStopDistance vrsd = new VehicleRealtimeStopDistance();
      vrsdList.add(vrsd);

      if (vehicleId.contains("_")) vehicleId = vehicleId.split("_")[1];
      vrsd.setVehicleId(vehicleId);
      if(timePrediction != null) {
        vrsd.setDistanceAway(timePrediction);
      } else {
        vrsd.setDistanceAway(distance);
      }
      vrsd.setIsKneeling(isKneeling);

      vrsd.setHasRealtime(visit.getMonitoredVehicleJourney().isMonitored());

      results.get(visit.getMonitoredVehicleJourney().getDestinationName().getValue()).add(
              new StopOnRoute(stopBean,vrsdList));

    }

    return results;
  }

  private Map<String,List<Boolean>> getHasRealtimesForStopAndRouteAndDirection(StopBean stopBean, RouteBean routeBean, StopGroupBean stopGroupBean){

    Map<String,List<Boolean>> hasRealtimes = new HashMap<>();
    List<MonitoredStopVisitStructure> visitList = _realtimeService.getMonitoredStopVisitsForStop(
        stopBean.getId(), 0, System.currentTimeMillis(), _realtimeService.showApc(), _realtimeService.showRawApc(), false);

    for (MonitoredStopVisitStructure visit : visitList) {
      String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
      if (!routeBean.getId().equals(routeId))
        continue;

      String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
      if (!stopGroupBean.getId().equals(directionId))
        continue;

      if (!hasRealtimes.containsKey(visit.getMonitoredVehicleJourney().getDestinationName().getValue()))
        hasRealtimes.put(visit.getMonitoredVehicleJourney().getDestinationName().getValue(), new ArrayList<>());

      if(hasRealtimes.get(visit.getMonitoredVehicleJourney().getDestinationName().getValue()).size() >= 3)
        continue;
      try {
        hasRealtimes.get(visit.getMonitoredVehicleJourney().getDestinationName().getValue()).add(!visit.getMonitoredVehicleJourney().getProgressStatus().getValue().equals("spooking"));
      } catch (Exception e) {
        hasRealtimes.get(visit.getMonitoredVehicleJourney().getDestinationName().getValue()).add(true);
      }
    }

    return hasRealtimes;
  }

  // route view
  private Map<String, List<String>> getStopIdAndDirectionToDistanceAwayStringsListMapForRoute(
      RouteBean routeBean) {
    Map<String, List<String>> result = new HashMap<String, List<String>>();

    Boolean showApc = _realtimeService.showApc();
    Boolean showRawApc = _realtimeService.showApc();

      // stop visits
    List<VehicleActivityStructure> journeyList = _realtimeService.getVehicleActivityForRoute(
        routeBean.getId(), null, 0, System.currentTimeMillis(), showApc, showRawApc);

    // build map of stop IDs to list of distance strings
    for (VehicleActivityStructure journey : journeyList) {
      // on detour?
      MonitoredCallStructure monitoredCall = journey.getMonitoredVehicleJourney().getMonitoredCall();
      if (monitoredCall == null) {
        continue;
      }

      String direction = journey.getMonitoredVehicleJourney().getDirectionRef().getValue();
      String stopId = monitoredCall.getStopPointRef().getValue();

      List<String> distanceStrings = result.get(stopId);
      if (distanceStrings == null) {
        distanceStrings = new ArrayList<String>();
      }

      distanceStrings.add(getPresentableDistance(
          journey.getMonitoredVehicleJourney(),
          journey.getRecordedAtTime().getTime(), false));
      result.put(stopId + "_" + direction, distanceStrings);
    }

    return result;
  }

  private String getPresentableTime(
		  MonitoredVehicleJourneyStructure journey, long updateTime,
	      boolean isStopContext) {

	  NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
	  MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
	  
	  if(!isStopContext) {
		  return null;
	  }
	  
	  int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
	  long age = (System.currentTimeMillis() - updateTime) / 1000;

	  if (age > staleTimeout) {
		  return null;
	  }
	  
	  if(monitoredCall.getExpectedArrivalTime() != null) {
		  long predictedArrival = monitoredCall.getExpectedArrivalTime().getTime();

		  SiriExtensionWrapper wrapper = (SiriExtensionWrapper) monitoredCall.getExtensions().getAny();
		  SiriDistanceExtension distanceExtension = wrapper.getDistances();
		  String distance = distanceExtension.getPresentableDistance();
		  //add space to the distance so that occupancy lines up correctly with time [pjm]
		  String distanceBold = " <strong>" + distance + "</strong>";

		  double minutes = Math.floor((predictedArrival - updateTime) / 60 / 1000);
		  String timeString = Math.round(minutes) + " minute" + ((Math.abs(minutes) != 1) ? "s" : "");
		  String timeAndDistance = "<strong>" + timeString + "</strong>," + distance;
		  String loadOccupancy = getPresentableOccupancy(journey, updateTime);
		    
		  Date originDepartureTime = journey.getOriginAimedDepartureTime();


		  String timePrefix = distanceBold;
		  if(Boolean.parseBoolean(_configurationService.getConfigurationValueAsString(
                  "display.showNextTripPredictions", "false"))) {
            timePrefix = timeAndDistance;
          }

		  if(originDepartureTime != null && isLayover(progressStatus)){
			  if(isDepartureOnSchedule(originDepartureTime)){
				  return timePrefix + " (at terminal, scheduled to depart at " +
						  getFormattedOriginAimedDepartureTime(originDepartureTime) + ")";
			  }
			  
			  return timePrefix + " (at terminal)";
		  }
		  else if(originDepartureTime != null && isPrevTrip(progressStatus)) {

			  if(isDepartureOnSchedule(originDepartureTime)) {
		    		return timePrefix + " (+ layover, scheduled to depart terminal at "
		    				+ getFormattedOriginAimedDepartureTime(originDepartureTime) + ")";
		    	}
		    	else{
		    		return timePrefix + " (+ scheduled layover at terminal)";
		    	}
	  	  }
		  else {
			  return timeAndDistance + loadOccupancy;
		  }
	  }
	  
	  return null;
  }

  private String getPresentableOccupancy(MonitoredVehicleJourneyStructure journey, long updateTime){
      // if data is old, no occupancy
      int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
      long age = (System.currentTimeMillis() - updateTime) / 1000;
      if (age > staleTimeout && journey != null && journey.getVehicleRef() != null) {
          System.out.println("tossing record "+ journey.getVehicleRef().getValue()
                  + " with age " + age + "s old");
          return "";
      }

      String apcMode = _configurationService.getConfigurationValueAsString("display.apcMode", "PASSENGERCOUNT");

      String occupancyStr = "";
      if (apcMode != null) {
        switch (apcMode.toUpperCase()) {
          case "NONE":
            occupancyStr = "";
            break;
          case "OCCUPANCY":
            occupancyStr = getApcModeOccupancy(journey);
            break;
          case "LOADFACTOR":
            occupancyStr = getApcModeLoadFactor(journey);
            break;
          case "PASSENGERCOUNT":
            occupancyStr = getApcModePassengerCount(journey);
            break;
          case "LOADFACTORPASSENGERCOUNT":
            occupancyStr = getApcModeLoadFactorPassengerCount(journey);
            break;
        }
      }
      return occupancyStr;
  }

  private String getApcModeOccupancy(MonitoredVehicleJourneyStructure journey) {

      if (journey.getOccupancy() != null) {

          String loadOccupancy = journey.getOccupancy().toString();
          loadOccupancy = loadOccupancy.toUpperCase();
          //TODO: Modify output load text here
          if (loadOccupancy.equals("SEATS_AVAILABLE") || loadOccupancy.equals("MANY_SEATS_AVAILABLE")) {
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcDotG'></span> <span class='apcTextG'>Seats Available</span></div>";
              //loadOccupancy = "<icon class='apcicong'> </icon>";
          } else if (loadOccupancy.equals("FEW_SEATS_AVAILABLE") || loadOccupancy.equals("STANDING_AVAILABLE")) {
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcDotY'></span> <span class='apcTextY'>Limited Seating</span></div>";
              //loadOccupancy = "<icon class='apcicony'> </icon>";
          } else if (loadOccupancy.equals("FULL")) {
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcDotR'></span> <span class='apcTextR'>Standing Room Only</span></div>";
              //loadOccupancy = "<icon class='apciconr'> </icon>";
          }

          return " " + loadOccupancy;
      } else
          return "";
  }

  private String getApcModePassengerCount(MonitoredVehicleJourneyStructure journey) {

      SiriExtensionWrapper wrapper = (SiriExtensionWrapper)journey.getMonitoredCall().getExtensions().getAny();

      if(wrapper.getCapacities() != null && wrapper.getCapacities().getPassengerCount() != null) {

          String loadOccupancy = wrapper.getCapacities().getPassengerCount() + " passengers on vehicle";

          return ", ~" +loadOccupancy;
      }else
          return "";

  }

  private String getApcModeLoadFactor(MonitoredVehicleJourneyStructure journey) {

      SiriExtensionWrapper wrapper = (SiriExtensionWrapper)journey.getMonitoredCall().getExtensions().getAny();

      if(wrapper.getCapacities() != null && wrapper.getCapacities().getPassengerCount() != null &&
              wrapper.getCapacities().getOccupancyLoadFactor() != null) {

          String loadOccupancy = wrapper.getCapacities().getOccupancyLoadFactor();
          loadOccupancy = loadOccupancy.toUpperCase();
          if(loadOccupancy.equals("L")){
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextG'>Low Occupancy</span></div>";
          }
          else if (loadOccupancy.equals("M")){
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextY'>Medium Occupancy</span></div>";
          }
          else if (loadOccupancy.equals("H")){
              loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextR'>High Occupancy</span></div>";
          }

          return " " +loadOccupancy;
      }else
          return "";

  }
  
  private String getApcModeLoadFactorPassengerCount(MonitoredVehicleJourneyStructure journey) {

		SiriExtensionWrapper wrapper = (SiriExtensionWrapper)journey.getMonitoredCall().getExtensions().getAny();
		
		if(wrapper.getCapacities() != null && wrapper.getCapacities().getPassengerCount() != null &&
                wrapper.getCapacities().getOccupancyLoadFactor() != null) {
			
			String loadOccupancy = wrapper.getCapacities().getOccupancyLoadFactor();
			loadOccupancy = loadOccupancy.toUpperCase();
			if(loadOccupancy.equals("L")){
				loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextG'>Low Occupancy </span> <span class='apcTextSmallG'> (" +
                        wrapper.getCapacities().getPassengerCount() + " passengers)</span></div>";
			}
			else if (loadOccupancy.equals("M")){
				loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextY'>Medium Occupancy </span> <span class='apcTextSmallY'> (" +
                        wrapper.getCapacities().getPassengerCount() + " passengers)</span></div>";
			}
			else if (loadOccupancy.equals("H")){
				loadOccupancy = "<div class='apcLadderContainer'><span class='apcTextR'>High Occupancy </span> <span class='apcTextSmallR'> (" +
                        wrapper.getCapacities().getPassengerCount() + " passengers)</span></div>";
			}
			
			return " " +loadOccupancy;
		}else
			return "";

  }

  private void fillDistanceVehicleAndRealtime(RouteBean routeBean,
                                              Map<String, List<VehicleRealtimeStopDistance>> stopIdAndDirToVRSDMap) {

    Boolean showApc = _realtimeService.showApc();
    Boolean showRawApc = _realtimeService.showRawApc();

    List<VehicleActivityStructure> journeyList = _realtimeService.getVehicleActivityForRoute(
            routeBean.getId(), null, 0, SystemTime.currentTimeMillis(), showApc, showRawApc);

    // build map of stop IDs to list of distance strings
    for (VehicleActivityStructure journey : journeyList) {
      // on detour?
      MonitoredCallStructure monitoredCall = journey.getMonitoredVehicleJourney().getMonitoredCall();
      if (monitoredCall == null) {
        continue;
      }

      VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = journey.getMonitoredVehicleJourney();
      Date recordedAtTime = journey.getRecordedAtTime();
      String direction = journey.getMonitoredVehicleJourney().getDirectionRef().getValue();
      String stopId = monitoredCall.getStopPointRef().getValue();

      List<VehicleRealtimeStopDistance> vrsdList = stopIdAndDirToVRSDMap.get(getKey(stopId,direction));
      if(vrsdList==null) {
          vrsdList=new ArrayList<>();
          stopIdAndDirToVRSDMap.put(getKey(stopId,direction),vrsdList);
      }
      VehicleRealtimeStopDistance vehicleAndStopDistance = new VehicleRealtimeStopDistance();
      vehicleAndStopDistance.setDistanceAway(getDistanceAway(monitoredVehicleJourney,recordedAtTime));
      vehicleAndStopDistance.setVehicleId(getVehicleId(monitoredVehicleJourney));
      vehicleAndStopDistance.setHasRealtime(hasRealtimeData(monitoredVehicleJourney));
      vehicleAndStopDistance.setIsKneeling(getIsKneeling(monitoredVehicleJourney));
      vrsdList.add(vehicleAndStopDistance);
    }
  }

  private String getDistanceAway(
      VehicleActivityStructure.MonitoredVehicleJourney mvj,
      Date recordedAtTime) {
    return getPresentableDistance(
        mvj,
        recordedAtTime.getTime(), false);
  }

  private String getVehicleId(
      VehicleActivityStructure.MonitoredVehicleJourney mvj) {
    if (mvj != null && mvj.getVehicleRef() != null) {
      String id = mvj.getVehicleRef().getValue();
      if (id.contains("_")) id = id.split("_")[1];
      return id;
    } else {
      return "N/A";
    }
  }


  private Boolean hasRealtimeData(
      VehicleActivityStructure.MonitoredVehicleJourney mvj) {

    // Realtime Data Check for Stop
    Boolean spooking = false;
    try {
      spooking = mvj.getProgressStatus().getValue() == "spooking";
    } catch (Exception e) {
      //nothing
    }
    return mvj.isMonitored() && !spooking;
  }

  private boolean getIsKneeling(MonitoredVehicleJourneyStructure journey){
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper) monitoredCall.getExtensions().getAny();
    if(wrapper.getFeatures()==null){
      return false;
    }
    return wrapper.getFeatures().getKneelingVehicle();
  }
  
  private String getPresentableDistance(
      MonitoredVehicleJourneyStructure journey, long updateTime,
      boolean isStopContext) {
    MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
    SiriExtensionWrapper wrapper = (SiriExtensionWrapper) monitoredCall.getExtensions().getAny();
    SiriDistanceExtension distanceExtension = wrapper.getDistances();

    String message = "";
    //add space to the distance so that occupancy lines up correctly with time [pjm]
    String distance = " <strong>" + distanceExtension.getPresentableDistance() + "</strong>";
    String loadOccupancy = getPresentableOccupancy(journey, updateTime);
    
    NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
    Date originDepartureTime = journey.getOriginAimedDepartureTime();
    
    if(originDepartureTime != null){
	    // at terminal label only appears in stop results
	    if (isStopContext && isLayover(progressStatus)) {
	    	if(isDepartureOnSchedule(originDepartureTime)) {
	    		message += "at terminal, scheduled to depart at " 
	    				+ getFormattedOriginAimedDepartureTime(originDepartureTime);
	    	}
	    	else{
	        	message += "at terminal";
	    	}
	        
	    } else if (isStopContext && isPrevTrip(progressStatus)) {
	    	if(isDepartureOnSchedule(originDepartureTime)) {
	    		message += "+ layover, scheduled to depart terminal at " 
	    				+ getFormattedOriginAimedDepartureTime(originDepartureTime);
	    	}
	    	else{
	    		message += "+ scheduled layover at terminal";
	    	}
	    }
    }
    	
    int staleTimeout = _configurationService.getConfigurationValueAsInteger(
        "display.staleTimeout", 120);
    long age = (System.currentTimeMillis() - updateTime) / 1000;

    if (age > staleTimeout) {
      if (message.length() > 0) {
        message += ", ";
      }

      message += "old data";
    }

    if (message.length() > 0)
      // here we choose not to show occupancy as it is likely a different trip
      return distance + " (" + message + ") ";
    else
      return distance + loadOccupancy;
  }
  
  private boolean isLayover(NaturalLanguageStringStructure progressStatus){
	  return progressStatus != null && progressStatus.getValue().contains(PROGRESS_LAYOVER);
  }
  
  private boolean isPrevTrip(NaturalLanguageStringStructure progressStatus){
	  return progressStatus != null && progressStatus.getValue().contains(PROGRESS_PREV_TRIP);
  }
  
  private boolean isDepartureOnSchedule(Date departureDate){
	  return departureDate != null && departureDate.getTime() > System.currentTimeMillis();
  }
  
  private String getFormattedOriginAimedDepartureTime(Date originAimedDepartureTime){
	  DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT);
	  return formatter.format(originAimedDepartureTime);
  }

  private String getKey(String... keyParams){
    String key = null;
    for(String keyParam : keyParams){
      if(key != null){
        key += "_";
      }
      key += keyParam;
    }
    return key;
  }
}
