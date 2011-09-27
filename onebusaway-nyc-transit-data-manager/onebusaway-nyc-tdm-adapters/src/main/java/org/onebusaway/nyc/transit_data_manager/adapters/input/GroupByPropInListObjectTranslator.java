package org.onebusaway.nyc.transit_data_manager.adapters.input;

public interface GroupByPropInListObjectTranslator<S, T> {
  public T restructure(S listInput);
}
