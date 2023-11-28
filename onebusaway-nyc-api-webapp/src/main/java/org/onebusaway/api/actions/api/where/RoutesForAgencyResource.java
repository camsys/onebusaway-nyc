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
package org.onebusaway.api.actions.api.where;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.RouteV2Bean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/where/routes-for-agency/{agencyId}")
public class RoutesForAgencyResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private String _id;

  public RoutesForAgencyResource() {
    super(V2);
  }

  @PathParam("agencyId")
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  @GET
  public Response show() {

    if (hasErrors())
      return getValidationErrorsResponse();

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    ListBean<RouteBean> routes = _service.getRoutesForAgencyId(_id);

    BeanFactoryV2 factory = getBeanFactoryV2();
    List<RouteV2Bean> beans = new ArrayList<RouteV2Bean>();
    for (RouteBean route : routes.getList())
      beans.add(factory.getRoute(route));

    return getOkResponse(factory.list(beans, false));
  }
}
