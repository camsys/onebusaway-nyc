package org.onebusaway.nyc.report_archive.impl;

import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcAndInferredLocationRecord;
import org.onebusaway.nyc.report_archive.model.CcLocationReportRecord;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class NycQueuedInferredLocationDaoImpl implements
    NycQueuedInferredLocationDao {

  protected static Logger _log = LoggerFactory.getLogger(NycQueuedInferredLocationDaoImpl.class);

  private HibernateTemplate _template;

  @Autowired
  private CcLocationCache _ccLocationCache;

  public void setCcLocationCache(CcLocationCache cache) {
    _ccLocationCache = cache;
  }

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void saveOrUpdateRecord(ArchivedInferredLocationRecord record) {
    _template.saveOrUpdate(record);

    CcLocationReportRecord cc = findRealtimeRecord(record);
    if (cc != null) {
      CcAndInferredLocationRecord lastKnown = new CcAndInferredLocationRecord(
          record, cc);
      _template.saveOrUpdate(lastKnown);
    }

    _template.flush();
    _template.clear();
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void saveOrUpdateRecords(ArchivedInferredLocationRecord... records) {
    List<ArchivedInferredLocationRecord> list = new ArrayList<ArchivedInferredLocationRecord>(
        records.length);
    for (ArchivedInferredLocationRecord record : records)
      list.add(record);
    _template.saveOrUpdateAll(list);

    // LastKnownRecord
    LinkedHashMap<Integer, CcAndInferredLocationRecord> lastKnownRecords = new LinkedHashMap<Integer, CcAndInferredLocationRecord>(
        records.length);
    for (ArchivedInferredLocationRecord record : records) {
      CcLocationReportRecord cc = findRealtimeRecord(record);
      if (cc != null) {
        CcAndInferredLocationRecord lastKnown = new CcAndInferredLocationRecord(
            record, cc);
        lastKnownRecords.put(lastKnown.getVehicleId(), lastKnown);
      }
    }
    _template.saveOrUpdateAll(lastKnownRecords.values());
    _template.flush();
    _template.clear();
  }

  private CcLocationReportRecord findRealtimeRecord(
      ArchivedInferredLocationRecord record) {
    // first check cache for realtime record
    CcLocationReportRecord realtime = _ccLocationCache.get(record.getUUID());

    // if not in cache log cache miss
    if (realtime == null) {
      /*
       * NOTE: db is NOT queried for lost record for
       * performance reasons.  Assume queue has fallen
       * behind and incoming update will correct this.
       */
      _log.info("cache miss for " + record.getVehicleId());
    }
    return realtime;
  }

  @Override
  public List<CcAndInferredLocationRecord> getAllLastKnownRecords() {

    String hql = "from CcAndInferredLocationRecord " + "order by vehicleId";

    @SuppressWarnings("unchecked")
    List<CcAndInferredLocationRecord> list = _template.find(hql);
    // our join will return a list of object arrays now, in the order
    // we selected above

    return list;
  }

  @Override
  public CcAndInferredLocationRecord getLastKnownRecordForVehicle(
      Integer vehicleId) throws Exception {

    if (vehicleId == null) {
      return null;
    }

    return _template.get(CcAndInferredLocationRecord.class, vehicleId);
  }

}