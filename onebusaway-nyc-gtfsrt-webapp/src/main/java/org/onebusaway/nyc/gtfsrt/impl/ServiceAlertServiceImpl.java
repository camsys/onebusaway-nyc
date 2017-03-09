package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    protected List<FeedEntity.Builder> getEntities(long time) {
        List<FeedEntity.Builder> ret = new ArrayList<FeedEntity.Builder>();
        ListBean<ServiceAlertBean> serviceAlertBeans = _transitDataService.getAllServiceAlertsForAgencyId(getAgencyId());
        for (ServiceAlertBean bean : serviceAlertBeans.getList()) {
            Alert alert = _feedBuilder.getAlertFromServiceAlert(bean);
            FeedEntity.Builder entity = FeedEntity.newBuilder();
            entity.setAlert(alert);
            entity.setId(bean.getId());
            ret.add(entity);
        }
        return ret;
    }
}
