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
import java.util.List;
import java.util.Optional;

import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.Response;

@RestController
@RequestMapping("/where/agencies-with-coverage")
public class AgenciesWithCoverageResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V1 = 1;

  private static final int V2 = 2;

  private MaxCountSupport _maxCount = new MaxCountSupport();

  @Autowired
  private TransitDataService _service;

  public AgenciesWithCoverageResource() {
    super(V1);
  }



  @GetMapping
  public Response index(@RequestParam(name = "MaxCount", required = false) Optional<Integer> maxCount) throws IOException, ServiceException {
    applyIfPresent(maxCount,_maxCount::setMaxCount);
    if (hasErrors())
      return getValidationErrorsResponse();

    List<AgencyWithCoverageBean> beans = _service.getAgenciesWithCoverage();

    if (isVersion(V1)) {
      return getOkResponse(beans);
    } else if (isVersion(V2)) {
      BeanFactoryV2 factory = getBeanFactoryV2();
      return getOkResponse(factory.getResponse(beans));
    } else {
      return getUnknownVersionResponse();
    }
  }
}
