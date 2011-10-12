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

import org.onebusaway.nyc.transit_data_federation.impl.bundle.BundleManagementServiceImpl;
import org.onebusaway.utility.DateLibrary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BundleManagementController {

  @Autowired
  private BundleManagementServiceImpl _bundleManager;

  @RequestMapping(value = "/change-bundle.do", method = RequestMethod.GET)
  public ModelAndView index(@RequestParam String bundleId, 
      @RequestParam(required=false) String time) throws Exception {

    if(time != null && !time.equals("")) {
	    _bundleManager.setTime(DateLibrary.getIso8601StringAsTime(time));
	  }

	  _bundleManager.changeBundle(bundleId);

	  return new ModelAndView("change-bundle.jspx");
  }

}
