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
package org.onebusaway.nyc.presentation.model.realtime;

import org.onebusaway.nyc.presentation.impl.WebappSupportLibrary;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public class VehicleResult {
  
  private TripStatusBean statusBean;
  
  public VehicleResult() {}
  
  public void setTripStatusBean(TripStatusBean statusBean) {
    this.statusBean = statusBean;
  }

  public String getRouteId() {
    TripBean activeTrip = statusBean.getActiveTrip();
    
    if(activeTrip != null) {
      RouteBean routeBean = activeTrip.getRoute();
      return routeBean.getId();
    } else {
      return null;
    }
  }

  public String getRouteIdWithoutAgency() {
    WebappSupportLibrary idParser = new WebappSupportLibrary();
    return idParser.parseIdWithoutAgency(getRouteId());
  }
  
  public String getHeadsign() {
    TripBean activeTrip = statusBean.getActiveTrip();
    
    if(activeTrip != null)
      return activeTrip.getTripHeadsign();
    else
      return null;
  }
  
  public String getVehicleId() {
    return statusBean.getVehicleId();
  }
  
  public String getVehicleIdWithoutAgency() {
    WebappSupportLibrary idParser = new WebappSupportLibrary();
    return idParser.parseIdWithoutAgency(getVehicleId());
  }
  
  public Double getLatitude() {
    return statusBean.getLocation().getLat();
  }

  public Double getOrientation() {
    return statusBean.getOrientation();
  }
  
  public Double getLongitude() {
    return statusBean.getLocation().getLon();
  }

}
