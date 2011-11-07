package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.impl.DefaultSearchModelFactory;
import org.onebusaway.nyc.presentation.model.search.StopResult;

public class DesktopWebSearchModelFactory extends DefaultSearchModelFactory {

  public StopResult getStopSearchResultModel() {
    return new DesktopWebStopResult();
  }

}