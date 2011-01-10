package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

public class VehicleLocationDetails {

  private String id;
  
  private String vehicleId;

  private NycVehicleLocationRecord lastObservation;

  private List<Particle> particles;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public NycVehicleLocationRecord getLastObservation() {
    return lastObservation;
  }

  public void setLastObservation(NycVehicleLocationRecord lastObservation) {
    this.lastObservation = lastObservation;
  }

  public List<Particle> getParticles() {
    return particles;
  }

  public void setParticles(List<Particle> particles) {
    this.particles = particles;
  }
}
