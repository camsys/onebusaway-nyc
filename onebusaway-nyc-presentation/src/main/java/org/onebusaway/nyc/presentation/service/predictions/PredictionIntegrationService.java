package org.onebusaway.nyc.presentation.service.predictions;

import java.util.List;

import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public interface PredictionIntegrationService {

  public List<TimepointPredictionRecord> getTimepointPredictions(TripStatusBean tripStatus);
  
}