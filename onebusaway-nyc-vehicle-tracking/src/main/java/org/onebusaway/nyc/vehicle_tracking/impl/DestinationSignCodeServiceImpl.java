package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.DestinationSignCodeRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class DestinationSignCodeServiceImpl implements DestinationSignCodeService {

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
  public String getDestinationSignCodeForTripId(AgencyAndId tripId) {
    List<DestinationSignCodeRecord> records = _dao.getDestinationSignCodeRecordsForTripId(tripId);
    if (records.size() == 0)
      return null;
    else if (records.size() == 1)
      return records.get(0).getDestinationSignCode();
    else
      throw new IllegalStateException(
          "multiple destination sign codes found for tripId=" + tripId);
  }
}
