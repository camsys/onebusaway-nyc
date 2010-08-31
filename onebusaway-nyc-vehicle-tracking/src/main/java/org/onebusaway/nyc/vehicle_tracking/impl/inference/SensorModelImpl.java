package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Arrays;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.model.narrative.TripNarrative;
import org.onebusaway.transit_data_federation.services.ShapePointService;
import org.onebusaway.transit_data_federation.services.narrative.NarrativeService;
import org.onebusaway.transit_data_federation.services.tripplanner.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripInstanceProxy;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private NarrativeService _narrativeService;

  private ShapePointService _shapePointService;

  private Gaussian _locationDeviationModel = new Gaussian(0.0, 20);

  private Gaussian _tripLocationDeviationModel = new Gaussian(0.0, 20);

  @Autowired
  public void setNarrativeService(NarrativeService narrativeService) {
    _narrativeService = narrativeService;
  }

  @Autowired
  public void setShapePointService(ShapePointService shapePointService) {
    _shapePointService = shapePointService;
  }

  /**
   * 
   * @param locationVariance in meters
   */
  public void setLocationVariance(double locationVariance) {
    _locationDeviationModel = new Gaussian(0, locationVariance);
  }

  /**
   * 
   * @param tripLocationVariance in meters
   */
  public void setTripLocationVariance(double tripLocationVariance) {
    _tripLocationDeviationModel = new Gaussian(0, tripLocationVariance);
  }

  @Override
  public double likelihood(Particle particle, Observation obs) {

    // Really simple starter sensor model: penalize for distance away from
    // observed GPS location
    double pLocation = computeLocationVsGpsPropability(particle, obs);

    // Penalize for distance away from trip path
    double pTrip = computeLocationVsTripProbability(particle);

    // log-likelihood is more mathematically stable
    return pLocation * pTrip;
  }

  private double computeLocationVsGpsPropability(Particle particle,
      Observation obs) {

    VehicleState state = particle.getData();
    ProjectedPoint point = obs.getPoint();

    double locationDeviation = state.getEdgeState().getPointOnEdge().distance(
        point);
    return _locationDeviationModel.getProbability(locationDeviation);
  }

  private double computeLocationVsTripProbability(Particle particle) {

    VehicleState state = particle.getData();
    TripInstanceProxy tripInstance = state.getTripInstance();

    if( tripInstance == null)
      return 1.0;
    
    TripEntry trip = tripInstance.getTrip();
    
    TripNarrative tripNarrative = _narrativeService.getTripForId(trip.getId());
    AgencyAndId shapeId = tripNarrative.getShapeId();

    // If we don't have a shape, we just assume uniform distribution
    if (shapeId == null)
      return 1.0;

    ShapePoints points = _shapePointService.getShapePointsForShapeId(shapeId);

    int index = Arrays.binarySearch(points.getDistTraveled(),
        state.getTripPositionOffset());
    if (index < 0)
      index = -(index + 1);

    double[] lats = points.getLats();
    double[] lons = points.getLons();

    double lat = lats[index];
    double lon = lons[index];

    ProjectedPoint point = state.getEdgeState().getPointOnEdge();
    double distance = SphericalGeometryLibrary.distance(lat, lon,
        point.getLat(), point.getLon());
    return _tripLocationDeviationModel.getProbability(distance);
  }
}
