package org.onebusaway.nyc.vehicle_tracking.model;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.simulator.VehicleLocationDetails;

import com.google.common.collect.Multiset;

public class InferenceOutputState {
  private NycQueuedInferredLocationBean _nycQueuedInferredLocationBean;
  private NycTestInferredLocationRecord _nycTestInferredLocationRecord;
  private AgencyAndId _vehicleId;
  private Multiset<Particle> _currentParticles;
  private Multiset<Particle> _currentSampledParticles;
  private List<JourneyPhaseSummary> _journeySummaries;
  private VehicleLocationDetails _details;
  private VehicleLocationDetails _badParticleDetails;
  
  public void setNycQueuedInferredLocationBean(NycQueuedInferredLocationBean record) {
    this._nycQueuedInferredLocationBean = record;
  }
  public NycQueuedInferredLocationBean getNycQueuedInferredLocationBean() {
    return _nycQueuedInferredLocationBean;
  }
  public void setNycTestInferredLocationRecord(NycTestInferredLocationRecord currentState) {
    _nycTestInferredLocationRecord = currentState;
  }
  public NycTestInferredLocationRecord getNycTestInferredLocationRecord() {
    return _nycTestInferredLocationRecord;
  }
  public void setVehicleId(AgencyAndId vehicleId) {
    _vehicleId = vehicleId;
  }
  public AgencyAndId getVehicleId() {
    return _vehicleId;
  }
  public void setCurrentParticle(Multiset<Particle> currentParticles) {
    _currentParticles = currentParticles;
  }
  public Multiset<Particle> getCurrentParticles() {
    return _currentParticles;
  }
  public void setCurrentSampledParticles(
      Multiset<Particle> currentSampledParticles) {
    _currentSampledParticles = currentSampledParticles;
  }
  public Multiset<Particle> getCurrentSampledParticles() {
    return _currentSampledParticles;
  }
  public void setCurrentJourneySummaries(
      List<JourneyPhaseSummary> journeySummaries) {
    _journeySummaries = journeySummaries;
  }
  public List<JourneyPhaseSummary> getCurrentJourneySummaries() {
    return _journeySummaries;
  }
  public void setVehicleLocationDetails(VehicleLocationDetails details) {
    _details = details;
  }
  public VehicleLocationDetails getVehicleLocationDetails() {
    return _details;
  }
  public void setBadParticleDetails(VehicleLocationDetails badParticleDetails) {
    _badParticleDetails = badParticleDetails;
  }
  public VehicleLocationDetails getBadParticleDetails() {
    return _badParticleDetails;
  }
  
}
