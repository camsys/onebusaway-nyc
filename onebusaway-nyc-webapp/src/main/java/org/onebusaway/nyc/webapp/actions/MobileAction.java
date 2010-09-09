package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.webapp.impl.NycSearcher;
import org.springframework.beans.factory.annotation.Autowired;

public class MobileAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private TransitDataService transitService;
  
  @Autowired
  private ServiceAreaService serviceArea;
  
  @Autowired
  private GeocoderService geocoderService;

  private String q;

  private List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Override
  public String execute() throws Exception {
    if (q != null)
      performSearch();  
    return SUCCESS;
  }

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }

  private void performSearch() {
    NycSearcher searcher = new NycSearcher(transitService, geocoderService, serviceArea);
    searchResults = searcher.search(q);
  }

  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

}
