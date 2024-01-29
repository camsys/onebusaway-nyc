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

import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.web.mapping.formatting.FieldErrorMessage;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.problems.EProblemReportStatus;
import org.onebusaway.transit_data.model.problems.StopProblemReportBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/where/report-problem-with-stop-resource")
public class ReportProblemWithStopController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService _service;

  public ReportProblemWithStopController() {
    super(2);
  }


  @GetMapping
  public ResponseBean create(StopProblemReportBean model) throws IOException, ServiceException {

    FieldErrorSupport fieldErrors = new FieldErrorSupport()
            .hasFieldError(model.getStopId(),"stopId");
    if (fieldErrors.hasErrors())
      return getValidationErrorsResponseBean(fieldErrors.getErrors());

    model.setTime(System.currentTimeMillis());
    model.setStatus(EProblemReportStatus.NEW);
    _service.reportProblemWithStop(model);

    return getOkResponseBean(new Object());
  }
}
