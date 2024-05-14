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

import java.io.IOException;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.impl.SearchBoundsFactory;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForBoundsQueryBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/where/trips-for-location")
public class TripsForLocationResponse extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  private static final double MAX_BOUNDS_RADIUS = 20000.0;

  @Autowired
  private NycTransitDataService _service;

  public TripsForLocationResponse() {
    super(V2);
  }


  @GetMapping
  public ResponseEntity<ResponseBean> index(SearchBoundsFactory searchBoundsFactory,
                                            @RequestParam(name ="Date", required = false) long time,
                                            @RequestParam(name ="MaxCount", required = false) Long maxCountArg,
                                            @RequestParam(name ="IncludeTrip", required = false, defaultValue = "true") boolean includeTrip,
                                            @RequestParam(name ="IncludeStatus", required = false, defaultValue = "false") boolean includeStatus,
                                            @RequestParam(name ="IncludeSchedule", required = false, defaultValue = "false") boolean includeSchedule) throws IOException, ServiceException {
    MaxCountSupport maxCount = createMaxCountFromArg(maxCountArg);
    time = longToTime(time);
    if (!isVersion(V2))
      return getUnknownVersionResponseBean();


    if(searchBoundsFactory.getMaxSearchRadius()==0){
      searchBoundsFactory.setMaxSearchRadius(MAX_BOUNDS_RADIUS);
    }

    CoordinateBounds bounds = searchBoundsFactory.createBounds();

    TripsForBoundsQueryBean query = new TripsForBoundsQueryBean();
    query.setBounds(bounds);
    query.setTime(time);
    query.setMaxCount(maxCount.getMaxCount());

    TripDetailsInclusionBean inclusion = query.getInclusion();
    inclusion.setIncludeTripBean(includeTrip);
    inclusion.setIncludeTripSchedule(includeSchedule);
    inclusion.setIncludeTripStatus(includeStatus);

    BeanFactoryV2 factory = getBeanFactoryV2(_service);

    try {
      ListBean<TripDetailsBean> trips = _service.getTripsForBounds(query);
      return getOkResponseBean(factory.getTripDetailsResponse(trips));
    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponseBean(factory.getEmptyList(TripDetailsV2Bean.class, true));
    }
  }
}
