/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.api.actions.api.where;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.where.ActiveBundleBeanV1;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/where/ActiveBundle")
public class ActiveBundleResource extends ApiActionSupport {

    private static final long serialVersionUID = 1L;

    private static final int V1 = 1;

    @Autowired
    private TransitDataService _service;

    public ActiveBundleResource() {
        super(V1);
    }

    @GET
    public Response index() {

        if (hasErrors())
            return getValidationErrorsResponse();

        String activeBundleId = _service.getActiveBundleId();
        ActiveBundleBeanV1 activeBundleBean = new ActiveBundleBeanV1(activeBundleId);

        return getOkResponse(activeBundleBean);
    }
}
