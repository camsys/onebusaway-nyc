package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.Date;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingMutableDao;
import org.onebusaway.realtime.api.VehicleLocationListener;
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

  private VehicleLocationListener _vehicleLocationListener;

  private VehicleTrackingMutableDao _recordDao;

  private String _agencyId = "MTA NYCT";

  @Autowired
  public void setVehicleLocationInferenceService(
      VehicleLocationInferenceService service) {
    _vehicleLocationInferenceService = service;
  }

  @Autowired
  public void setVehicleLocationListener(
      VehicleLocationListener vehicleLocationListener) {
    _vehicleLocationListener = vehicleLocationListener;
  }

  @Autowired
  public void setRecordDao(VehicleTrackingMutableDao recordDao) {
    _recordDao = recordDao;
  }

  public void setAgencyId(String agencyId) {
    _agencyId = agencyId;
  }

  public String getDefaultVehicleAgencyId() {
    return _agencyId;
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
    if (location.Latitude != null) {
      record.setLatitude(location.Latitude);
      record.setLongitude(location.Longitude);
    }
    record.setTime(delivery.ResponseTimestamp.getTimeInMillis());
    record.setTimeReceived(new Date().getTime());

    if (vehicleActivity.Extensions != null
        && vehicleActivity.Extensions.NMEA != null && vehicleActivity.Extensions.NMEA.sentences != null) {
      for (String sentence : vehicleActivity.Extensions.NMEA.sentences) {
        if (sentence.startsWith("$GPRMC")) {
          record.setRmc(sentence);
        } else if (sentence.startsWith("$GPGGA")) {
          record.setGga(sentence);
        }
      }
    }
    String deviceId = monitoredVehicleJourney.FramedVehicleJourneyRef.DatedVehicleJourneyRef;
    record.setDeviceId(deviceId);

    handleRecord(record, false);
  }

  @Override
  public void handleVehicleLocation(long time, String vehicleId, double lat,
      double lon, String dsc, boolean saveResult) {

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setTime(time);
    record.setVehicleId(new AgencyAndId(_agencyId, vehicleId));
    record.setLatitude(lat);
    record.setLongitude(lon);
    record.setDestinationSignCode(dsc);

    handleRecord(record, saveResult);
  }

  @Override
  public void handleVehicleLocation(VehicleLocationRecord record) {
    _vehicleLocationListener.handleVehicleLocationRecord(record);
  }

  @Override
  public void resetVehicleLocation(String vehicleId) {
    AgencyAndId vid = new AgencyAndId(_agencyId, vehicleId);
    _vehicleLocationInferenceService.resetVehicleLocation(vid);
    _vehicleLocationListener.resetVehicleLocation(vid);
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

  @Override
  public List<Particle> getCurrentParticlesForVehicleId(String vehicleId) {
    return _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(new AgencyAndId(
        _agencyId, vehicleId));
  }

  /****
   * Private Methods
   ****/

  private void handleRecord(NycVehicleLocationRecord record, boolean saveResult) {
    _vehicleLocationInferenceService.handleVehicleLocation(record, saveResult);
    _recordDao.saveOrUpdateVehicleLocationRecord(record);
  }
}
