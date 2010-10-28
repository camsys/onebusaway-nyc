package org.onebusaway.nyc.sms.actions;

import java.util.List;

import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.nyc.presentation.model.Mode;
import org.onebusaway.nyc.presentation.model.search.SearchResult;
import org.onebusaway.nyc.presentation.service.NycSearchService;
import org.onebusaway.nyc.sms.model.SmsDisplayer;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends AbstractNycSmsAction {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private NycSearchService searchService;
  
  /** text message */
  private String message;

  /** contains logic for preparing sms responses */
  private SmsDisplayer sms;
  
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getSmsResponse() {
    return sms.toString();
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

    List<SearchResult> searchResults = searchService.search(message, Mode.SMS);
    
    sms = new SmsDisplayer(searchResults);
    
    if (searchService.isStop(message)) {
      sms.singleStopResponse();
    } else {
      int nResults = searchResults.size();
      if (nResults == 0)
        sms.noResultsResponse();
      else if (nResults == 1)
        sms.singleStopResponse();
      else if (nResults == 2)
        sms.twoStopResponse();
      else
        sms.manyStopResponse();
    }

    return SUCCESS;
  }
}
