package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

public class SiriServiceDao implements SiriServicePersister {

  private static final Logger _log = LoggerFactory.getLogger(SiriServiceDao.class);

  private HibernateTemplate _template;

  @Autowired
  @Qualifier("appSessionFactory")
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public boolean saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    boolean isNew = false;
    ServiceAlertRecord record = getServiceAlertByServiceAlertId(serviceAlertBean.getId());
    if (record != null) {
      record.setDeleted(false);
      _template.saveOrUpdate(record.updateFrom(serviceAlertBean));
    } else {
      _template.saveOrUpdate(new ServiceAlertRecord(serviceAlertBean));
      isNew = true;
    }
    return isNew;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    ServiceAlertRecord record = getServiceAlertByServiceAlertId(serviceAlertId);
    if (record == null)
      return null;
    record.setDeleted(true);
    _template.saveOrUpdate(record);
    return ServiceAlertRecord.toBean(record);
  }

  private ServiceAlertRecord getServiceAlertByServiceAlertId(
      String serviceAlertId) {
    List<ServiceAlertRecord> list = _template.find(
        "from ServiceAlertRecord where service_alert_id=?", serviceAlertId);
    if (list.size() > 0)
      return list.get(0);
    else
      return null;
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
    _log.info("Ran query: " + hql + " size of results " + list.size());
    for (Object o : list) {
      ServiceAlertBean b = ServiceAlertRecord.toBean((ServiceAlertRecord) o);
      results.add(b);
    }
    return results;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription) {
    // This is not the most efficient way to do this.  FIXME
    List<ServiceAlertSubscription> list = _template.find(
        "from ServiceAlertSubscription where address=?", subscription.getAddress());
    if (list.size() > 0) {
      _log.info("Subscription already exists, updating.");
      ServiceAlertSubscription old = list.get(0);
      old.updateFrom(subscription);
      _template.saveOrUpdate(old);
      return;
    }

    _log.info("Saving new subscription.");
    _template.saveOrUpdate(subscription);
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void deleteSubscription(ServiceAlertSubscription subscription) {
    _template.delete(subscription);
    _template.flush();
  }

  @Override
  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    return (List<ServiceAlertSubscription>) _template.find("from ServiceAlertSubscription");
  }

}
