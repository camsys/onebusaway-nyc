package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

public class SiriServiceDao implements SiriServicePersister {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    _template.saveOrUpdate(new ServiceAlertRecord(serviceAlertBean));
  }

  @Override
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    ServiceAlertRecord record = getServiceAlertByServiceAlertId(serviceAlertId);
    record.setDeleted(true);
    _template.saveOrUpdate(record);
    return ServiceAlertRecord.toBean(record);
  }

  private ServiceAlertRecord getServiceAlertByServiceAlertId(String serviceAlertId) {
    List<ServiceAlertRecord> list = _template.find("from ServiceAlertRecord where service_alert_id=?", serviceAlertId);
    return list.get(0);
  }

  @Override
  public List<ServiceAlertBean> getAllActiveServiceAlerts() {
    return getServiceAlerts("from ServiceAlertRecord r where r.deleted = false");
  }

  @Override
  public List<ServiceAlertBean> getAllServiceAlerts() {
    return getServiceAlerts("from ServiceAlertRecord");
  }

  private List<ServiceAlertBean> getServiceAlerts(String hsql) {
    List<ServiceAlertBean> results = new ArrayList<ServiceAlertBean>();
    String hql = hsql;
    List<Object> list = _template.find(hql);
    for (Object o : list) {
      ServiceAlertBean b = ServiceAlertRecord.toBean((ServiceAlertRecord)o);
      results.add(b);
    }
    return results;
  }

  @Override
  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteSubscription(ServiceAlertSubscription subscription) {
    // TODO Auto-generated method stub

  }

  @Override
  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    // TODO Auto-generated method stub

    // ********** NOT DONE ***************
    // ********** NOT DONE ***************
    // ********** NOT DONE ***************
    // ********** NOT DONE ***************
    // ********** NOT DONE ***************

    return new ArrayList<ServiceAlertSubscription>();
  }

}
