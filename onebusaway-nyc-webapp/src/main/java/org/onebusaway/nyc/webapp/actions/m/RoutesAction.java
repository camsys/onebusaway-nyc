/**
 * Copyright (c) 2013 Kurt Raschke
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.m;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Action for route index page
 *
 */
public class RoutesAction extends OneBusAwayNYCActionSupport {

    private static final long serialVersionUID = 1L;
    @Autowired
    private ConfigurationService _configurationService;
    @Autowired
    private NycTransitDataService _nycTransitDataService;

    public boolean getShowAgencyNames() {
        return _configurationService.getConfigurationValueAsString("display.showAgencyNames", "false").equals("true");
    }
    
    public boolean getUseAgencyId() {
        return _configurationService.getConfigurationValueAsString("display.useAgencyId", "false").equals("true");   
    }
    
    public List<RouteBean> getRoutes() {
        List<RouteBean> allRoutes = new ArrayList<RouteBean>();

        List<AgencyWithCoverageBean> agencies = _nycTransitDataService.getAgenciesWithCoverage();

        for (AgencyWithCoverageBean agency : agencies) {
            allRoutes.addAll(_nycTransitDataService.getRoutesForAgencyId(agency.getAgency().getId()).getList());
        }
        Collections.sort(allRoutes, new Comparator<RouteBean>(){

            @Override
            public int compare(RouteBean t, RouteBean t1) {
                if (t.getAgency().getName().compareTo(t1.getAgency().getName()) == 0) {
                    if (t.getShortName() != null && t1.getShortName() != null) {
                    return t.getShortName().compareTo(t1.getShortName());
                    } else {
                    return t.getId().compareTo(t1.getId());
                    }
                } else {
                return t.getAgency().getName().compareTo(t1.getAgency().getName());
                }
            }
        
    });
     return allRoutes;   
    }
}
