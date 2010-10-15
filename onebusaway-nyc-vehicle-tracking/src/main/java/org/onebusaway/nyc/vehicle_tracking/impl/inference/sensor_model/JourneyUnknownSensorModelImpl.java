package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * 
 * @author bdferris
 * 
 */
public class JourneyUnknownSensorModelImpl implements SensorModel<Observation> {

  /**
   * We shouldn't be in unknown state if we have an out-of-service DSC
   */
  private double _propabilityOfUnknownWithAnOutOfServiceDSC = 0.05;

  private DestinationSignCodeService _destinationSignCodeService;

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Override
  public double likelihood(Particle particle, Observation observation) {

    NycVehicleLocationRecord record = observation.getRecord();
    String dsc = record.getDestinationSignCode();
    boolean outOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(dsc);

    /**
     * Note that these two probabilities don't have to add up to 1, as they are
     * conditionally independent.
     */
    if (outOfService)
      return _propabilityOfUnknownWithAnOutOfServiceDSC;

    // TODO : How do we measure this?
    return 1.0;
  }
}
