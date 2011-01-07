package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

public interface SensorModelRule {

  public double likelihood(SensorModelSupportLibrary library, Context context);
}
