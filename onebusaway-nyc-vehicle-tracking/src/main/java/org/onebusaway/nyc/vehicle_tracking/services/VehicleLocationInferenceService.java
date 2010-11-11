package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;

public interface VehicleLocationInferenceService {

  public void handleVehicleLocation(NycVehicleLocationRecord record,
      boolean saveResult);

  public void resetVehicleLocation(AgencyAndId vid);

  public VehicleLocationRecord getVehicleLocationForVehicle(AgencyAndId vid);

  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords();

  public VehicleLocationManagementRecord getVehicleLocationManagementRecordForVehicle(
      AgencyAndId vid);

  public List<VehicleLocationManagementRecord> getVehicleLocationManagementRecords();

  public void setVehicleStatus(AgencyAndId vid, boolean enabled);

  /**
   * This is primarily here for debugging
   * 
   * @param vehicleId
   * @return the most recent list of particles for the specified vehicle
   */
  public List<Particle> getCurrentParticlesForVehicleId(AgencyAndId vehicleId);
}
