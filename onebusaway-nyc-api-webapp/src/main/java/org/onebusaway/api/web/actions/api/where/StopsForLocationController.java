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

import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.onebusaway.api.model.ResponseBean;
@RestController
@RequestMapping("/where/stops-for-location")
public class StopsForLocationController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  private static final double DEFAULT_SEARCH_RADIUS_WITHOUT_QUERY = 500;

  private static final double DEFAULT_SEARCH_RADIUS_WITH_QUERY = 10 * 1000;

  @Autowired
  private NycTransitDataService _service;


  public StopsForLocationController() {
    super(V1);
  }


  @GetMapping
  public ResponseBean index( @RequestParam(name ="Lat", required = false)Double lat,
                             @RequestParam(name ="Lon", required = false) Double lon,
                             @RequestParam(name ="LatSpan", required = false) Double latSpan,
                             @RequestParam(name ="LonSpan", required = false) Double lonSpan,
                             @RequestParam(name ="Radius", required = false) Double radius,
                             @RequestParam(name ="Query", required = false) String query,
                             @RequestParam(name ="MaxCount", required = false) Long maxCountArg) throws IOException, ServiceException {
    MaxCountSupport _maxCount = new MaxCountSupport(100, 250);
    if(maxCountArg!=-1) _maxCount.setMaxCount(maxCountArg.intValue());
    int maxCount = _maxCount.getMaxCount();

    if (maxCount <= 0)
      addFieldError("maxCount", "must be greater than zero");
//    todo: add field errors for Lat,Lon,LatSpan,LonSpan,Radius

    if (hasErrors())
      return getValidationErrorsResponseBean();

    CoordinateBounds bounds = getSearchBounds(radius, lat, lon, latSpan, lonSpan, query);

    SearchQueryBean searchQuery = new SearchQueryBean();
    searchQuery.setBounds(bounds);
    searchQuery.setMaxCount(maxCount);
    searchQuery.setType(EQueryType.BOUNDS);
    if (query != null) {
      searchQuery.setQuery(query);
      searchQuery.setType(EQueryType.BOUNDS_OR_CLOSEST);
    }

    try {
      StopsBean result = _service.getStops(searchQuery);
      return transformResult(result);
    } catch (OutOfServiceAreaServiceException ex) {
      return transformOutOfRangeResult();
    }
  }

  private ResponseBean transformResult(StopsBean result) {
    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    factory.filterNonRevenueStops(result);
    if (isVersion(V1)) {
      return getOkResponseBean(result);
    } else if (isVersion(V2)) {
      return getOkResponseBean(factory.getResponse(result));
    } else {
      return getUnknownVersionResponseBean();
    }
  }

  private ResponseBean transformOutOfRangeResult() {
    if (isVersion(V1)) {
      return getOkResponseBean(new StopsBean());
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponseBean(factory.getEmptyList(StopV2Bean.class, true));
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
