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
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.VehicleStatusV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;

@RestController
@RequestMapping("/where/vehicle/{vehicleId}")
public class VehicleController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  public VehicleController() {
    super(V2);
  }

  @GetMapping
  public ResponseEntity<ResponseBean> show(@PathVariable("vehicleId") String id,
                                           @RequestParam(name ="Time", required = false) Long time) throws IOException, ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponseBean();


    time = longToTime(time);

    BeanFactoryV2 factory = getBeanFactoryV2();

    try {
      VehicleStatusBean vehicle = _service.getVehicleForAgency(id, time);

      if (vehicle == null)
        return getResourceNotFoundResponseBean();

      return getOkResponseBean(factory.getVehicleStatusResponse(vehicle));

    } catch (OutOfServiceAreaServiceException ex) {
      return getOkResponseBean(factory.getEmptyList(VehicleStatusV2Bean.class, true));
    }
  }
}
