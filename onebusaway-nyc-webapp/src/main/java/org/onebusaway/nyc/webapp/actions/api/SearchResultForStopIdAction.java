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
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.SearchModelFactory;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.api.model.DesktopApiRouteDestinationItem;
import org.onebusaway.nyc.webapp.actions.api.model.DesktopApiSearchModelFactory;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchResultForStopIdAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private StopResult _result;
  
  private String _stopId;

  @Autowired
  private StopSearchService _stopSearchService;
  
  @Autowired
  private RealtimeService _realtimeService;

  public void setStopId(String stopId) {
    _stopId = stopId;
  }

  @Override
  public String execute() {
    if(_stopId != null) {
      SearchModelFactory modelFactory = new DesktopApiSearchModelFactory();
      _stopSearchService.setModelFactory(modelFactory);      

      _result = _stopSearchService.makeResultForStopId(_stopId);

      injectRealtimeData(_result);
      
      return SUCCESS;
    }

    return ERROR;
  }
  
  private StopResult injectRealtimeData(StopResult stopSearchResult) {
    for(RouteResult route : stopSearchResult.getRoutesAvailable()) {
      for(RouteDestinationItem _destination : route.getDestinations()) {
        DesktopApiRouteDestinationItem destination = (DesktopApiRouteDestinationItem)_destination;

        // service alerts
        List<NaturalLanguageStringBean> serviceAlerts = 
            _realtimeService.getServiceAlertsForStop(stopSearchResult.getStopId());

        destination.setServiceAlerts(serviceAlerts);

        // distance aways
        List<DistanceAway> distanceAways = 
            _realtimeService.getDistanceAwaysForStopAndDestination(stopSearchResult.getStopId(), destination);
        
        destination.setDistanceAways(distanceAways);
      }
    }    
    
    return stopSearchResult;
  }

  public StopResult getResult() {
    return _result;
  }
}
