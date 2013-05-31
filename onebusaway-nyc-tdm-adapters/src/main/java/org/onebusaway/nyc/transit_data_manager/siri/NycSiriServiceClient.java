package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class NycSiriServiceClient extends NycSiriService {

  
  @Override
  void setupForMode() throws Exception, JAXBException {
    boolean setupDone = false;
    int attempts = 0;
    do {
      attempts += 1;
      try {
        _log.info("Setting up for client mode.");
        sendAndProcessSubscriptionAndServiceRequest();
        setupDone = true;
      } catch (Exception e) {
        _log.error("Setup for client failed, exception is: " + e.getMessage());
        _log.error("Retrying in 60 seconds.");
          Thread.sleep(60 * 1000);
      }
    } while (!setupDone && attempts <= 4);
    if (setupDone) {
      _log.info("Setup for client mode complete.");
      return;
    }
    _log.error("*********************************************************************\n"
        + "Setup for client mode DID NOT COMPLETE SUCCESSFULLY AFTER 4 ATTEMPTS.\n"
        + "*********************************************************************");
  }

  @Override
  void addOrUpdateServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean,
      String defaultAgencyId) {
    getTransitDataService().createServiceAlert(defaultAgencyId,
        serviceAlertBean);
    result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
        "added");
  }

  @Override
  void removeServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, String serviceAlertId) {
    getTransitDataService().removeServiceAlert(serviceAlertId);
    result.countPtSituationElementResult(deliveryResult, serviceAlertId,
        "removed");
  }

  @Override
  List<String> getExistingAlertIds(Set<String> agencies) {
    List<String> alertIds = new ArrayList<String>();
    for (String agency : agencies) {
      ListBean<ServiceAlertBean> alerts = getTransitDataService().getAllServiceAlertsForAgencyId(
          agency);
      for (ServiceAlertBean alert : alerts.getList()) {
        alertIds.add(alert.getId());
      }
    }
    return alertIds;
  }

  @Override
  void postServiceDeliveryActions(SituationExchangeResults result,
      Collection<String> deletedIds) throws Exception {
    // None when in client mode
  }

  @Override
  void addSubscription(ServiceAlertSubscription subscription) {
    // not used in client mode
  }

  @Override
  public List<ServiceAlertSubscription> getActiveServiceAlertSubscriptions() {
    // not used in client mode
    return null;
  }

  @Override
  public SiriServicePersister getPersister() {
    // not used in client mode
    return null;
  }

  @Override
  public void setPersister(SiriServicePersister _siriServicePersister) {
    // not used in client mode
  }

  @Override
  public void deleteAllServiceAlerts() {
    for (AgencyWithCoverageBean agency : getTransitDataService().getAgenciesWithCoverage()) {
      _log.info("Clearing service alerts for agency "
          + agency.getAgency().getId());
      getTransitDataService().removeAllServiceAlertsForAgencyId(
          agency.getAgency().getId());
    }

  }

}
