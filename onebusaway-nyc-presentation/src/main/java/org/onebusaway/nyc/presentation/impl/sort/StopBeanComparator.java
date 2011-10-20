package org.onebusaway.nyc.presentation.impl.sort;

import org.onebusaway.transit_data.model.StopBean;

import java.util.Comparator;
import java.util.Map;

public class StopBeanComparator implements Comparator<StopBean> {

  private final Map<String, Double> stopIdToDistances;

  public StopBeanComparator(Map<String, Double> stopIdToDistances) {
    this.stopIdToDistances = stopIdToDistances;
  }

  @Override
  public int compare(StopBean o1, StopBean o2) {
    Double d1 = stopIdToDistances.get(o1.getId());
    Double d2 = stopIdToDistances.get(o2.getId());
    if (d1 == null)
      d1 = Double.valueOf(0);
    if (d2 == null)
      d2 = Double.valueOf(0);
    return d1.compareTo(d2);
  }

}