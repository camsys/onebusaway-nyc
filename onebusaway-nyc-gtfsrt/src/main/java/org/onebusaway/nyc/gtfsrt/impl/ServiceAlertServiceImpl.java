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
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Build the service alerts feed for the current time.
 * Caches the feed via a background refresh thread; refresh interval is
 * configurable through the ConfigurationService.
 */
@Component
public class ServiceAlertServiceImpl extends AbstractFeedMessageService {

    private static final String CONFIG_ALERTS_REFRESH_INTERVAL = "gtfsrt.alertRefreshInterval";

    private ServiceAlertFeedBuilder _feedBuilder;

    private NycTransitDataService _transitDataService;

    private ConfigurationService _configurationService;

    @Autowired
    public void setFeedBuilder(ServiceAlertFeedBuilder feedBuilder) {
        _feedBuilder = feedBuilder;
    }

    @Autowired
    public void setTransitDataService(NycTransitDataService transitDataService) {
        _transitDataService = transitDataService;
    }

    @Autowired
    public void setConfigurationService(ConfigurationService configurationService) {
        _configurationService = configurationService;
    }

    @PostConstruct
    @Override
    public void init() {
        refreshConfig();
        super.init();
    }

    @Refreshable(dependsOn = CONFIG_ALERTS_REFRESH_INTERVAL)
    private void refreshConfig() {
        int interval = Integer.parseInt(
                _configurationService.getConfigurationValueAsString(
                        CONFIG_ALERTS_REFRESH_INTERVAL,
                        String.valueOf(DEFAULT_REFRESH_INTERVAL_SECONDS)));
        setRefreshIntervalSeconds(interval);
    }

    @Override
    public List<FeedEntity.Builder> getEntities(long time) {
        List<FeedEntity.Builder> ret = new ArrayList<>();
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
