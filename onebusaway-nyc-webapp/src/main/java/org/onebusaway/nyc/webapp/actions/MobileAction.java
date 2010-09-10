package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.nyc.webapp.service.NycSearchService;
import org.springframework.beans.factory.annotation.Autowired;

public class MobileAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private NycSearchService searchService;

  private String q;

  private List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Override
  public String execute() throws Exception {
    if (q != null)
      searchResults = searchService.search(q);
    return SUCCESS;
  }

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }

  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

}
