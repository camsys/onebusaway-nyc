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
package org.onebusaway.api.actions.api.where;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;

public class IsRouteExpressAction extends ApiActionSupport {

    private static final long serialVersionUID = 1L;
    private static final int V2 = 2;

    @Autowired
    private NycTransitDataService _service;

    private String _id;

    public IsRouteExpressAction() {
        super(V2);
    }

    @RequiredFieldValidator
    public void setId(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    public DefaultHttpHeaders show() throws ServiceException {

        if (hasErrors())
            return setValidationErrorsResponse();

        boolean result = _service.isRouteExpress(_id);

        return setOkResponse(result);
    }
}
