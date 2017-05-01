/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
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
package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Build the service alerts feed for the current time.
 */
@Component
public class ServiceAlertServiceImpl extends AbstractFeedMessageService {

    private ServiceAlertFeedBuilder _feedBuilder;

    private NycTransitDataService _transitDataService;

    @Autowired
    public void setFeedBuilder(ServiceAlertFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Override
    public List<FeedEntity.Builder> getEntities(long time) {
        List<FeedEntity.Builder> ret = new ArrayList<FeedEntity.Builder>();
        for (AgencyWithCoverageBean ab : _transitDataService.getAgenciesWithCoverage()) {
            String agencyId = ab.getAgency().getId();
            ListBean<ServiceAlertBean> serviceAlertBeans = _transitDataService.getAllServiceAlertsForAgencyId(agencyId);
            for (ServiceAlertBean bean : serviceAlertBeans.getList()) {
                Alert.Builder alert = _feedBuilder.getAlertFromServiceAlert(bean);
                FeedEntity.Builder entity = FeedEntity.newBuilder();
                entity.setAlert(alert);
                entity.setId(bean.getId());
                ret.add(entity);
            }
        }
        return ret;
    }
}
