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

import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.web.mapping.formatting.NycDateConverterWrapper;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureForStopQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/where/arrival-and-departure-for-stop/{stopId}")
public class ArrivalAndDepartureForStopController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;


  public ArrivalAndDepartureForStopController() {
    super(V2);
  }


  @GetMapping
  public ResponseBean show(@PathVariable("stopId") String stopId,
                           ArrivalAndDepartureForStopQueryBean _query) throws ServiceException {

    FieldErrorSupport fieldErrors = new FieldErrorSupport()
            .hasFieldError(stopId,"stopId").hasFieldError(_query.getTripId(),"tripId")
            .hasFieldError(_query.getServiceDate(),"serviceDate");
    if (fieldErrors.hasErrors())
      return getValidationErrorsResponseBean(fieldErrors.getErrors());

    _query.setStopId(stopId);
    if (_query.getTime() == 0)
      _query.setTime(System.currentTimeMillis());
    _query.setTime(NycDateConverterWrapper.truncateToMidnight(_query.getTime()).toEpochMilli());

    ArrivalAndDepartureBean result = _service.getArrivalAndDepartureForStop(_query);

    if (result == null)
      return getResourceNotFoundResponseBean();

    if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponseBean(factory.getResponse(result));
    } else {
      return getUnknownVersionResponseBean();
    }
  }
}
