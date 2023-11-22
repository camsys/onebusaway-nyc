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

import java.util.Date;

import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.springframework.beans.factory.annotation.Autowired;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/trip-for-vehicle/{vehicleId}")
public class TripForVehicleResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  private String _id;

  private Date _time = new Date();
  
  private boolean _includeTrip = false;

  private boolean _includeSchedule = false;
  
  private boolean _includeStatus = true;

  public TripForVehicleResource() {
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
    _time = time;
  }

  @QueryParam("IncludeTrip")
  public void setIncludeTrip(boolean includeTrip) {
    _includeTrip = includeTrip;
  }

  @QueryParam("IncludeTrip")
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
    
    TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setVehicleId(_id);
    query.setTime(_time);
    
    TripDetailsInclusionBean inclusion = query.getInclusion();
    inclusion.setIncludeTripBean(_includeTrip);
    inclusion.setIncludeTripSchedule(_includeSchedule);
    inclusion.setIncludeTripStatus(_includeStatus);

    TripDetailsBean trip = _service.getTripDetailsForVehicleAndTime(query);

    if (trip == null)
      return getResourceNotFoundResponse();

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    EntryWithReferencesBean<TripDetailsV2Bean> response = factory.getResponse(trip);
    return getOkResponse(response);
  }
}
