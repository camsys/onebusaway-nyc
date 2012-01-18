/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.List;

@Controller
public class DataDebugController {

  @Autowired
  private TransitGraphDao _graphDao;
  
  @Autowired
  private DestinationSignCodeService _dscService;
  
  @RequestMapping(value = "/data-debug.do", method = RequestMethod.GET)
  public ModelAndView index() {

    List<AgencyAndId> nullTrips = new ArrayList<AgencyAndId>();
    
    for(TripEntry tripEntry : _graphDao.getAllTrips()) {
      AgencyAndId tripId = tripEntry.getId();
      String dsc = _dscService.getDestinationSignCodeForTripId(tripId);
      
      if(dsc == null) {
        nullTrips.add(tripId);
      }
    }
    
    return new ModelAndView("data-debug.jspx", "nullTrips", nullTrips);
  }

}
