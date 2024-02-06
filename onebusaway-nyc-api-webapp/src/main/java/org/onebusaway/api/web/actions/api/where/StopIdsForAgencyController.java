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
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.ResponseBean;
@RestController
@RequestMapping("/where/stop-ids-for-agency/{agencyId}")
public class StopIdsForAgencyController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  public StopIdsForAgencyController() {
    super(V2);
  }

  @GetMapping
  public ResponseBean show(@PathVariable("agencyId") String id) {

    if( ! isVersion(V2))
      return getUnknownVersionResponseBean();
    
    ListBean<String> stopIds = _service.getStopIdsForAgencyId(id);
    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    factory.filterNonRevenueStopIds(id, stopIds.getList());
    return getOkResponseBean(factory.getEntityIdsResponse(stopIds));
  }
}
