package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

@Component
public class VehicleTrackingDaoImpl implements VehicleTrackingDao {

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  @Override
  public void saveOrUpdateDestinationSignCodeRecord(
      DestinationSignCodeRecord record) {
    _template.saveOrUpdate(record);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForDestinationSignCode(
      String destinationSignCode) {

    return _template.findByNamedQueryAndNamedParam(
        "destinationSignCodeRecordsForDestinationSignCode",
        "destinationSignCode", destinationSignCode);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DestinationSignCodeRecord> getDestinationSignCodeRecordsForTripId(
      AgencyAndId tripId) {

    return _template.findByNamedQueryAndNamedParam(
        "destinationSignCodeRecordsForTripId", "tripId", tripId);
  }
}
