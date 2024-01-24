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
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForAgencyQueryBean;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;

@RestController
@RequestMapping("/where/trips-for-agency/{agencyId}")
public class TripsForAgencyController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  public TripsForAgencyController() {
    super(V2);
  }

  @GetMapping
  public ResponseBean show(@PathVariable("agencyId") String id,
                           @RequestParam(name ="Time",required = false) Long time,
                           @RequestParam(name ="MaxCount",required = false, defaultValue = "-1") Long maxCount,
                           @RequestParam(name ="IncludeTrip",required = false) boolean includeTrip,
                           @RequestParam(name ="IncludeStatus",required = false, defaultValue = "false") boolean includeStatus,
                           @RequestParam(name ="IncludeSchedule",required = false, defaultValue = "false")boolean includeSchedule) throws IOException, ServiceException {

    MaxCountSupport _maxCount = createMaxCountFromArg(maxCount);

    time = longToTime(time);

    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    if (hasErrors())
      return getValidationErrorsResponseBean();

    TripsForAgencyQueryBean query = new TripsForAgencyQueryBean();
    query.setAgencyId(id);
    query.setTime(time);
    query.setMaxCount(_maxCount.getMaxCount());

    TripDetailsInclusionBean inclusion = query.getInclusion();
    inclusion.setIncludeTripBean(includeTrip);
    inclusion.setIncludeTripStatus(includeStatus);
    inclusion.setIncludeTripSchedule(includeSchedule);

    BeanFactoryV2 factory = getBeanFactoryV2(_service);

    try {
      ListBean<TripDetailsBean> trips = _service.getTripsForAgency(query);
      return getOkResponseBean(factory.getTripDetailsResponse(trips));
    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponseBean(factory.getEmptyList(TripDetailsV2Bean.class, true));
    }
  }
}