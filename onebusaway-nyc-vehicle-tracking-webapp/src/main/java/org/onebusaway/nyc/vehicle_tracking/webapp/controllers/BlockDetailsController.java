package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data.model.blocks.BlockBean;
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

    List<String> dscs = new ArrayList<String>();

    for (TripBean trip : block.getTrips()) {
      AgencyAndId tripId = AgencyAndIdLibrary.convertFromString(trip.getId());
      String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(tripId);
      if (dsc == null)
        dsc = "NA";
      dscs.add(dsc);
    }

    Map<String, Object> model = new HashMap<String, Object>();
    model.put("block", block);
    model.put("dscs", dscs);
    return new ModelAndView("block-details.jspx", model);
  }

}
