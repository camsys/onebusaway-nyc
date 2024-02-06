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

import java.util.List;

import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.services.AlarmService;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/where/cancel-alarms")
public class CancelAlarmsController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  @Autowired
  private AlarmService _alarmService;

  public CancelAlarmsController() {
    super(V2);
  }


  @GetMapping
  public ResponseBean index(@RequestParam(name ="Ids", required = false) List<String> ids) throws ServiceException {

    if (ids != null) {
      for (String id : ids) {
        _service.cancelAlarmForArrivalAndDepartureAtStop(id);
        _alarmService.cancelAlarm(id);
      }
    }

    if (isVersion(V2)) {
      return getOkResponseBean("");
    } else {
      return getUnknownVersionResponseBean();
    }
  }
}
