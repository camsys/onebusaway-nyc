/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.api;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.api.model.DesktopApiStopSearchResult;
import org.onebusaway.nyc.webapp.actions.api.model.DesktopApiVehicleLocation;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class VehicleLocationWithNextStopsForVehicleIdAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private DesktopApiVehicleLocation _result;
  
  private String _vehicleId;

  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ConfigurationService _configurationService;
  
  public void setVehicleId(String vehicleId) {
    _vehicleId = vehicleId;
  }

  @Override
  public String execute() {
    if(_vehicleId != null) {

      TripForVehicleQueryBean query = new TripForVehicleQueryBean();
      query.setVehicleId(_vehicleId);
      query.setTime(new Date());
      
      TripDetailsBean tripDetails = _transitDataService.getTripDetailsForVehicleAndTime(query);

      TripBean tripBean = tripDetails.getTrip();
      if(tripBean == null)
        return ERROR;
                  
      List<DesktopApiStopSearchResult> nextStops = 
          getNextStopsWithDistanceAways(tripDetails.getStatus(), tripDetails.getSchedule());
      
      _result = new DesktopApiVehicleLocation();
      _result.setTripStatusBean(tripDetails.getStatus());      
      _result.setNextStops(nextStops);
      
      return SUCCESS;
    }
    
    return ERROR;
  }

  private List<DesktopApiStopSearchResult> getNextStopsWithDistanceAways(TripStatusBean statusBean, 
      TripStopTimesBean tripSchedule) {

    List<DesktopApiStopSearchResult> output = new ArrayList<DesktopApiStopSearchResult>();

    int nextStopCount = 0;    
    boolean started = false;
    for(TripStopTimeBean stopTime : tripSchedule.getStopTimes()) {
      StopBean stop = stopTime.getStop();

      DistanceAway distanceAway = null;

      if(stop.equals(statusBean.getNextStop())) {
        started = true;

        Integer stopsAway = 0;
        double metersAway = statusBean.getNextStopDistanceFromVehicle();
        Integer feetAway = (int)(metersAway * 3.2808399);  

        distanceAway = new DistanceAway();
        distanceAway.setConfigurationService(_configurationService);
        distanceAway.setStopsAway(stopsAway);
        distanceAway.setFeetAway(feetAway);
        distanceAway.setStatusBean(statusBean);
        
        DesktopApiStopSearchResult nextStopItem = new DesktopApiStopSearchResult();
        nextStopItem.setStopBean(stop);
        nextStopItem.setDistanceAway(distanceAway);
                
        output.add(nextStopItem);

        nextStopCount++;
      } else if(started) {
        if(nextStopCount > 2)
          break;

        Integer stopsAway = nextStopCount;
        double metersAway = (stopTime.getDistanceAlongTrip() - statusBean.getDistanceAlongTrip());
        Integer feetAway = (int)(metersAway * 3.2808399);  

        distanceAway = new DistanceAway();
        distanceAway.setConfigurationService(_configurationService);
        distanceAway.setStopsAway(stopsAway);
        distanceAway.setFeetAway(feetAway);
        distanceAway.setStatusBean(statusBean);

        DesktopApiStopSearchResult nextStopItem = new DesktopApiStopSearchResult();
        nextStopItem.setStopBean(stop);
        nextStopItem.setDistanceAway(distanceAway);

        output.add(nextStopItem);

        nextStopCount++;
      }      
    }

    return output;
  }
  
  public DesktopApiVehicleLocation getVehicleLocation() {
    return _result;
  }
}
