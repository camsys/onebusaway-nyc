package org.onebusaway.nyc.transit_data_federation.services.bundle;

public interface InferenceProcessor {

  boolean isCancelled();

  boolean isDone();

  void shutdown();

}
