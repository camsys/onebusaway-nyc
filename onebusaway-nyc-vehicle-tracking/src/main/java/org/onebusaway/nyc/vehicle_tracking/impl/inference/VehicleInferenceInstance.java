package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingConfigurationService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.springframework.beans.factory.annotation.Autowired;

public class VehicleInferenceInstance {

  private ParticleFilter<Observation> _particleFilter;

  private VehicleTrackingConfigurationService _configurationService;

  private NycVehicleLocationRecord _previousRecord = null;

  public void setModel(ParticleFilterModel<Observation> model) {
    _particleFilter = new ParticleFilter<Observation>(model);
  }

  @Autowired
  public void setVehicleTrackingConfigurationService(
      VehicleTrackingConfigurationService configurationService) {
    _configurationService = configurationService;
  }

  public synchronized void handleUpdate(NycVehicleLocationRecord record,
      boolean saveResult) {

    // If this record occurs BEFORE the most recent update, we ignore it
    if (record.getTime() < _particleFilter.getTimeOfLastUpdated())
      return;

    /**
     * Recall that a vehicle might send a location update with missing lat-lon
     * if it's sitting at the curb with the engine turned off.
     */
    boolean latlonMissing = record.locationDataIsMissing();

    if (latlonMissing) {

      /**
       * If we don't have a previous record, we can't use the previous lat-lon
       * to replace the missing values
       */
      if (_previousRecord == null)
        return;

      record.setLatitude(_previousRecord.getLatitude());
      record.setLongitude(_previousRecord.getLongitude());
    }

    Observation observation = new Observation(record, _previousRecord);
    _previousRecord = record;

    _particleFilter.updateFilter(record.getTime(), observation);
  }

  public synchronized VehicleLocationRecord getCurrentState() {

    Particle particle = _particleFilter.getMostLikelyParticle();

    VehicleState state = particle.getData();
    EdgeState edgeState = state.getEdgeState();
    MotionState motionState = state.getMotionState();
    JourneyState journeyState = state.getJourneyState();
    BlockState blockState = state.getBlockState();

    CoordinatePoint edgeLocation = edgeState.getLocationOnEdge();

    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setCurrentLocationLat(edgeLocation.getLat());
    record.setCurrentLocationLon(edgeLocation.getLon());

    record.setTimeOfRecord((long) particle.getTimestamp());

    EVehiclePhase phase = journeyState.getPhase();
    record.setPhase(phase);

    Set<String> statusFields = new HashSet<String>();

    if (blockState != null) {

      BlockInstance blockInstance = blockState.getBlockInstance();
      record.setBlockId(blockInstance.getBlock().getBlock().getId());
      record.setServiceDate(blockInstance.getServiceDate());

      ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
      record.setDistanceAlongBlock(blockLocation.getDistanceAlongBlock());

      if (EVehiclePhase.IN_PROGRESS.equals(phase)) {

        double d = SphericalGeometryLibrary.distance(edgeLocation,
            blockLocation.getLocation());
        if (d > _configurationService.getVehicleOffRouteDistanceThreshold())
          statusFields.add("deviated");

        int secondsSinceLastMotion = (int) ((particle.getTimestamp() - motionState.getLastInMotionTime()) / 1000);
        if (secondsSinceLastMotion > _configurationService.getVehicleStalledTimeThreshold())
          statusFields.add("stalled");
      }

    }

    // Set the status field
    if (!statusFields.isEmpty()) {
      StringBuilder b = new StringBuilder();
      for (String status : statusFields) {
        if (b.length() > 0)
          b.append(',');
        b.append(status);
      }
      record.setStatus(b.toString());
    }

    return record;
  }

  public synchronized List<Particle> getCurrentParticles() {
    return new ArrayList<Particle>(_particleFilter.getWeightedParticles());
  }
}
