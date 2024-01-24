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

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.RouteV2Bean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.onebusaway.api.model.ResponseBean;

@RestController
@RequestMapping("/where/routes-for-agency/{agencyId}")
public class RoutesForAgencyController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  public RoutesForAgencyController() {
    super(V2);
  }


  @GetMapping
  public ResponseBean show(@PathVariable("agencyId") String id) {

    if (hasErrors())
      return getValidationErrorsResponseBean();

    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    ListBean<RouteBean> routes = _service.getRoutesForAgencyId(id);

    BeanFactoryV2 factory = getBeanFactoryV2();
    List<RouteV2Bean> beans = new ArrayList<RouteV2Bean>();
    for (RouteBean route : routes.getList())
      beans.add(factory.getRoute(route));

    return getOkResponseBean(factory.list(beans, false));
  }
}