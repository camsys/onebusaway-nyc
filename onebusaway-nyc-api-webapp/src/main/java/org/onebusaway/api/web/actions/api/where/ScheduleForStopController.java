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

import java.util.Date;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.StopScheduleBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.onebusaway.api.model.ResponseBean;

import javax.validation.Valid;

@RestController
@RequestMapping("/where/schedule-for-stop/{stopId}")
public class ScheduleForStopController {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  @Autowired
  private ApiActionSupport _support;

  @GetMapping
  public ResponseEntity<ResponseBean> show(@Valid @PathVariable("stopId") String id,
                                           @Valid @RequestParam(name ="Date", required = false) Long time) throws ServiceException {

    FieldErrorSupport fieldErrors = new FieldErrorSupport()
            .hasFieldError(time,"Date");
    if (fieldErrors.hasErrors())
      return _support.getValidationErrorsResponseBean(fieldErrors.getErrors());

    time = _support.longToTime(time);
    Date date = new Date(time);

    StopScheduleBean stopSchedule = _service.getScheduleForStop(id, date);

    BeanFactoryV2 factory = _support.getBeanFactoryV2();
    return _support.getOkResponseBean(factory.getResponse(stopSchedule));
  }
}
