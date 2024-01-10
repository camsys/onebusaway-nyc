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
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;

import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/trip-details/{tripId}")
public class TripDetailsResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  private String _id;

  private Date _serviceDate;

  private Date _time = new Date();
  
  private String _vehicleId;
  
  private boolean _includeTrip = true;

  private boolean _includeSchedule = true;
  
  private boolean _includeStatus = true;

  public TripDetailsResource() {
    super(V2);
  }

  @PathParam("tripId")
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  @QueryParam("Date")
  public void setServiceDate(Date date) {
    _serviceDate = date;
  }

  @QueryParam("Time")
  public void setTime(Date time) {
    _time = time;
  }

  @QueryParam("VehicleId")
  public void setVehicleId(String vehicleId) {
    _vehicleId = vehicleId;
  }

  @QueryParam("IncludeTrip")
  public void setIncludeTrip(boolean includeTrip) {
    _includeTrip = includeTrip;
  }

  @QueryParam("IncludeSchedule")
  public void setIncludeSchedule(boolean includeSchedule) {
    _includeSchedule = includeSchedule;
  }

  @QueryParam("IncludeStatus")
  public void setIncludeStatus(boolean includeStatus) {
    _includeStatus = includeStatus;
  }

  @GET
  public Response show() throws ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    if (hasErrors())
      return getValidationErrorsResponse();
    
    TripDetailsQueryBean query = new TripDetailsQueryBean();
    query.setTripId(_id);
    if( _serviceDate != null)
      query.setServiceDate(_serviceDate.getTime());
      query.setTime(_time.getTime());
      query.setVehicleId(_vehicleId);
    
      TripDetailsInclusionBean inclusion = query.getInclusion();
      inclusion.setIncludeTripBean(_includeTrip);
      inclusion.setIncludeTripSchedule(_includeSchedule);
      inclusion.setIncludeTripStatus(_includeStatus);

      TripDetailsBean tripDetails = _service.getSingleTripDetails(query);

    if (tripDetails == null)
      return getResourceNotFoundResponse();

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    EntryWithReferencesBean<TripDetailsV2Bean> response = factory.getResponse(tripDetails);
    return getOkResponse(response);
  }

}
