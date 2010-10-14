package org.onebusaway.nyc.sms.actions;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.presentation.model.search.SearchResult;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.NycSearchService;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends AbstractNycSmsAction {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private NycSearchService searchService;
  
  /** text message */
  private String message;
  
  /** results returned to jsp */
  private List<SearchResult> searchResults;

  // stop search result for single stop template
  public StopSearchResult getStopResult() {
    if (searchResults.size() != 1)
      return null;
    SearchResult result = searchResults.get(0);
    return (StopSearchResult) result;
  }
  
  public List<StopSearchResult> getStopResults() {
    List<StopSearchResult> result = new ArrayList<StopSearchResult>(searchResults.size());
    for (SearchResult searchResult : searchResults)
      result.add((StopSearchResult) searchResult);
    return result;
  }
  
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String execute() throws Exception {
    if (message == null)
      throw new ServiceException("No message specified");

    message = message.trim();
    if (message.isEmpty())
      throw new ServiceException("No message specified");
    
    if (searchService.isRoute(message))
      throw new ServiceException("Route specified");

    searchResults = searchService.search(message);
    
    if (searchService.isStop(message))
      return "single-stop";
    
    int nResults = searchResults.size();
    if (nResults == 0)
      return "no-stops";
    if (nResults == 1)
      return "single-stop";
    if (nResults == 2)
      return "two-stops";
    return "many-stops";
  }
}
