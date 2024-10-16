/**
 * Copyright (c) 2013 Kurt Raschke
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

package org.onebusaway.nyc.presentation.impl.routes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.onebusaway.nyc.presentation.comparator.AgencyAndRouteComparator;
import org.onebusaway.nyc.presentation.comparator.AlphanumComparator;
import org.onebusaway.nyc.presentation.comparator.RouteComparator;
import org.onebusaway.nyc.presentation.impl.realtime.PresentationServiceImpl;
import org.onebusaway.nyc.presentation.service.routes.RouteListService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component("NycRouteListService")
public class RouteListServiceImpl implements RouteListService {
	
    @Autowired
    private ConfigurationService _configurationService;
    @Autowired
    private NycTransitDataService _nycTransitDataService;
    
    private static Logger _log = LoggerFactory.getLogger(RouteListServiceImpl.class);
    
    @Override
    public boolean getShowAgencyNames() {
        return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.showAgencyNames", "false"));
    }

    @Override
    public boolean getUseAgencyId() {
        return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.useAgencyId", "false"));
    }

    @Override
    public List<RouteBean> getRoutes() {
    	_log.info("getRoutes Called");
    	List<RouteBean> allRoutes = new ArrayList<RouteBean>();

        List<AgencyWithCoverageBean> agencies = _nycTransitDataService.getAgenciesWithCoverage();

        for (AgencyWithCoverageBean agency : agencies) {
            allRoutes.addAll(_nycTransitDataService.getRoutesForAgencyId(agency.getAgency().getId()).getList());
        }
        
        _log.info("getShowAgencyNames() is " + getShowAgencyNames());
        
        if(getShowAgencyNames()){
        	Collections.sort(allRoutes, new AgencyAndRouteComparator());
        	_log.debug("AgencyAndRouteComparator Sort");
        }
        else{
        	Collections.sort(allRoutes, new RouteComparator());
        	_log.debug("RouteComparator Sort");
        }
		
        return allRoutes;
    }
}
