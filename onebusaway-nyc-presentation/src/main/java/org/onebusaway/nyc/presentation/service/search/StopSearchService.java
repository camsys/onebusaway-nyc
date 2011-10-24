package org.onebusaway.nyc.presentation.service.search;

import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.ModelFactory;

import java.util.List;

public interface StopSearchService {

  public void setModelFactory(ModelFactory factory);

  public List<StopSearchResult> resultsForLocation(Double lat, Double lng);

  public List<StopSearchResult> makeResultFor(String stopId);

}