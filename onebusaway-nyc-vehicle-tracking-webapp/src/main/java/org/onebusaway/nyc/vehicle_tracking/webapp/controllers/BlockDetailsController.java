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

import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.beans.BlockBeanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BlockDetailsController {

  private BlockBeanService _blockBeanService;

  private DestinationSignCodeService _destinationSignCodeService;

  @Autowired
  public void setBlockBeanService(BlockBeanService blockBeanService) {
    _blockBeanService = blockBeanService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @RequestMapping(value = "/block-details.do", method = RequestMethod.GET)
  public ModelAndView index(@RequestParam String blockId) {

    AgencyAndId id = AgencyAndIdLibrary.convertFromString(blockId);
    BlockBean block = _blockBeanService.getBlockForId(id);

    if (block == null)
      return new ModelAndView("block-details-notFound.jspx");
    
    Map<String,String> dscsByTripId = new HashMap<String, String>();
    for( BlockConfigurationBean blockConfig : block.getConfigurations() ) {
      for( BlockTripBean blockTrip : blockConfig.getTrips() ) {
        TripBean trip = blockTrip.getTrip();
        AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(trip.getId());
        String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);
        if (dsc == null)
          dsc = "NA";
        dscsByTripId.put(trip.getId(),dsc);
      }
    }

    Map<String, Object> model = new HashMap<String, Object>();
    model.put("block", block);
    model.put("dscs", dscsByTripId);
    return new ModelAndView("block-details.jspx", model);
  }

}
