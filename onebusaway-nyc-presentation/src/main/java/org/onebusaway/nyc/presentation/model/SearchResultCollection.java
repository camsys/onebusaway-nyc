package org.onebusaway.nyc.presentation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.transit_data.model.RouteBean;

public class SearchResultCollection implements Serializable {

  private static final long serialVersionUID = 1L;

  private Class<? extends SearchResult> _resultType = null;
  
  private List<SearchResult> _matches = new ArrayList<SearchResult>();

  private List<SearchResult> _suggestions = new ArrayList<SearchResult>();

  private Set<RouteBean> _routeFilter = new HashSet<RouteBean>();
  
  private Double _queryLat = null;
  
  private Double _queryLon = null;
  
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

  public void addRouteFilter(RouteBean route) {
    _routeFilter.add(route);
  }
  
  public void addRouteFilters(Set<RouteBean> routes) {
    _routeFilter.addAll(routes);
  }

  public boolean isEmpty() {
    return ((_matches.size() + _suggestions.size()) == 0);
  }
  
  public List<SearchResult> getMatches() {
    return _matches;
  }

  public Set<RouteBean> getRouteFilter() {
    return _routeFilter;
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

  public Double getQueryLat() {
    return _queryLat;
  }

  public void setQueryLat(Double queryLat) {
    this._queryLat = queryLat;
  }

  public Double getQueryLon() {
    return _queryLon;
  }

  public void setQueryLon(Double queryLon) {
    this._queryLon = queryLon;
  }
}