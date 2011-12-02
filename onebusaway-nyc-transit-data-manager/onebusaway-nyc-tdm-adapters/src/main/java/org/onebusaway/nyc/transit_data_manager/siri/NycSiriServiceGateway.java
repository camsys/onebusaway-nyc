package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;

public class NycSiriServiceGateway extends NycSiriService {

  @Autowired
  private SiriServicePersister _siriServicePersister;

  @Override
  void setupForMode() throws Exception, JAXBException {
    _log.info("No setup required in gateway mode.");
  }

  
  @Override
  void addOrUpdateServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean, String defaultAgencyId) {
    result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
        "added");
    getPersister().saveOrUpdateServiceAlert(serviceAlertBean);
  }
  
  
  void removeServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, String serviceAlertId) {
    result.countPtSituationElementResult(deliveryResult, serviceAlertId,
        "removed");
    getPersister().deleteServiceAlertById(serviceAlertId);
  }

  
  List<String> getExistingAlertIds(Set<String> agencies) {
    List<String> existing = new ArrayList<String>();
    List<ServiceAlertBean> alerts = getPersister().getAllActiveServiceAlerts();
    for (ServiceAlertBean alert: alerts) {
      existing.add(alert.getId());
    }
    return existing;
  }


  @Override
  void postServiceDeliveryActions(SituationExchangeResults results, Collection<String> deletedIds) throws Exception {
    for (ServiceAlertSubscription subscription: getActiveServiceAlertSubscriptions()) {
      subscription.send(getPersister().getAllActiveServiceAlerts(), deletedIds);
    }
  }

  void addSubscription(ServiceAlertSubscription subscription) {
    getPersister().saveOrUpdateSubscription(subscription);
  }

  public List<ServiceAlertSubscription> getActiveServiceAlertSubscriptions() {
    return getPersister().getAllActiveSubscriptions();
  }

  public SiriServicePersister getPersister() {
    return _siriServicePersister;
  }

  public void setPersister(SiriServicePersister _siriServicePersister) {
    this._siriServicePersister = _siriServicePersister;
  }


  @Override
  public boolean isInputIncremental() {
    return false;
  }


}
