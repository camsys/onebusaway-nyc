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
import org.onebusaway.api.web.mapping.formatting.FieldErrorMessage;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.problems.EProblemReportStatus;
import org.onebusaway.transit_data.model.problems.StopProblemReportBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/report-problem-with-stop-resource")
public class ReportProblemWithStopResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService _service;

  private StopProblemReportBean _model = new StopProblemReportBean();

  public ReportProblemWithStopResource() {
    super(2);
  }

  @FieldErrorMessage("requiredField.stopId")
  @QueryParam("StopId")
  public void setStopId(String stopId) {
    _model.setStopId(stopId);
  }
  
  public String getStopId() {
    return _model.getStopId();
  }

  @QueryParam("Data")
  public void setData(String data) {
    _model.setData(data);
  }

  @QueryParam("UserComment")
  public void setUserComment(String userComment) {
    _model.setUserComment(userComment);
  }

  @QueryParam("UserLat")
  public void setUserLat(double userLat) {
    _model.setUserLat(userLat);
  }

  @QueryParam("UserLon")
  public void setUserLon(double userLon) {
    _model.setUserLon(userLon);
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
    _service.reportProblemWithStop(_model);

    return getOkResponse(new Object());
  }
}
