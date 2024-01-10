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
import org.onebusaway.api.conversion.FieldErrorMessage;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.problems.EProblemReportStatus;
import org.onebusaway.transit_data.model.problems.TripProblemReportBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/report-problem-with-trip-resource")
public class ReportProblemWithTripResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService _service;

  private TripProblemReportBean _model = new TripProblemReportBean();

  public ReportProblemWithTripResource() {
    super(2);
  }

  @FieldErrorMessage("requiredField.tripId")
  @QueryParam("TripId")
  public void setTripId(String tripId) {
    _model.setTripId(tripId);
  }
  
  public String getTripId() {
    return _model.getTripId();
  }

  @QueryParam("ServiceDate")
  public void setServiceDate(long serviceDate) {
    _model.setServiceDate(serviceDate);
  }

  @QueryParam("VehicleId")
  public void setVehicleId(String vehicleId) {
    _model.setVehicleId(vehicleId);
  }

  @QueryParam("StopId")
  public void setStopId(String stopId) {
    _model.setStopId(stopId);
  }

  @QueryParam("Data")
  public void setData(String data) {
    _model.setData(data);
  }

  @QueryParam("Comment")
  public void setUserComment(String comment) {
    _model.setUserComment(comment);
  }

  @QueryParam("OnVehicle")
  public void setUserOnVehicle(boolean onVehicle) {
    _model.setUserOnVehicle(onVehicle);
  }

  @QueryParam("VehicleNumber")
  public void setUserVehicleNumber(String vehicleNumber) {
    _model.setUserVehicleNumber(vehicleNumber);
  }

  @QueryParam("Lat")
  public void setUserLat(double lat) {
    _model.setUserLat(lat);
  }

  @QueryParam("Lon")
  public void setUserLon(double lon) {
    _model.setUserLon(lon);
  }

  @QueryParam("UserLocationAccuracy")
  public void setUserLocationAccuracy(double userLocationAccuracy) {
    _model.setUserLocationAccuracy(userLocationAccuracy);
  }

  @GET
  public Response create() throws IOException, ServiceException {
    return index();    
  }

  public Response index() throws IOException, ServiceException {

    if (hasErrors())
      return getValidationErrorsResponse();

    _model.setTime(System.currentTimeMillis());
    _model.setStatus(EProblemReportStatus.NEW);
    _service.reportProblemWithTrip(_model);

    return getOkResponse(new Object());
  }
}
