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
package org.onebusaway.nyc.transit_data_federation.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.model.schedule.ServiceHour;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.schedule.ScheduledServiceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
class NycTransitDataServiceImpl implements NycTransitDataService {

  @Autowired
  private BundleManagementService _bundleManagementService;

  @Autowired
  private ScheduledServiceService _scheduledServiceService;

  @Override
  public String getActiveBundleId() {
    BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();
    
    if(currentBundle != null) {
      return currentBundle.getId();
    } else {
      return null;
    }
  }

  @Override
  public Boolean routeHasUpcomingScheduledService(Date time, String _routeId, String directionId)  {
    ServiceHour serviceHour = new ServiceHour(time);
    AgencyAndId routeId = AgencyAndIdLibrary.convertFromString(_routeId);
    
    return _scheduledServiceService.routeHasUpcomingScheduledService(serviceHour, routeId, directionId);
  }

  @Override
  public Boolean stopHasUpcomingScheduledService(Date time, String _stopId, String _routeId, String directionId) {
    ServiceHour serviceHour = new ServiceHour(time);
    AgencyAndId stopId = AgencyAndIdLibrary.convertFromString(_stopId);
    AgencyAndId routeId = AgencyAndIdLibrary.convertFromString(_routeId);
    
    return _scheduledServiceService.stopHasUpcomingScheduledService(serviceHour, stopId, routeId, directionId);
  }
}
