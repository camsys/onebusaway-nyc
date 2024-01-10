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
import java.sql.Date;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.VehicleStatusV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/vehicle/{vehicleId}")
public class VehicleResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private String _id;

  private long _time = 0;

  public VehicleResource() {
    super(V2);
  }

  @PathParam("vehicleId")
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  @QueryParam("Time")
  public void setTime(Date time) {
    _time = time.getTime();
  }

  @GET
  public Response show() throws IOException, ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    if (hasErrors())
      return getValidationErrorsResponse();

    long time = System.currentTimeMillis();
    if (_time != 0)
      time = _time;

    BeanFactoryV2 factory = getBeanFactoryV2();

    try {
      VehicleStatusBean vehicle = _service.getVehicleForAgency(_id, time);

      if (vehicle == null)
        return getResourceNotFoundResponse();

      return getOkResponse(factory.getVehicleStatusResponse(vehicle));

    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponse(factory.getEmptyList(VehicleStatusV2Bean.class, true));
    }
  }
}
