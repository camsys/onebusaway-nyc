package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

public interface SensorModelRule {

  public SensorModelResult likelihood(SensorModelSupportLibrary library, Context context);
}
