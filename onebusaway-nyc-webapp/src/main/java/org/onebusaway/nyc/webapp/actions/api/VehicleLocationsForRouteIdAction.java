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

import org.onebusaway.nyc.presentation.model.realtime.VehicleResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class VehicleLocationsForRouteIdAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private List<VehicleResult> _results;
  
  private String _routeId;

  @Autowired
  private RealtimeService _realtimeService;

  public void setRouteId(String routeId) {
    _routeId = routeId;
  }

  @Override
  public String execute() {
    if(_routeId != null) {
      _results = _realtimeService.getLocationsForVehiclesServingRoute(_routeId);
      return SUCCESS;
    }
    
    return ERROR;
  }

  public List<VehicleResult> getVehicleLocations() {
    return _results;
  }
}
