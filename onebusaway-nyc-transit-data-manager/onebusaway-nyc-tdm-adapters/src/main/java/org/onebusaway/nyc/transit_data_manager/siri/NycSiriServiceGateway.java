package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class NycSiriServiceGateway extends NycSiriService {

  @Override
  void setupForMode() throws Exception, JAXBException {
    _log.info("No setup required in gateway mode.");
  }

  
  @Override
  void addOrUpdateServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean, String defaultAgencyId) {
    getCurrentServiceAlerts().put(serviceAlertBean.getId(), serviceAlertBean);
    result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
        "added");
  }
  
  
  void removeServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, String serviceAlertId) {
    ServiceAlertBean removed = getCurrentServiceAlerts().remove(serviceAlertId);
    result.countPtSituationElementResult(deliveryResult, serviceAlertId,
        "removed");
  }

  
  List<String> getExistingAlertIds(Set<String> agencies) {
    return new ArrayList<String>(getCurrentServiceAlerts().keySet());
  }


  @Override
  void postServiceDeliveryActions(SituationExchangeResults results) throws Exception {
    for (ServiceAlertSubscription subscription: getServiceAlertSubscriptions()) {
      subscription.send(results, getCurrentServiceAlerts());
    }
  }

  
}
