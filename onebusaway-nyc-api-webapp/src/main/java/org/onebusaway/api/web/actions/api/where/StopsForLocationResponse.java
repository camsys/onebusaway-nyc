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
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.StopV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/stop-for-location")
public class StopsForLocationResponse extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  private static final double DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY = 500;

  private static final double DEFAULT_SEARCH_RADIUS_WITH_QUERY = 10 * 1000;

  @Autowired
  private NycTransitDataService _service;

  private double _lat;

  private double _lon;

  private double _radius;

  private double _latSpan;

  private double _lonSpan;

  private MaxCountSupport _maxCount = new MaxCountSupport(100, 250);

  private String _query;

  public StopsForLocationResponse() {
    super(V1);
  }

  @QueryParam("Lat")
  public void setLat(double lat) {
    _lat = lat;
  }

  @QueryParam("Lon")
  public void setLon(double lon) {
    _lon = lon;
  }

  @QueryParam("Radius")
  public void setRadius(double radius) {
    _radius = radius;
  }

  @QueryParam("LatSpan")
  public void setLatSpan(double latSpan) {
    _latSpan = latSpan;
  }

  @QueryParam("LonSpan")
  public void setLonSpan(double lonSpan) {
    _lonSpan = lonSpan;
  }

  @QueryParam("Query")
  public void setQuery(String query) {
    _query = query;
  }

  @QueryParam("MaxCount")
  public void setMaxCount(int maxCount) {
    _maxCount.setMaxCount(maxCount);
  }

  @GET
  public Response index() throws IOException, ServiceException {

    int maxCount = _maxCount.getMaxCount();

    if (maxCount <= 0)
      addFieldError("maxCount", "must be greater than zero");

    if (hasErrors())
      return getValidationErrorsResponse();

    CoordinateBounds bounds = getSearchBounds();

    SearchQueryBean searchQuery = new SearchQueryBean();
    searchQuery.setBounds(bounds);
    searchQuery.setMaxCount(maxCount);
    searchQuery.setType(EQueryType.BOUNDS);
    if (_query != null) {
      searchQuery.setQuery(_query);
      searchQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
    }

    try {
      StopsBean result = _service.getStops(searchQuery);
      return transformResult(result);
    } catch (OutOfServiceAreaServiceException ex) {
      return transformOutOfRangeResult();
    }
  }

  private Response transformResult(StopsBean result) {
    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    factory.filterNonRevenueStops(result);
    if (isVersion(V1)) {
      return getOkResponse(result);
    } else if (isVersion(V2)) {
      return getOkResponse(factory.getResponse(result));
    } else {
      return getUnknownVersionResponse();
    }
  }

  private Response transformOutOfRangeResult() {
    if (isVersion(V1)) {
      return getOkResponse(new StopsBean());
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponse(factory.getEmptyList(StopV2Bean.class, true));
    } else {
      return getUnknownVersionResponse();
    }
  }

  private CoordinateBounds getSearchBounds() {

    if (_radius > 0) {
      return SphericalGeometryLibrary.bounds(_lat, _lon, _radius);
    } else if (_latSpan > 0 && _lonSpan > 0) {
      return SphericalGeometryLibrary.boundsFromLatLonOffset(_lat, _lon,
          _latSpan / 2, _lonSpan / 2);
    } else {
      if (_query != null)
        return SphericalGeometryLibrary.bounds(_lat, _lon,
            DEFAULT_SEARCH_RADIUS_WITH_QUERY);
      else
        return SphericalGeometryLibrary.bounds(_lat, _lon,
            DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY);
    }
  }
}
