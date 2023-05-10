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


}
