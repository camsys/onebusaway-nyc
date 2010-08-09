package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
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

  private String _agencyId = "2008";

  @Autowired
  public void setVehicleLocationInferenceService(
      VehicleLocationInferenceService service) {
    _vehicleLocationInferenceService = service;
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

    VehicleLocationInferenceRecord record = new VehicleLocationInferenceRecord();
    record.setVehicleId(new AgencyAndId(_agencyId,
        monitoredVehicleJourney.VehicleRef));
    record.setDestinationSignCode(vehicleActivity.VehicleMonitoringRef);
    record.setLat(location.Latitude);
    record.setLon(location.Longitude);
    record.setTimestamp(delivery.ResponseTimestamp.getTimeInMillis());
    _vehicleLocationInferenceService.handleVehicleLocation(record);
  }

  @Override
  public void handleVehicleLocation(String vehicleId, double lat, double lon,
      String dsc) {

    VehicleLocationInferenceRecord record = new VehicleLocationInferenceRecord();
    record.setVehicleId(new AgencyAndId(_agencyId, vehicleId));
    record.setLat(lat);
    record.setLon(lon);
    record.setDestinationSignCode(dsc);

    _vehicleLocationInferenceService.handleVehicleLocation(record);
  }

  @Override
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords() {
    return _vehicleLocationInferenceService.getLatestProcessedVehicleLocationRecords();
  }

}
