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

package org.onebusaway.api.web.actions.api.where;

import org.onebusaway.api.model.ResponseBean;
import org.onebusaway.api.model.where.ActiveBundleBeanV1;
import org.onebusaway.api.web.actions.api.ApiActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ActiveBundleController extends ApiActionSupport {

    private static final long serialVersionUID = 1L;

    private static final int V1 = 1;

    @Autowired
    private TransitDataService _service;

    public ActiveBundleController() {
        super(V1);
    }

    @RequestMapping("/where/active-bundle")
    public ResponseBean index() {

        String activeBundleId = _service.getActiveBundleId();
        ActiveBundleBeanV1 activeBundleBean = new ActiveBundleBeanV1(activeBundleId);

        return getOkResponseBean(activeBundleBean);
    }
}
