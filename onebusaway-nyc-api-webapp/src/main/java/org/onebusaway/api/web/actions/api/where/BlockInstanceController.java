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
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.blocks.BlockInstanceV2Bean;
import org.onebusaway.api.web.mapping.formatting.NycDateConverterWrapper;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/where/block-instance/{blockId}")
public class BlockInstanceController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  @Autowired
  private NycDateConverterWrapper _formatter;


  public BlockInstanceController() {
    super(V2);
  }


  @GetMapping
  public ResponseEntity<ResponseBean> show(@PathVariable("blockId") String id,
                                           @RequestParam(name ="ServiceDate", required = false) String serviceDateString
                           ) throws ServiceException {
    if (!isVersion(V2))
      return getUnknownVersionResponseBean();

    Long serviceDate = null;
    try {
      serviceDate = _formatter.stringToLong(serviceDateString);
    } catch (RuntimeException e){
      FieldErrorSupport fieldErrors = new FieldErrorSupport()
              .invalidValue("ServiceDate");
      if (fieldErrors.hasErrors())
        return getValidationErrorsResponseBean(fieldErrors.getErrors());
    }



    BlockInstanceBean blockInstance = _service.getBlockInstance(id,
        serviceDate);

    if (blockInstance == null)
      return getResourceNotFoundResponseBean();

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    BlockInstanceV2Bean bean = factory.getBlockInstance(blockInstance);
    EntryWithReferencesBean<BlockInstanceV2Bean> response = factory.entry(bean);
    return getOkResponseBean(response);
  }
}
