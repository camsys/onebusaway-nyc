package org.onebusaway.nyc.transit_data_manager.adapters;

/**
 * Convert from one model object to another model object representing the
 * same/similar information.
 * 
 * @author sclark
 * 
 * @param <T> The input type.
 * @param <S> The output type.
 */
public interface ModelCounterpartConverter<T, S> {

  /**
   * Convert an object of type T to an object of Type S.
   * 
   * @param input An object of type T.
   * @return An object of type S representing the equivalent information.
   */
  S convert(T input);
}
