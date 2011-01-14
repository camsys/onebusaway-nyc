package org.onebusaway.nyc.webapp.actions.m;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.onebusaway.nyc.presentation.model.DistanceAway;
import org.onebusaway.nyc.presentation.model.Mode;
import org.onebusaway.nyc.presentation.model.RouteItem;
import org.onebusaway.nyc.presentation.model.StopItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.SearchResult;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.NycSearchService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private NycSearchService searchService;

  private String q;

  private List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Override
  public String execute() throws Exception {
    if (q != null)
      searchResults = searchService.search(q, Mode.MOBILE_WEB);
    return SUCCESS;
  }

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }

  public boolean getQueryIsEmpty() {
	  return q.isEmpty();
  }
  
  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

  public String getLastUpdateTime() {
	Date lastUpdated = null;
	for(SearchResult _result : searchResults) {
		if(_result.getType().equals("route")) {
			RouteSearchResult result = (RouteSearchResult)_result;
			for(StopItem stop : result.getStopItems()) {
				for(DistanceAway distanceAway : stop.getDistanceAways()) {
					if(lastUpdated == null || distanceAway.getUpdateTimestamp().getTime() < lastUpdated.getTime()) {
						lastUpdated = distanceAway.getUpdateTimestamp();
					}
				}
			}
		} else if(_result.getType().equals("stop")) {
			StopSearchResult result = (StopSearchResult)_result;
			for(RouteItem route : result.getRoutesAvailable()) {
				for(DistanceAway distanceAway : route.getDistanceAways()) {
					if(lastUpdated == null || distanceAway.getUpdateTimestamp().getTime() < lastUpdated.getTime()) {
						lastUpdated = distanceAway.getUpdateTimestamp();
					}
				}
			}
		}
	}

	if(lastUpdated != null) {
		return DateFormat.getTimeInstance().format(lastUpdated);
	} else {
		return "(unknown)";
	}
  }  
  
  public String getTitle() {
	String title = this.q;
	
	if(searchResults.size() == 1) {
	  SearchResult result = searchResults.get(0);
	  
	  if(result != null) {
		title = result.getName() + " (" + title + ")";
	  }
	}
	
	return title;
  }
}
