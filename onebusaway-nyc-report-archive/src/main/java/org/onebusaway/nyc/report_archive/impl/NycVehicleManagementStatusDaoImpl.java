package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.NycVehicleManagementStatusRecord;
import org.onebusaway.nyc.report_archive.services.NycVehicleManagementStatusDao;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public
class NycVehicleManagementStatusDaoImpl implements NycVehicleManagementStatusDao {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Transactional(rollbackFor=Throwable.class)
  @Override
  public void saveOrUpdateRecord(NycVehicleManagementStatusRecord record) {
    _template.saveOrUpdate(record);
    _template.evict(record);
  }

}