package org.onebusaway.nyc.vehicle_tracking.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationDetails;
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
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class VehicleLocationServiceImpl implements VehicleLocationService {

  private static final int THREE_HOURS = 3 * 60 * 60 * 1000;

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
  public void handleVehicleLocation(Siri siri, String body) {
    ServiceDelivery delivery = siri.ServiceDelivery;
    VehicleActivity vehicleActivity = delivery.VehicleMonitoringDelivery.deliveries.get(0);
    MonitoredVehicleJourney monitoredVehicleJourney = vehicleActivity.MonitoredVehicleJourney;
    VehicleLocation location = monitoredVehicleJourney.VehicleLocation;

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setRawData(body);
    record.setVehicleId(new AgencyAndId(_agencyId,
        monitoredVehicleJourney.VehicleRef));
    record.setDestinationSignCode(vehicleActivity.VehicleMonitoringRef);
    if (location.Latitude != null) {
      record.setLatitude(location.Latitude);
      record.setLongitude(location.Longitude);
    }
    /**
     * TODO : This is a hack to deal with the lack of timezone info in SIRI
     * records
     */
    long time = delivery.ResponseTimestamp.getTimeInMillis();
    long delta = Math.abs(System.currentTimeMillis() + THREE_HOURS - time);
    if (delta < 60 * 1000)
      time -= THREE_HOURS;
    record.setTime(time);
    record.setTimeReceived(new Date().getTime());

    if (vehicleActivity.Extensions != null
        && vehicleActivity.Extensions.NMEA != null
        && vehicleActivity.Extensions.NMEA.sentences != null) {
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
    record.setTimeReceived(time);
    record.setVehicleId(new AgencyAndId(_agencyId, vehicleId));
    record.setLatitude(lat);
    record.setLongitude(lon);
    record.setDestinationSignCode(dsc);

    handleRecord(record, saveResult);
  }

  @Override
  public void handleVehicleLocation(VehicleLocationRecord record) {
    _vehicleLocationInferenceService.handleVehicleLocationRecord(record);
  }

  @Override
  public void handleNycTestLocationRecord(NycTestLocationRecord record) {
    AgencyAndId vid = new AgencyAndId(_agencyId, record.getVehicleId());
    _vehicleLocationInferenceService.handleNycTestLocationRecord(vid, record);
  }

  @Override
  public void resetVehicleLocation(String vehicleId) {
    AgencyAndId vid = getVehicleId(vehicleId);
    _vehicleLocationInferenceService.resetVehicleLocation(vid);
    _vehicleLocationListener.resetVehicleLocation(vid);
  }

  @Override
  public VehicleLocationRecord getVehicleLocationForVehicle(String vehicleId) {
    AgencyAndId vid = getVehicleId(vehicleId);
    return _vehicleLocationInferenceService.getVehicleLocationForVehicle(vid);
  }

  @Override
  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords() {
    return _vehicleLocationInferenceService.getLatestProcessedVehicleLocationRecords();
  }

  @Override
  public List<Particle> getCurrentParticlesForVehicleId(String vehicleId) {
    return _vehicleLocationInferenceService.getCurrentParticlesForVehicleId(getVehicleId(vehicleId));
  }

  @Override
  public VehicleLocationDetails getDetailsForVehicleId(String vehicleId) {
    return _vehicleLocationInferenceService.getDetailsForVehicleId(getVehicleId(vehicleId));
  }

  @Override
  public VehicleLocationDetails getParticleDetails(String vehicleId,
      int particleId) {

    VehicleLocationDetails details = getDetailsForVehicleId(vehicleId);
    if (details == null)
      return null;
    List<Particle> particles = details.getParticles();
    if (particles != null) {
      for (Particle p : particles) {
        if (p.getIndex() == particleId) {
          List<Particle> history = new ArrayList<Particle>();
          while (p != null) {
            history.add(p);
            p = p.getParent();
          }
          details.setParticles(history);
          return details;
        }
      }
    }
    return null;
  }

  @Override
  public VehicleLocationManagementRecord getVehicleLocationManagementRecordForVehicle(
      String vehicleId) {
    AgencyAndId vid = getVehicleId(vehicleId);
    return _vehicleLocationInferenceService.getVehicleLocationManagementRecordForVehicle(vid);
  }

  @Override
  public List<VehicleLocationManagementRecord> getVehicleLocationManagementRecords() {
    return _vehicleLocationInferenceService.getVehicleLocationManagementRecords();
  }

  @Override
  public void setVehicleStatus(String vehicleId, boolean enabled) {
    AgencyAndId vid = getVehicleId(vehicleId);
    _vehicleLocationInferenceService.setVehicleStatus(vid, enabled);
    if (!enabled)
      _vehicleLocationListener.resetVehicleLocation(vid);
  }

  /****
   * Private Methods
   ****/

  private void handleRecord(NycVehicleLocationRecord record, boolean saveResult) {
    _vehicleLocationInferenceService.handleNycVehicleLocationRecord(record);
    _recordDao.saveOrUpdateVehicleLocationRecord(record);
  }

  private AgencyAndId getVehicleId(String vehicleId) {
    if (vehicleId.startsWith(_agencyId))
      return AgencyAndIdLibrary.convertFromString(vehicleId);
    return new AgencyAndId(_agencyId, vehicleId);
  }

}
