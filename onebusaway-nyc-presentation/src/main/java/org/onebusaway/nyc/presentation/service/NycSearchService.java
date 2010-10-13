package org.onebusaway.nyc.presentation.service;

import java.util.List;

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
  public List<SearchResult> search(String q);
  
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