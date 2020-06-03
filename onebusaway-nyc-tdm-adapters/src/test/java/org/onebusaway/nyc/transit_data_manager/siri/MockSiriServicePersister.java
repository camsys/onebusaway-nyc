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
import java.util.HashMap;
import java.util.List;

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

public class MockSiriServicePersister extends HashMap<String, ServiceAlertBean> implements
    SiriServicePersister {

  private static final long serialVersionUID = 1L;
  
  List<ServiceAlertSubscription> subs = new ArrayList<ServiceAlertSubscription>();
//  Map<String, ServiceAlertBean> alerts = new HashMap<String, ServiceAlertBean>();
  
  public boolean saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    throw new RuntimeException("not implemented");
  }

  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    throw new RuntimeException("not implemented");
  }

  public List<ServiceAlertBean> getAllActiveServiceAlerts() {
    return new ArrayList<ServiceAlertBean>(super.values());
  }

  public List<ServiceAlertBean> getAllServiceAlerts() {
    throw new RuntimeException("not implemented");
  }

  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription) {
    subs.add(subscription);
  }

  public void deleteSubscription(ServiceAlertSubscription subscription) {
    subs.remove(subscription);
  }

  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    return subs;
  }

}