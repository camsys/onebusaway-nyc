/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.web.actions.api.where;

import java.util.Date;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;


@RestController
@RequestMapping("/where/trip-for-vehicle/{vehicleId}")
public class TripForVehicleController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;


  public TripForVehicleController() {
    super(V2);
  }


  @GetMapping
  public ResponseBean show(@PathVariable("vehicleId") String id,
                           @RequestParam(name ="Time", required = false, defaultValue = "") Long timeLong,
                           @RequestParam(name ="IncludeTrip", required = false, defaultValue = "true") boolean includeTrip,
                           @RequestParam(name ="IncludeTrip", required = false, defaultValue = "true") boolean includeSchedule,
                           @RequestParam(name ="IncludeStatus", required = false, defaultValue = "false")boolean includeStatus) throws ServiceException {
// todo: time should be handled like a new date
    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    if (hasErrors())
      return getValidationErrorsResponseBean();
    Date time = new Date(timeLong);
    TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setVehicleId(id);
    query.setTime(time);
    
    TripDetailsInclusionBean inclusion = query.getInclusion();
    inclusion.setIncludeTripBean(includeTrip);
    inclusion.setIncludeTripSchedule(includeSchedule);
    inclusion.setIncludeTripStatus(includeStatus);

    TripDetailsBean trip = _service.getTripDetailsForVehicleAndTime(query);

    if (trip == null)
      return getResourceNotFoundResponseBean();

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    EntryWithReferencesBean<TripDetailsV2Bean> response = factory.getResponse(trip);
    return getOkResponseBean(response);
  }
}
