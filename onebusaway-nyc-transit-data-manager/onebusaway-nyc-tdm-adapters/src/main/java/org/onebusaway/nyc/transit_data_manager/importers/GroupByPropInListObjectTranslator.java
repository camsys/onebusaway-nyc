package org.onebusaway.nyc.transit_data_manager.importers;

public interface GroupByPropInListObjectTranslator <S,T> {
  public T restructure (S listInput) ;
}
