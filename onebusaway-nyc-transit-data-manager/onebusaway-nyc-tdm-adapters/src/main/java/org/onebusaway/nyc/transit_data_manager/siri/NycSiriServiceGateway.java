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
      DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean,
      String defaultAgencyId) {
    boolean isNew = getPersister().saveOrUpdateServiceAlert(serviceAlertBean);
    result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
        (isNew ? "added" : "updated"));
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
    for (ServiceAlertBean alert : alerts) {
      existing.add(alert.getId());
    }
    return existing;
  }

  @Override
  void postServiceDeliveryActions(SituationExchangeResults results,
      Collection<String> deletedIds) throws Exception {
    for (ServiceAlertSubscription subscription : getActiveServiceAlertSubscriptions()) {
      new Thread(new SubscriptionSender(subscription, deletedIds)).start();
    }
  }

  class SubscriptionSender implements Runnable {

    private ServiceAlertSubscription subscription;
    private Collection<String> deletedIds;

    public SubscriptionSender(ServiceAlertSubscription subscription,
        Collection<String> deletedIds) {
      this.subscription = subscription;
      this.deletedIds = deletedIds;
    }

    public void run() {
      try {
        _log.info("Sending service alerts to subscriber: "
            + subscription.getAddress());
        subscription.send(getPersister().getAllActiveServiceAlerts(),
            deletedIds, _environment.getEnvironment());
        _log.info("Successfully sent service alerts to subscriber: "
            + subscription.getAddress());
      } catch (Exception e) {
        Integer failures = subscription.getConsecutiveFailures();
        if (failures == null)
          failures = new Integer(0);
        failures += 1;
        subscription.setConsecutiveFailures(failures);
        getPersister().saveOrUpdateSubscription(subscription);
        _log.error("Unable to send service alert updates to subscriber: "
            + subscription.getAddress() + ", consecutive failures: " + failures
            + ", exception: " + e.getMessage());
        if (failures >= 3) {
          _log.info(">=3 consecutive failures, deleting subscription.");
          getPersister().deleteSubscription(subscription);
        }
      }
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
  public void deleteAllServiceAlerts() {
    // noop for gateway mode
  }
  
}
