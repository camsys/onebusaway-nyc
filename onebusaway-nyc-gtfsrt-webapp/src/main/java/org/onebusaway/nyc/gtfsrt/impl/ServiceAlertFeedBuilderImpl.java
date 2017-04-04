package org.onebusaway.nyc.gtfsrt.impl;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.nyc.gtfsrt.service.ServiceAlertFeedBuilder;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.stereotype.Component;

import static org.onebusaway.nyc.gtfsrt.util.GtfsRealtimeLibrary.makeAlert;

@Component
public class ServiceAlertFeedBuilderImpl implements ServiceAlertFeedBuilder {
    @Override
    public Alert.Builder getAlertFromServiceAlert(ServiceAlertBean alert) {
        return makeAlert(alert);
    }
}
