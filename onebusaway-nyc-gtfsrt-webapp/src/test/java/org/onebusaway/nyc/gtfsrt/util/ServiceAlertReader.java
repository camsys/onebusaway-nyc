package org.onebusaway.nyc.gtfsrt.util;

import org.onebusaway.nyc.transit_data_manager.siri.ServiceAlertRecord;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.util.List;

public class ServiceAlertReader extends RecordReader<ServiceAlertBean> {

    public List<ServiceAlertBean> getRecords(String filename) {
        return super.getRecords(filename, ServiceAlertRecord.class);
    }

    @Override
    public ServiceAlertBean convert(Object o) {
        ServiceAlertRecord rec = (ServiceAlertRecord) o;
        rec.setDescriptions(rec.getDescriptions().replace("\\\\", "\\"));
        return ServiceAlertRecord.toBean(((ServiceAlertRecord) o));
    }
}
