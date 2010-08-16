package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.NycVehicleLocationRecordDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Component
public class NycVehicleLocationRecordDaoImpl implements
    NycVehicleLocationRecordDao {

  private HibernateTemplate _template;

  /**
   * Note that we are using the "mutable" SessionFactory, so that we can write
   * trip records to a potentially different database than the mostly read-only
   * transit data source.
   * 
   * @param sessionFactory
   */
  @Autowired
  public void setSessionFactory(
      @Qualifier("mutable") SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  @Override
  public void saveOrUpdateRecord(NycVehicleLocationRecord record) {
    _template.saveOrUpdate(record);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<NycVehicleLocationRecord> getRecordsForTimeRange(long timeFrom,
      long timeTo) {
    String[] names = {"timeFrom", "timeTo"};
    Object[] values = {timeFrom, timeTo};
    return _template.findByNamedQueryAndNamedParam("recordsForTimeRange",
        names, values);
  }
}
