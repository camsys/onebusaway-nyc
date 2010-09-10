package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.webapp.service.NycSearchService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests for a generic search. Can return route/stop specific results.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  private List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Autowired
  private NycSearchService searchService;
  
  // from q variable in query string
  private String q;
  
  @Override
  public String execute() {
    if (q == null || q.isEmpty())
      return SUCCESS;

    searchResults = searchService.search(q);

    return SUCCESS;
  }

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }
  
  public void setQ(String query) {
    this.q = query.trim();
  }
  
}
