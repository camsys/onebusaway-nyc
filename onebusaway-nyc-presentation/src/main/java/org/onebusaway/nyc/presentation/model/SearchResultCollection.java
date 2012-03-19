package org.onebusaway.nyc.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchResultCollection implements Serializable {

  private static final long serialVersionUID = 1L;

  private Class<? extends SearchResult> _resultType = null;
  
  private List<SearchResult> _matches = new ArrayList<SearchResult>();

  private List<SearchResult> _suggestions = new ArrayList<SearchResult>();

  private Set<String> _routeIdFilter = new HashSet<String>();
  
  public void addMatch(SearchResult thing) throws IllegalArgumentException {
    if(_resultType == null) { 
      _resultType = thing.getClass();
    }

    if(!_resultType.isInstance(thing)) {
      throw new IllegalArgumentException("All results must be of type " + _resultType);
    }
    
    _matches.add(thing);
  }

  public void addSuggestion(SearchResult thing) throws IllegalArgumentException {
    if(_resultType == null) { 
      _resultType = thing.getClass();
    }

    if(!_resultType.isInstance(thing)) {
      throw new IllegalArgumentException("All results must be of type " + _resultType);
    }
    
    _suggestions.add(thing);
  }

  public void addRouteIdFilter(String routeId) {
    _routeIdFilter.add(routeId);
  }
  
  public void addRouteIdFilters(Set<String> routeIds) {
    _routeIdFilter.addAll(routeIds);
  }

  public boolean isEmpty() {
    return ((_matches.size() + _suggestions.size()) == 0);
  }
  
  public List<SearchResult> getMatches() {
    return _matches;
  }

  public Set<String> getRouteIdFilter() {
    return _routeIdFilter;
  }

  public List<SearchResult> getSuggestions() {
    return _suggestions;
  }

  public String getResultType() {
    if(_resultType != null)
      return _resultType.getSimpleName();
    else
      return null;
  }  
}