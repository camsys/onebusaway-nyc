package org.onebusaway.nyc.webapp.model;

import java.util.List;

import org.onebusaway.nyc.webapp.impl.DistancePresenter;
import org.onebusaway.nyc.webapp.impl.WebappIdParser;
import org.onebusaway.transit_data.model.StopBean;

/**
 * data transfer object wrapping a stop bean
 * useful to contain logic of returning back just the id portion of the stop
 * which doesn't include the agency id
 */
public class StopItem {

  private final String id;
  private final String name;
  private final List<Double> meterDistances;
  
  private static final WebappIdParser idParser = new WebappIdParser();

  public StopItem(StopBean stopBean, List<Double> meterDistances) {
    this(idParser.parseIdWithoutAgency(stopBean.getId()), stopBean.getName(), meterDistances);
  }
  
  public StopItem(String id, String name, List<Double> meterDistances) {
    this.id = id;
    this.name = name;
    this.meterDistances = meterDistances;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<Double> getDistances() {
    return meterDistances;
  }
  
  public String getPresentableDistances() {
    if (meterDistances == null || meterDistances.size() == 0)
      return "";
    
    StringBuilder b = new StringBuilder();
    
    for (Double distance : meterDistances) {
      if(b.length() > 0)
    	  b.append(", ");
      
      b.append(DistancePresenter.displayFeet(distance));
    }

    b.insert(0, " ");
    
    return b.toString();
  }
}
