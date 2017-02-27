package org.onebusaway.nyc.gtfsrt.service;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public interface ServiceAlertFeedBuilder {

    GtfsRealtime.Alert getAlertFromServiceAlert(ServiceAlertBean alert);
}
