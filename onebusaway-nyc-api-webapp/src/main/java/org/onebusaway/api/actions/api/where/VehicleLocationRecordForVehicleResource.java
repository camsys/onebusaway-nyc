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

import java.io.IOException;
import java.sql.Date;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/vehicle-location-record-for-vehicle/{id}")
public class VehicleLocationRecordForVehicleResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private String _id;

  private long _time = System.currentTimeMillis();

  public VehicleLocationRecordForVehicleResource() {
    super(V2);
  }

  @PathParam("id")
  public void setId(String id) {
    _id = id;
  }

  @QueryParam("Time")
  public void setTime(Date time) {
    if(time!=null)
      _time = time.getTime();
  }

  @GET
  public Response show() throws IOException, ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    if (hasErrors())
      return getValidationErrorsResponse();

    BeanFactoryV2 factory = getBeanFactoryV2();

    VehicleLocationRecordBean record = _service.getVehicleLocationRecordForVehicleId(
        _id, _time);
    if (record == null)
      return getResourceNotFoundResponse();
    return getOkResponse(factory.entry(factory.getVehicleLocationRecord(record)));
  }
}
