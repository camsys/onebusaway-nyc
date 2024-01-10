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
import org.onebusaway.api.model.transit.EntryWithReferencesBean;
import org.onebusaway.api.model.transit.blocks.BlockInstanceV2Bean;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.springframework.beans.factory.annotation.Autowired;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/where/block-instance/{blockId}")
public class BlockInstanceResource extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private NycTransitDataService _service;

  private String _id;

  private long _serviceDate;

  public BlockInstanceResource() {
    super(V2);
  }

  @PathParam("blockId")
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  @QueryParam("ServiceDate")
  public void setServiceDate(Date serviceDate) {
    _serviceDate = serviceDate.getTime();
  }

  @GET
  public Response show() throws ServiceException {

    if (!isVersion(V2))
      return getUnknownVersionResponse();

    if (hasErrors())
      return getValidationErrorsResponse();

    BlockInstanceBean blockInstance = _service.getBlockInstance(_id,
        _serviceDate);

    if (blockInstance == null)
      return getResourceNotFoundResponse();

    BeanFactoryV2 factory = getBeanFactoryV2(_service);
    BlockInstanceV2Bean bean = factory.getBlockInstance(blockInstance);
    EntryWithReferencesBean<BlockInstanceV2Bean> response = factory.entry(bean);
    return getOkResponse(response);
  }
}
