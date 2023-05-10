package org.onebusaway.nyc.transit_data_manager.siri;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import java.util.List;

public interface ServiceAlertsPersister {
  boolean saveOrUpdateServiceAlert(String defaultAgencyId, ServiceAlertBean serviceAlertBean);

  boolean saveOrUpdateServiceAlerts(String defaultAgencyId, List<ServiceAlertBean> serviceAlertBeans);

  void deleteServiceAlertById(String serviceAlertId);
}
