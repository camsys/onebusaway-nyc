package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class DestinationSignCodeServiceImpl implements DestinationSignCodeService {

  private static Logger _log = LoggerFactory.getLogger(DestinationSignCodeServiceImpl.class);

  private VehicleTrackingDao _dao;

  @Autowired
  public void setDao(VehicleTrackingDao dao) {
    _dao = dao;
  }

  @Override
  @Cacheable
  public List<AgencyAndId> getTripIdsForDestinationSignCode(
      String destinationSignCode) {
    List<DestinationSignCodeRecord> records = _dao.getDestinationSignCodeRecordsForDestinationSignCode(destinationSignCode);
    return MappingLibrary.map(records, "tripId");
  }

  @Override
  @Cacheable
  public String getDestinationSignCodeForTripId(AgencyAndId tripId) {
    List<DestinationSignCodeRecord> records = _dao.getDestinationSignCodeRecordsForTripId(tripId);
    if (records.size() == 0)
      return null;
    else if (records.size() == 1)
      return records.get(0).getDestinationSignCode();
    else {
      _log.warn("multiple destination sign codes found for tripId=" + tripId);
      return records.get(0).getDestinationSignCode();
    }
  }

  @Override
  @Cacheable
  public boolean isOutOfServiceDestinationSignCode(String destinationSignCode) {
    List<DestinationSignCodeRecord> records = _dao.getOutOfServiceDestinationSignCodeRecords();
    for (DestinationSignCodeRecord record : records) {
      if (record.getDestinationSignCode().equals(destinationSignCode))
        return true;
    }
    return false;
  }

  @Override
  @Cacheable
  public boolean isUnknownDestinationSignCode(String destinationSignCode) {
    List<DestinationSignCodeRecord> records = _dao.getAnyDestinationSignCodeRecordsForDestinationSignCode(destinationSignCode);
    return records.isEmpty();
  }
}
