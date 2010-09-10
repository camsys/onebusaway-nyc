package org.onebusaway.webapp.service;

import java.util.List;

import org.onebusaway.nyc.webapp.model.search.SearchResult;

/**
 * nyc specific search logic
 */
public interface NycSearchService {

  /**
   * Return results specific to nyc logic
   * @param q Query to search for
   * @return List of search results matching query according to nyc logic
   */
  public abstract List<SearchResult> search(String q);

}