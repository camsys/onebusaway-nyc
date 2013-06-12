package org.onebusaway.nyc.report_archive.services;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;


public interface DummyPredictionOutputQueueSenderService {

  void enqueue(FeedMessage generatePredictionsForVehicle);

}
