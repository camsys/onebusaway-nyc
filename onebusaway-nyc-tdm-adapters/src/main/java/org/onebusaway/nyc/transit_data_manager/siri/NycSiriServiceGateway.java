/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

public class NycSiriServiceGateway extends NycSiriService {

  @Autowired
  private SiriServicePersister _siriServicePersister;
  @Autowired
  private ServiceAlertsPersister _serviceAlertsPersister;

  @Override
  void setupForMode() throws Exception, JAXBException {
    _log.info("No setup required in gateway mode.");
  }

  @Override
  void addOrUpdateServiceAlert(Map<String, List<ServiceAlertBean>> agencyIdToServiceAlerts,
                               SituationExchangeResults result,
                               DeliveryResult deliveryResult, ServiceAlertBean serviceAlertBean,
                               String defaultAgencyId) {

    if (defaultAgencyId == null) {
      if (serviceAlertBean != null && serviceAlertBean.getId() != null) {
        try {
          AgencyAndId serviceAlertId = AgencyAndIdLibrary.convertFromString(serviceAlertBean.getId());
          defaultAgencyId = serviceAlertId.getAgencyId();
        } catch (IllegalStateException ise) {
          // bury
        }
      }
    }
    try {
      boolean isNew = getPersister().saveOrUpdateServiceAlert(serviceAlertBean);
      if (!agencyIdToServiceAlerts.containsKey(defaultAgencyId)) {
        agencyIdToServiceAlerts.put(defaultAgencyId, new ArrayList<>());
      }
      agencyIdToServiceAlerts.get(defaultAgencyId).add(serviceAlertBean);
      result.countPtSituationElementResult(deliveryResult, serviceAlertBean,
              (isNew ? "added" : "updated"));
    } catch (Throwable t) {
      _log.error("service alert " + serviceAlertBean + " failed on save");
    }
  }

  void removeServiceAlert(SituationExchangeResults result,
      DeliveryResult deliveryResult, String serviceAlertId) {
    result.countPtSituationElementResult(deliveryResult, serviceAlertId,
        "removed");
    try {
      // delete from siri tables
      getPersister().deleteServiceAlertById(serviceAlertId);
    } catch (Exception t) {
      _log.error("exception deleting siri alert: ", t, t);
    }
    try {
      // also delete from app tables
      getServiceAlertsPersister().deleteServiceAlertById(serviceAlertId);
    } catch (Exception t) {
      _log.error("exception delting service alert: ", t, t);
    }


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

  public ServiceAlertsPersister getServiceAlertsPersister() {
    return _serviceAlertsPersister;
  }


  public void setPersister(SiriServicePersister _siriServicePersister) {
    this._siriServicePersister = _siriServicePersister;
  }

  public void setServiceAlertsPersister(ServiceAlertsPersister persister) {
    this._serviceAlertsPersister = persister;
  }

  @Override
  public void deleteAllServiceAlerts() {
    // noop for gateway mode
  }
  
}
