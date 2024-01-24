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
import org.onebusaway.api.model.transit.RouteV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.SearchQueryBean.EQueryType;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.ResponseBean;
@RestController
@RequestMapping("/where/routes-for-location")
public class RoutesForLocationController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  private static final double DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY = 500;

  private static final double DEFAULT_SEARCH_RADIUS_WITH_QUERY = 10 * 1000;

  @Autowired
  private TransitDataService _service;

  public RoutesForLocationController() {
    super(V1);
  }

  @GetMapping
  public ResponseBean index( @RequestParam(name ="Lat", required = false)double lat,
                             @RequestParam(name ="Lon", required = false) double lon,
                             @RequestParam(name ="LatSpan", required = false) double latSpan,
                             @RequestParam(name ="LonSpan", required = false) double lonSpan,
                             @RequestParam(name ="Radius", required = false) double radius,
                             @RequestParam(name ="Query", required = false) String query,
                             @RequestParam(name ="MaxCount", required = false) Long maxCount) throws IOException, ServiceException {

    if (maxCount <= 0)
      addFieldError("maxCount", "must be greater than zero");

    if (hasErrors())
      return getValidationErrorsResponseBean();

    MaxCountSupport _maxCount = new MaxCountSupport(10, 50);
    _maxCount.setMaxCount(maxCount.intValue());

    CoordinateBounds bounds = getSearchBounds(radius, lat, lon, latSpan, lonSpan, query);

    SearchQueryBean routesQuery = new SearchQueryBean();

    if (query != null)
      routesQuery.setQuery(query);

    routesQuery.setBounds(bounds);
    routesQuery.setMaxCount(_maxCount.getMaxCount());
    routesQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);

    try {
      RoutesBean result = _service.getRoutes(routesQuery);
      return transformResult(result);
    } catch (OutOfServiceAreaServiceException ex) {
      return transformOutOfRangeResult();
    }
  }

  private ResponseBean transformResult(RoutesBean result) {
    if (isVersion(V1)) {
      return getOkResponseBean(result);
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponseBean(factory.getResponse(result));
    } else {
      return getUnknownVersionResponseBean();
    }
  }

  private ResponseBean transformOutOfRangeResult() {
    if (isVersion(V1)) {
      return getOkResponseBean(new RoutesBean());
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponseBean(factory.getEmptyList(RouteV2Bean.class, true));
    } else {
      return getUnknownVersionResponseBean();
    }
  }

  private CoordinateBounds getSearchBounds(double radius, double lat, double lon, double latSpan,double lonSpan, String query) {

    if (radius > 0) {
      return SphericalGeometryLibrary.bounds(lat, lon, radius);
    } else if (latSpan > 0 && lonSpan > 0) {
      return SphericalGeometryLibrary.boundsFromLatLonOffset(lat, lon,
          latSpan / 2, lonSpan / 2);
    } else {
      if (query != null)
        return SphericalGeometryLibrary.bounds(lat, lon,
            DEFAULT_SEARCH_RADIUS_WITH_QUERY);
      else
        return SphericalGeometryLibrary.bounds(lat, lon,
            DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY);
    }
  }
}