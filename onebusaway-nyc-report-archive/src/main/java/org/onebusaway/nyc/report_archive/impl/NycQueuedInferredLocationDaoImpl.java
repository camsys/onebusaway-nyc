package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.InferredLocationRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public
class NycQueuedInferredLocationDaoImpl implements NycQueuedInferredLocationDao {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Override
  public void saveOrUpdateRecord(ArchivedInferredLocationRecord record) {
    _template.saveOrUpdate(record);

    InferredLocationRecord currentRecord = new InferredLocationRecord(record);

    _template.saveOrUpdate(currentRecord);
  }

  public void saveOrUpdateRecords(ArchivedInferredLocationRecord... records) {
    List<ArchivedInferredLocationRecord> list = new ArrayList<ArchivedInferredLocationRecord>(
        records.length);
    for (ArchivedInferredLocationRecord record : records)
      list.add(record);
    _template.saveOrUpdateAll(list);
  }

  @Override
  public List<ArchivedInferredLocationRecord> getAllLastKnownRecords() {
    
    List<ArchivedInferredLocationRecord> firstArchivedRecord = new ArrayList<ArchivedInferredLocationRecord>();
    
    return firstArchivedRecord;
  }
}