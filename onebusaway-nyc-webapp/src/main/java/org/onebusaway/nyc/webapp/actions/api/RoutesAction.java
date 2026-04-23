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
package org.onebusaway.nyc.webapp.actions.api;

import java.io.IOException;
import java.util.List;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.routes.RouteListService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.git.GitRepositoryHelper;
import org.onebusaway.nyc.util.model.GitRepositoryState;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.api.model.RouteResult;
import org.onebusaway.transit_data.model.RouteBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Action for release (status) page
 *
 */
@ParentPackage("json-default")
//@Result(type="json")
@Result(type="json", params={"callbackParameter", "callback"})
public class RoutesAction extends OneBusAwayNYCActionSupport {

    private static final long serialVersionUID = 1L;

    @Autowired
    @Qualifier("NycRouteListService")
    private RouteListService _routeListService;

    @Autowired
    private ConfigurationService _configurationService;

//    public boolean getShowAgencyNames() {
//        return _routeListService.getShowAgencyNames();
//    }
//
//    public boolean getUseAgencyId() {
//        return _routeListService.getUseAgencyId();
//    }

    private List<RouteBean> _routes = null;


//    public String getGoogleAdClientId() {
//        return _configurationService.getConfigurationValueAsString("display.googleAdsClientId", "");
//    }


    @Override
    public String execute() throws Exception {
        _routes = _routeListService.getRoutes();
        return SUCCESS;
    }

    public List<RouteBean> getRoutes() {
        return _routes;

    }

}