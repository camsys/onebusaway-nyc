package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensormodel;

public interface SensorModelRule {

  public double likelihood(SensorModelSupportLibrary library, Context context);
}
