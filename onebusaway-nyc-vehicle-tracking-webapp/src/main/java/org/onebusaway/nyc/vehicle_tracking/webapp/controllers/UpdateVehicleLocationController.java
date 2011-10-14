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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UpdateVehicleLocationController {

  private static SimpleDateFormat _format = new SimpleDateFormat(
      "yyyy-MM-dd' 'HH:mm:ss");

  static {
    _format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
  }

  private VehicleLocationInferenceService _vehicleLocationService;

  @Autowired
  public void setVehicleLocationService(
		  VehicleLocationInferenceService vehicleLocationService) {
    _vehicleLocationService = vehicleLocationService;
  }

  @RequestMapping("/update-vehicle-location.do")
  public ModelAndView index(
      @RequestParam(required = false, defaultValue = "") String time,
      @RequestParam() String vehicleId,
      @RequestParam() double lat,
      @RequestParam() double lon,
      @RequestParam() String dsc,
      @RequestParam(required = false, defaultValue = "null") String operatorId,
      @RequestParam(required = false, defaultValue = "null") String runId,
      @RequestParam(required = false, defaultValue = "false") boolean saveResults)
      throws ParseException {

    long t = System.currentTimeMillis();

    if (time != null && ! time.trim().isEmpty()) {
      Date date = _format.parse(time);
      t = date.getTime();
    }

    NycRawLocationRecord vlr = new NycRawLocationRecord();
    vlr.setTime(t);
    vlr.setVehicleId(AgencyAndIdLibrary.convertFromString(vehicleId));
    vlr.setLatitude(lat);
    vlr.setLongitude(lon);
    vlr.setDestinationSignCode(dsc);
    vlr.setOperatorId(operatorId);
    String[] runInfo = StringUtils.splitByWholeSeparator(runId, "_");
    if (runInfo != null && runInfo.length > 0) {
      vlr.setRunNumber(runInfo[0]);
      if (runInfo.length > 1)
        vlr.setRunRouteId(runInfo[1]);
    }
    
    _vehicleLocationService.handleNycRawLocationRecord(vlr);

    return new ModelAndView("update-vehicle-location.jspx");
  }
}
