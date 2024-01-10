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

import java.util.Date;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.conversion.FieldErrorMessage;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureForStopQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/arrival-and-departure-for-stop/{id}")
public class ArrivalAndDepartureForStopResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private ArrivalAndDepartureForStopQueryBean _query = new ArrivalAndDepartureForStopQueryBean();

  public ArrivalAndDepartureForStopResource() {
    super(V2);
  }

  @FieldErrorMessage(Messages.MISSING_REQUIRED_FIELD)
  @PathParam("id")
  public void setId(String id) {
    super.ifMeaningfulValue(_query::setStopId, id);
  }

  public String getId() {
    return _query.getStopId();
  }

  @FieldErrorMessage(Messages.MISSING_REQUIRED_FIELD)
  @QueryParam("TripId")
  public void setTripId(String tripId) {
    super.ifMeaningfulValue(_query::setTripId, tripId);
  }

  public String getTripId() {
    return _query.getTripId();
  }

  @FieldErrorMessage(Messages.MISSING_REQUIRED_FIELD)
  @QueryParam("Date")
  public void setServiceDate(Date date) {
    super.ifMeaningfulValue(_query::setServiceDate, date);
  }

  public Date getServiceDate() {
    if (_query.getServiceDate() == 0)
      return null;
    return new Date(_query.getServiceDate());
  }

  @QueryParam("VehicleId")
  public void setVehicleId(String vehicleId) {
    super.ifMeaningfulValue(_query::setVehicleId, vehicleId);
  }

  public String getVehicleId() {
    return _query.getVehicleId();
  }

  @QueryParam("StopSequence")
  public void setStopSequence(int stopSequence) {
    super.ifMeaningfulValue(_query::setStopSequence, stopSequence);
  }

  public int getStopSequence() {
    return _query.getStopSequence();
  }

  @QueryParam("Time")
  public void setTime(Date time) {
    super.ifMeaningfulValue(_query::setTime, time);
  }

  @GET
  public Response show() throws ServiceException {

    if (hasErrors())
      return getValidationErrorsResponse();

    if (_query.getTime() == 0)
      _query.setTime(System.currentTimeMillis());

    ArrivalAndDepartureBean result = _service.getArrivalAndDepartureForStop(_query);

    if (result == null)
      return getResourceNotFoundResponse();

    if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponse(factory.getResponse(result));
    } else {
      return getUnknownVersionResponse();
    }
  }
}
