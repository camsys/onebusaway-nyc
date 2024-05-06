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

import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.services.beans.ServiceAlertsBeanService;
import org.onebusaway.util.AgencyAndIdLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Encapsulate app-mods service alert operations
 */
public class ServiceAlertsPersisterImpl implements ServiceAlertsPersister {

  @Autowired
  private ServiceAlertsBeanService _service;

  public void setServiceAlertsBeanService(ServiceAlertsBeanService service) {
    _service = service;
  }
  static final Logger _log = LoggerFactory.getLogger(ServiceAlertsPersisterImpl.class);
  @Override
  public boolean saveOrUpdateServiceAlert(String defaultAgencyId, ServiceAlertBean serviceAlertBean) {
    try {
      _service.createServiceAlert(defaultAgencyId, serviceAlertBean);
    } catch (Throwable t) {
      _log.error("exception saving sevice alert " + serviceAlertBean + " ", t ,t);
      return false;
    }
    return true;
  }

  @Override
  public boolean saveOrUpdateServiceAlerts(String defaultAgencyId, List<ServiceAlertBean> serviceAlertBeans) {
    try {
      _service.createServiceAlerts(defaultAgencyId, serviceAlertBeans);
      _service.sync();
    } catch (Throwable t) {
      _log.error("exception saving sevice alerts " + serviceAlertBeans + " ", t ,t);
      return false;
    }
    return true;
  }

  @Override
  public void deleteServiceAlertById(String serviceAlertId) {
    _service.removeServiceAlert(AgencyAndIdLibrary.convertFromString(serviceAlertId));
  }

  @Override
  public boolean deleteOrphans() {
    return _service.deleteOrphans();
  }


}
