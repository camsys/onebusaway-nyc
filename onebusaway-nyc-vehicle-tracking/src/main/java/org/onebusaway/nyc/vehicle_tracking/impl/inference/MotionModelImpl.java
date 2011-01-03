package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private JourneyStateTransitionModel _journeyMotionModel;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  private double _motionThreshold = 20;

  private BaseLocationService _baseLocationService;

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
    _journeyMotionModel = journeyMotionModel;
  }

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  public void setMotionThreshold(double motionThreshold) {
    _motionThreshold = motionThreshold;
  }

  @Override
  public void move(Particle parent, double timestamp, double timeElapsed,
      Observation obs, List<Particle> results) {

    VehicleState parentState = parent.getData();

    /**
     * Snap the observation to an edge on the street network
     */
    EdgeState edgeState = determineEdgeState(obs, parentState);

    MotionState motionState = updateMotionState(parentState, obs);

    List<VehicleState> vehicleStates = new ArrayList<VehicleState>();
    _journeyMotionModel.move(parentState, edgeState, motionState, obs,
        vehicleStates);

    for (VehicleState vs : vehicleStates)
      results.add(new Particle(timestamp, parent, 1.0, vs));
  }

  private EdgeState determineEdgeState(Observation obs, VehicleState parentState) {

    EdgeState edgeState = parentState.getEdgeState();

    /**
     * We only let the street network edge change if the GPS has changed. This
     * helps us avoid oscilation between potential states when the bus isn't
     * moving.
     */
    if (gpsHasChanged(obs)) {

      /**
       * We can cache the edge CDF for an observation
       */
      /*
       * CDFMap<EdgeState> edges = _observationCache.getValueForObservation(obs,
       * EObservationCacheKey.STREET_NETWORK_EDGES);
       * 
       * if (edges == null) { edges =
       * _edgeStateLibrary.calculatePotentialEdgeStates(obs.getPoint());
       * _observationCache.putValueForObservation(obs,
       * EObservationCacheKey.STREET_NETWORK_EDGES, edges); }
       * 
       * edgeState = edges.sample();
       */
    }

    return edgeState;
  }

  public MotionState updateMotionState(VehicleState parentState,
      Observation obs) {

    MotionState motionState = parentState.getMotionState();

    CoordinatePoint location = obs.getLocation();

    long lastInMotionTime = motionState.getLastInMotionTime();
    CoordinatePoint lastInMotionLocation = motionState.getLastInMotionLocation();
    boolean atBase = _baseLocationService.getBaseNameForLocation(location) != null;
    boolean atTerminal = _baseLocationService.getTerminalNameForLocation(location) != null;

    double d = SphericalGeometryLibrary.distance(
        motionState.getLastInMotionLocation(), location);

    if (d > _motionThreshold) {
      lastInMotionTime = obs.getTime();
      lastInMotionLocation = location;
    }

    return new MotionState(lastInMotionTime, lastInMotionLocation, atBase,
        atTerminal);
  }

  private boolean gpsHasChanged(Observation obs) {

    NycVehicleLocationRecord record = obs.getRecord();
    NycVehicleLocationRecord prevRecord = obs.getPreviousRecord();

    if (prevRecord == null)
      return true;

    return prevRecord.getLatitude() != record.getLatitude()
        || prevRecord.getLongitude() != record.getLongitude();
  }
}
