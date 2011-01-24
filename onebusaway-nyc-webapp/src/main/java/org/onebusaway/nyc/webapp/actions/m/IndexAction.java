package org.onebusaway.nyc.webapp.actions.m;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
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
  private static final String GA_ACCOUNT = "UA-XXXXXXXX-X";
  private static final String GA_PIXEL = "/ga";
	  
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

  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

  // Adapted from http://code.google.com/mobile/analytics/docs/web/#jsp
  // Contents of this method "Copyright 2009 Google Inc. All Rights Reserved."
  public String getGoogleAnalyticsTrackingUrl() {
	  try {
	      StringBuilder url = new StringBuilder();
	      url.append(GA_PIXEL + "?");
	      url.append("utmac=").append(GA_ACCOUNT);
	      url.append("&utmn=").append(Integer.toString((int) (Math.random() * 0x7fffffff)));
	
	      HttpServletRequest request = ServletActionContext.getRequest();      
	      String referer = request.getHeader("referer");
	      String query = request.getQueryString();
	      String path = request.getRequestURI();
	
	      if (referer == null || "".equals(referer)) {
	        referer = "-";
	      }
	      url.append("&utmr=").append(URLEncoder.encode(referer, "UTF-8"));
	
	      if (path != null) {
	        if (query != null) {
	          path += "?" + query;
	        }
	        url.append("&utmp=").append(URLEncoder.encode(path, "UTF-8"));
	      }

	      url.append("&guid=ON");

	      return url.toString().replace("&", "&amp;"); 
	  } catch(Exception e) {
		  return null;
	  }
  }

  public boolean getQueryIsEmpty() {
	  return q.isEmpty();
  }
  
  public List<SearchResult>getToc() {
	  List<SearchResult> tocList = new ArrayList<SearchResult>();
	  
	  for(SearchResult _result : searchResults) {
			if(_result.getType().equals("route"))
				tocList.add(_result);
	  }
	  
	  return tocList;	  
  }
  
  public String getCacheBreaker() {
	  return (int)Math.ceil((Math.random() * 100000)) + "";
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
		// no realtime data
		return DateFormat.getTimeInstance().format(new Date());
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
