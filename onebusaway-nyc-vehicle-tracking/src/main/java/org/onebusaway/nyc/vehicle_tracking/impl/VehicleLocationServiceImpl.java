package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingMutableDao;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.siri.model.VehicleLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class VehicleLocationServiceImpl implements VehicleLocationService {

  private VehicleLocationInferenceService _vehicleLocationInferenceService;

  private VehicleTrackingMutableDao _recordDao;

  private String _agencyId = "2008";

  @Autowired
  public void setVehicleLocationInferenceService(
      VehicleLocationInferenceService service) {
    _vehicleLocationInferenceService = service;
  }

  @Autowired
  public void setRecordDao(VehicleTrackingMutableDao recordDao) {
    _recordDao = recordDao;
  }

  public void setAgencyId(String agencyId) {
    _agencyId = agencyId;
  }

  @Override
  public void handleVehicleLocation(Siri siri) {
    ServiceDelivery delivery = siri.ServiceDelivery;
    VehicleActivity vehicleActivity = delivery.VehicleMonitoringDelivery.deliveries.get(0);
    MonitoredVehicleJourney monitoredVehicleJourney = vehicleActivity.MonitoredVehicleJourney;
    VehicleLocation location = monitoredVehicleJourney.VehicleLocation;

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setVehicleId(new AgencyAndId(_agencyId,
        monitoredVehicleJourney.VehicleRef));
    record.setDestinationSignCode(vehicleActivity.VehicleMonitoringRef);
    record.setLatitude(location.Latitude);
    record.setLongitude(location.Longitude);
    record.setTime(delivery.ResponseTimestamp.getTimeInMillis());

    handleRecord(record);
  }

  @Override
  public void handleVehicleLocation(long time, String vehicleId, double lat,
      double lon, String dsc) {

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setTime(time);
    record.setVehicleId(new AgencyAndId(_agencyId, vehicleId));
    record.setLatitude(lat);
    record.setLongitude(lon);
    record.setDestinationSignCode(dsc);

    handleRecord(record);
  }

  @Override
  public void resetVehicleLocation(String vehicleId) {
    AgencyAndId vid = new AgencyAndId(_agencyId, vehicleId);
    _vehicleLocationInferenceService.resetVehicleLocation(vid);
  }

  @Override
  public VehicleLocationRecord getVehicleLocationForVehicle(String vehicleId) {
    AgencyAndId vid = new AgencyAndId(_agencyId, vehicleId);
    return _vehicleLocationInferenceService.getVehicleLocationForVehicle(vid);
  }

  @Override
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords() {
    return _vehicleLocationInferenceService.getLatestProcessedVehicleLocationRecords();
  }

  /****
   * Private Methods
   ****/

  private void handleRecord(NycVehicleLocationRecord record) {
    _vehicleLocationInferenceService.handleVehicleLocation(record);
    _recordDao.saveOrUpdateVehicleLocationRecord(record);
  }
}
