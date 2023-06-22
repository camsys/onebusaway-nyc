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

package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.util.AgencyAndIdLibrary;


import java.util.List;
import java.util.stream.Collectors;

/**
 * A stop on a route, the route being the top-level search result.
 * @author jmaki
 *
 */
public class StopOnRoute {

  private StopBean stop;
  
  private List<String> distanceAways;

  private List<String> vehicleIds; // in approximate order with distanceAways;

  private List<Boolean> realtimes;

  private List<Boolean> strollers;

  private Boolean hasRealtime = true;

  private Boolean isStroller = false;

  List<VehicleRealtimeStopDistance> vehicleRealtimeStopDistances = null;

  public StopOnRoute(StopBean stop, List<VehicleRealtimeStopDistance> vehicleRealtimeStopDistances) {
    this.stop = stop;
    this.vehicleRealtimeStopDistances = vehicleRealtimeStopDistances;
    if(vehicleRealtimeStopDistances == null) return;
    this.distanceAways = vehicleRealtimeStopDistances.stream().map(v->v.getDistanceAway()).collect(Collectors.toList());
    this.vehicleIds = vehicleRealtimeStopDistances.stream().map(v->v.getVehicleId()).collect(Collectors.toList());
    this.realtimes = vehicleRealtimeStopDistances.stream().map(v->v.getHasRealtime()).collect(Collectors.toList());
    this.strollers = vehicleRealtimeStopDistances.stream().map(v->v.getIsStroller()).collect(Collectors.toList());
    hasRealtime=realtimes.isEmpty() ? false:realtimes.get(0);
  }
  
  public String getId() {
    return stop.getId();
  }
  
  public String getIdWithoutAgency() {
    return AgencyAndIdLibrary.convertFromString(getId()).getId();
  }
  
  public String getName() {
    return stop.getName();
  }
  
  public List<String> getDistanceAways() {
    return distanceAways;
  }

  //todo:this may not be stroller
  public Boolean getIsStroller() {
    return true;
  }

  public Boolean getHasRealtime() {
    return hasRealtime;
  }

  public void setHasRealtime(Boolean hasRealtime) {
    this.hasRealtime = hasRealtime;
  }

  public List<String> getVehicleIds() { return vehicleIds; }

  public List<Boolean> getStrollers() {return strollers;}

  public List<Boolean> getRealtimes() {
    return realtimes;
  }

  public Boolean getIsStroller(int i) {
    if(strollers!=null & i>-1 & i < strollers.size() -1){
      return strollers.get(i);
    }
    return false;
  }

  public List<VehicleRealtimeStopDistance> getVehicleRealtimeStopDistances() {
    return vehicleRealtimeStopDistances;
  }
}
