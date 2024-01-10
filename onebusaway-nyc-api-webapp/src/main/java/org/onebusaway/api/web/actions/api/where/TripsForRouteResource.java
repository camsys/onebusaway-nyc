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
import java.util.Date;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.TripDetailsV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/trips-for-route/{routeId}")
public class TripsForRouteResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  private String _id;

  private long _time = 0;

  private MaxCountSupport _maxCount = new MaxCountSupport();

  private boolean _includeTrip = true;
  
  private boolean _includeStatus = false;

  private boolean _includeSchedule = false;

  public TripsForRouteResource() {
    super(V2);
  }

  @PathParam("routeId")
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

  @QueryParam("MaxCount")
  public void setMaxCount(int maxCount) {
    _maxCount.setMaxCount(maxCount);
  }

  @QueryParam("IncludeTrip")
  public void setIncludeTrip(boolean includeTrip) {
    _includeTrip = includeTrip;
  }

  @QueryParam("IncludeStatus")
  public void setIncludeStatus(boolean includeStatus) {
    _includeStatus = includeStatus;
  }

  @QueryParam("IncludeSchedule")
  public void setIncludeSchedule(boolean includeSchedule) {
    _includeSchedule = includeSchedule;
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

    TripsForRouteQueryBean query = new TripsForRouteQueryBean();
    query.setRouteId(_id);
    query.setTime(time);
    query.setMaxCount(_maxCount.getMaxCount());

    TripDetailsInclusionBean inclusion = query.getInclusion();
    inclusion.setIncludeTripBean(_includeTrip);
    inclusion.setIncludeTripSchedule(_includeSchedule);
    inclusion.setIncludeTripStatus(_includeStatus);

    BeanFactoryV2 factory = getBeanFactoryV2(_service);

    try {
      ListBean<TripDetailsBean> trips = _service.getTripsForRoute(query);
      return getOkResponse(factory.getTripDetailsResponse(trips));
    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponse(factory.getEmptyList(TripDetailsV2Bean.class, true));
    }
  }
}
