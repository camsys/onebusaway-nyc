package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.transaction.annotation.Transactional;

public class SiriServiceDao implements SiriServicePersister {

  private static final Logger _log = LoggerFactory.getLogger(SiriServiceDao.class);

  private SessionFactory _sessionFactory;

  @Autowired
  @Qualifier("appSessionFactory")
  public void setSessionFactory(SessionFactory sessionFactory) {
    _sessionFactory = sessionFactory;
  }

  public Session getSession() {
    return _sessionFactory.getCurrentSession();
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public boolean saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    boolean isNew = false;
    List<ServiceAlertRecord> records = getServiceAlertsByServiceAlertId(serviceAlertBean.getId());
    if (records.size() > 0) {
      ServiceAlertRecord record = records.get(0);
      record.setUpdatedAt(new Date());
      record.setDeleted(false);
      getSession().saveOrUpdate(record.updateFrom(serviceAlertBean));
    } else {
      ServiceAlertRecord newRecord = new ServiceAlertRecord(serviceAlertBean);
      newRecord.setUpdatedAt(new Date());
      newRecord.setCreatedAt(new Date());
      getSession().saveOrUpdate(newRecord);
      isNew = true;
    }
    return isNew;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    List<ServiceAlertRecord> records = getServiceAlertsByServiceAlertId(serviceAlertId);

    if (records.size() == 0)
      return null;

    for(Iterator<ServiceAlertRecord> it = records.iterator(); it.hasNext();){
      ServiceAlertRecord nextRecord = it.next();
      nextRecord.setDeleted(true);
      nextRecord.setUpdatedAt(new Date());
      getSession().saveOrUpdate(nextRecord);
    }

    return ServiceAlertRecord.toBean(records.get(0));
  }

  private List<ServiceAlertRecord> getServiceAlertsByServiceAlertId(
      String serviceAlertId) {
    Query query = getSession().createQuery(
        "FROM ServiceAlertRecord where service_alert_id = :serviceAlertId");
    query.setString("serviceAlertId", serviceAlertId);
    return query.list();
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
    Query query = getSession().createQuery(hql);
    List<Object> list = query.list();
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
    Query query = getSession().createQuery("from ServiceAlertSubscription where address = :address");
    query.setString("address", subscription.getAddress());
    List<ServiceAlertSubscription> list = query.list();
    if (list.size() > 0) {
      _log.info("Subscription already exists, updating.");
      ServiceAlertSubscription old = list.get(0);
      old.updateFrom(subscription);
      getSession().saveOrUpdate(old);
      return;
    }
    _log.info("Saving new subscription.");
    getSession().saveOrUpdate(subscription);
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void deleteSubscription(ServiceAlertSubscription subscription) {
    getSession().delete(subscription);
    getSession().flush();
  }

  @Override
  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    Query query = getSession().createQuery("from ServiceAlertSubscription");
    return (List<ServiceAlertSubscription>) query.list();
  }

}
