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

import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/where/trip-details/{tripId}")
public class TripDetailsController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  public TripDetailsController() {
    super(V2);
  }

  @GetMapping
  public ResponseEntity<ResponseBean> show(@PathVariable("tripId") String id,
                                           @RequestParam(name ="ServiceDate", required=false) Date date,
                                           @RequestParam(name ="Time", required = false, defaultValue = "") Long time,
                                           @RequestParam(name ="VehicleId", required = false, defaultValue = "") String vehicleId,
                                           @RequestParam(name ="IncludeTrip", required = false, defaultValue = "true") boolean includeTrip,
                                           @RequestParam(name ="IncludeSchedule", required = false, defaultValue = "true") boolean includeSchedule,
                                           @RequestParam(name ="IncludeStatus", required = false, defaultValue = "true") boolean includeStatus
                           ) throws ServiceException {

//todo:confirm date is handled appropriately if no value is given
    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    time = longToTime(time);
    TripDetailsQueryBean query = new TripDetailsQueryBean();
    query.setTripId(id);
    if( date != null)
      query.setServiceDate(date.getTime());
      query.setTime(time);
      query.setVehicleId(vehicleId);
    
      TripDetailsInclusionBean inclusion = query.getInclusion();
      inclusion.setIncludeTripBean(includeTrip);
      inclusion.setIncludeTripSchedule(includeSchedule);
      inclusion.setIncludeTripStatus(includeStatus);

      TripDetailsBean tripDetails = _service.getSingleTripDetails(query);

    if (tripDetails == null)
      return(getResourceNotFoundResponseBean());
//      throw new ResponseStatusException(HttpStatus.valueOf(ResponseCodes.RESPONSE_RESOURCE_NOT_FOUND),getResourceNotFoundResponseBean().getText());

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    EntryWithReferencesBean<TripDetailsV2Bean> response = factory.getResponse(tripDetails);
    return getOkResponseBean(response);
  }

}
