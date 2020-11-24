/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
