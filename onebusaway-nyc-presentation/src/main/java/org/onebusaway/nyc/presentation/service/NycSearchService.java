/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.presentation.service;

import java.util.List;

import org.onebusaway.nyc.presentation.model.Mode;
import org.onebusaway.nyc.presentation.model.search.SearchResult;

/**
 * nyc specific search logic
 */
public interface NycSearchService {

  /**
   * Return results specific to nyc logic
   * @param q Query to search for
   * @return List of search results matching query according to nyc logic
   */
  public List<SearchResult> search(String q, Mode m);
  
  /**
   * Returns true if routeString can represent a route
   * @param routeString String to check
   * @return true if routeString represents a route
   */
  public boolean isRoute(String routeString);
  
  /**
   * Returns true if stopString looks like a stop
   * @param stopString String to check
   * @return true if stopString represents a stop
   */
  public boolean isStop(String stopString);

}