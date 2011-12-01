package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class MockSiriServicePersister implements
    SiriServicePersister {

  List<ServiceAlertSubscription> subs = new ArrayList<ServiceAlertSubscription>();
  @Override
  public void saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    // TODO Auto-generated method stub

  }

  @Override
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    // TODO not implemented
    return null;
  }

  @Override
  public List<ServiceAlertBean> getAllActiveServiceAlerts() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ServiceAlertBean> getAllServiceAlerts() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription) {
    subs.add(subscription);
  }

  @Override
  public void deleteSubscription(ServiceAlertSubscription subscription) {
    subs.remove(subscription);
  }

  @Override
  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    return subs;
  }

}