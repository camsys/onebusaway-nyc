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

import java.util.*;

import org.joda.time.DateTime;
import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.where.ArrivalAndDepartureBeanV1;
import org.onebusaway.api.model.where.StopWithArrivalsAndDeparturesBeanV1;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/where/arrivals-and-departures-for-stop/{stopId}")
public class ArrivalsAndDeparturesForStopController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  
//  private ArrivalsAndDeparturesQueryBean _query = new ArrivalsAndDeparturesQueryBean();

  public ArrivalsAndDeparturesForStopController() {
    super(V1);
  }

  @GetMapping
  public ResponseBean getTripsByBlockId(@PathVariable("stopId") String id,
                                        ArrivalsAndDeparturesQueryBean query) {


    StopWithArrivalsAndDeparturesBean result = _service.getStopWithArrivalsAndDepartures(
            id, query);

    if (result == null)
      return getResourceNotFoundResponseBean();

    List<ArrivalAndDepartureBean> realTimeBeans = new LinkedList<ArrivalAndDepartureBean>();
    for (ArrivalAndDepartureBean bean : result.getArrivalsAndDepartures()){
      if (bean.isPredicted()){
        realTimeBeans.add(bean);
      }
    }
    result.getArrivalsAndDepartures().removeAll(realTimeBeans);

    if (isVersion(V1)) {
      // Convert data to v1 form
      List<ArrivalAndDepartureBeanV1> arrivals = getArrivalsAsV1(result);
      StopWithArrivalsAndDeparturesBeanV1 v1 = new StopWithArrivalsAndDeparturesBeanV1(
              result.getStop(), arrivals, result.getNearbyStops());
      return getOkResponseBean(v1);
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponseBean(factory.getResponse(result));
    } else {
      return getUnknownVersionResponseBean();
    }

  }

  private List<ArrivalAndDepartureBeanV1> getArrivalsAsV1(
      StopWithArrivalsAndDeparturesBean result) {

    List<ArrivalAndDepartureBeanV1> v1s = new ArrayList<ArrivalAndDepartureBeanV1>();

    for (ArrivalAndDepartureBean bean : result.getArrivalsAndDepartures()) {

      TripBean trip = bean.getTrip();
      RouteBean route = trip.getRoute();
      StopBean stop = bean.getStop();
      
      ArrivalAndDepartureBeanV1 v1 = new ArrivalAndDepartureBeanV1();
      v1.setRouteId(route.getId());
      if (trip.getRouteShortName() != null)
        v1.setRouteShortName(trip.getRouteShortName());
      else
        v1.setRouteShortName(route.getShortName());
      v1.setScheduledArrivalTime(bean.getScheduledArrivalTime());
      v1.setScheduledDepartureTime(bean.getScheduledDepartureTime());
      v1.setStatus(bean.getStatus());      
      v1.setStopId(stop.getId());
      v1.setTripHeadsign(trip.getTripHeadsign());
      v1.setTripId(trip.getId());

      v1s.add(v1);
    }

    return v1s;
  }
}
