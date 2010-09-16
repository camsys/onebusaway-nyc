package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.realtime.BlockInstance;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocation;

public class VehicleInferenceInstance {

  private ParticleFilter<Observation> _particleFilter;

  public void setModel(ParticleFilterModel<Observation> model) {
    _particleFilter = new ParticleFilter<Observation>(model);
  }

  public synchronized void handleUpdate(NycVehicleLocationRecord record) {

    // If this record occurs BEFORE the most recent update, we ignore it
    if (record.getTime() < _particleFilter.getTimeOfLastUpdated())
      return;

    Observation observation = new Observation(record);

    _particleFilter.updateFilter(record.getTime(), observation);
  }

  public synchronized VehicleLocationRecord getCurrentState() {

    Particle particle = _particleFilter.getMostLikelyParticle();
    VehicleState state = particle.getData();

    ProjectedPoint p = state.getEdgeState().getPointOnEdge();

    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setCurrentLocationLat(p.getLat());
    record.setCurrentLocationLon(p.getLon());

    record.setTimeOfRecord((long) particle.getTimestamp());
    // record.setPositionDeviation(state.getPositionDeviation());

    BlockState blockState = state.getBlockState();

    if (blockState != null) {
      BlockInstance blockInstance = blockState.getBlockInstance();
      record.setBlockId(blockInstance.getBlock().getId());
      record.setServiceDate(blockInstance.getServiceDate());
      
      ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
      record.setDistanceAlongBlock(blockLocation.getDistanceAlongBlock());
    }

    return record;
  }

  public synchronized List<Particle> getParticles() {
    return new ArrayList<Particle>(_particleFilter.getWeightedParticles());
  }
}
