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

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.TripV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.onebusaway.api.model.ResponseBean;

@RestController
@RequestMapping("/where/trip/{tripId}")
public class TripController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  public TripController() {
    super(V2);
  }

  @GetMapping
  public ResponseBean show(@PathVariable("tripId") String id) throws ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    TripBean trip = _service.getTrip(id);

    if (trip == null)
      return getResourceNotFoundResponseBean();

    BeanFactoryV2 factory = getBeanFactoryV2();
    EntryWithReferencesBean<TripV2Bean> response = factory.getResponse(trip);
    return getOkResponseBean(response);
  }
}
