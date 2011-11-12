package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.PresentationModelFactory;

import java.util.List;

public interface StopSearchService {

  public void setModelFactory(PresentationModelFactory factory);

  public List<StopResult> resultsForLocation(Double lat, Double lng);

  public List<StopResult> resultsForQuery(String stopQuery);

  public StopResult makeResultForStopId(String stopId);

}