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
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/vehicle-location-records")
public class VehicleLocationRecordsResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private VehicleLocationRecordQueryBean _query = new VehicleLocationRecordQueryBean();

  private String _vehicleId;

  public VehicleLocationRecordsResource() {
    super(V2);
  }

  @QueryParam("BlockId")
  public void setBlockId(String blockId) {
    super.ifMeaningfulValue(_query::setBlockId, blockId);
  }

  @QueryParam("TripId")
  public void setTripId(String tripId) {
    super.ifMeaningfulValue(_query::setTripId, tripId);
  }

  @QueryParam("VehicleId")
  public void setVehicleId(String vehicleId) {
    super.ifMeaningfulValue(_query::setVehicleId, vehicleId);
  }

  @QueryParam("ServiceDate")
  public void setServiceDate(long serviceDate) {
    _query.setServiceDate(serviceDate);
  }

  @QueryParam("FromTime")
  public void setFromTime(Date fromTime) {
    super.ifMeaningfulValue(_query::setFromTime, fromTime);
  }

  @QueryParam("ToTime")
  public void setToTime(Date toTime) {
    super.ifMeaningfulValue(_query::setToTime, toTime);
  }

  @GET
  public Response index() throws IOException, ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    if (hasErrors())
      return getValidationErrorsResponse();

    BeanFactoryV2 factory = getBeanFactoryV2();

    try {
      ListBean<VehicleLocationRecordBean> vehicles = _service.getVehicleLocationRecords(_query);
      return getOkResponse(factory.getVehicleLocationRecordResponse(vehicles));
    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponse(factory.getEmptyList(VehicleStatusV2Bean.class, true));
    }
  }
}
