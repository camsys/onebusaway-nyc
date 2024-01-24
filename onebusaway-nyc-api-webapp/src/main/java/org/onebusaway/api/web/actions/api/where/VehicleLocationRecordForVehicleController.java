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
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;

@RestController
@RequestMapping("/where/vehicle-location-record-for-vehicle")
public class VehicleLocationRecordForVehicleController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

//  private long _time = System.currentTimeMillis();

  public VehicleLocationRecordForVehicleController() {
    super(V2);
  }

  @GetMapping
  public ResponseBean show(@RequestParam(value = "Id", required = false) String id,
                           @RequestParam(name ="Time", required = false, defaultValue = "-1") Long time) throws IOException, ServiceException {
    time = longToTime(time);
    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    if (hasErrors())
      return getValidationErrorsResponseBean();

    BeanFactoryV2 factory = getBeanFactoryV2();

    VehicleLocationRecordBean record = _service.getVehicleLocationRecordForVehicleId(
        id, time);
    if (record == null)
      return getResourceNotFoundResponseBean();
    return getOkResponseBean(factory.entry(factory.getVehicleLocationRecord(record)));
  }
}
