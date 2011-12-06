package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public interface SiriServicePersister {

  public void saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean);
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId);
  public List<ServiceAlertBean> getAllActiveServiceAlerts();
  public List<ServiceAlertBean> getAllServiceAlerts();
  
  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription);
  public void deleteSubscription(ServiceAlertSubscription subscription);
  public List<ServiceAlertSubscription> getAllActiveSubscriptions();
}
