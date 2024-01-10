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
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/where/route/{routeId}")
public class RouteResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private String _id;

  public RouteResource() {
    super(V1);
  }

  @PathParam("routeId")
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  @GET
  public Response show() throws ServiceException {

    if (hasErrors())
      return getValidationErrorsResponse();

    RouteBean route = _service.getRouteForId(_id);

    if (route == null)
      return getResourceNotFoundResponse();

    if (isVersion(V1)) {
      return getOkResponse(route);
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponse(factory.getResponse(route));
    } else {
      return getUnknownVersionResponse();
    }
  }
}
